package com.myorg;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.dynamodb.*;
import software.amazon.awscdk.services.lambda.Function;
import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

import java.util.Map;

public class TherapyJournalCdkStack extends Stack {
    public TherapyJournalCdkStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public TherapyJournalCdkStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Table sessionsTable = Table.Builder.create(this, "SessionsTable")
                .tableName("Sessions")
                .partitionKey(Attribute.builder()
                        .name("therapistId")
                        .type(AttributeType.STRING)
                        .build())
                .sortKey(Attribute.builder()
                        .name("startTime#sessionId")
                        .type(AttributeType.STRING)
                        .build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        Table mappingsTable = Table.Builder.create(this, "MappingsTable")
                .tableName("Mappings")
                .partitionKey(Attribute.builder()
                        .name("mappingId")
                        .type(AttributeType.STRING)
                        .build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        mappingsTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("clientMappingGSI")
                .partitionKey(Attribute.builder()
                        .name("clientId")
                        .type(AttributeType.STRING)
                        .build())
                .sortKey(Attribute.builder()
                        .name("therapistId")
                        .type(AttributeType.STRING)
                        .build())
                .build());
        mappingsTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("therapistMappingGSI")
                .partitionKey(Attribute.builder()
                        .name("therapistId")
                        .type(AttributeType.STRING)
                        .build())
                .sortKey(Attribute.builder()
                        .name("clientId")
                        .type(AttributeType.STRING)
                        .build())
                .build());

        Table appointmentTable = Table.Builder.create(this, "AppointmentsTable")
                .tableName("Appointments")
                .partitionKey(Attribute.builder()
                        .name("appointmentId")
                        .type(AttributeType.STRING)
                        .build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        Table messagesTable = Table.Builder.create(this, "MessagesTable")
                .tableName("Messages")
                .partitionKey(Attribute.builder()
                        .name("conversationId")
                        .type(AttributeType.STRING)
                        .build())
                .sortKey(Attribute.builder()
                        .name("timestamp#messageId")
                        .type(AttributeType.STRING)
                        .build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        messagesTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("senderMessageGSI")
                .partitionKey(Attribute.builder()
                        .name("senderId")
                        .type(AttributeType.STRING)
                        .build())
                .sortKey(Attribute.builder()
                        .name("timestamp#messageId")
                        .type(AttributeType.STRING)
                        .build())
                .build());

        messagesTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("receiverMessageGSI")
                .partitionKey(Attribute.builder()
                        .name("receiverId")
                        .type(AttributeType.STRING)
                        .build())
                .sortKey(Attribute.builder()
                        .name("timestamp#messageId")
                        .type(AttributeType.STRING)
                        .build())
                .build());

        // Therapy Session lambdas
        Function therapySessionCreateLambda = LambdaFactory.createJavaLambda(this, "TherapySessionCreate",
                "com.myorg.handlers.TherapySessionCreateHandler::handleRequest",
                Map.of("SESSIONS_TABLE", sessionsTable.getTableName()));
        sessionsTable.grantReadWriteData(therapySessionCreateLambda);

        Function therapySessionListLambda = LambdaFactory.createJavaLambda(this, "TherapySessionList",
                "com.myorg.handlers.TherapySessionListHandler::handleRequest",
                Map.of("SESSIONS_TABLE", sessionsTable.getTableName()));
        sessionsTable.grantReadData(therapySessionListLambda);

        Function therapySessionUpdateLambda = LambdaFactory.createJavaLambda(this, "TherapySessionUpdate",
                "com.myorg.handlers.TherapySessionUpdateHandler::handleRequest",
                Map.of("SESSIONS_TABLE", sessionsTable.getTableName()));
        sessionsTable.grantReadWriteData(therapySessionUpdateLambda);

        Function therapySessionDeleteLambda = LambdaFactory.createJavaLambda(this, "TherapySessionDelete",
                "com.myorg.handlers.TherapySessionDeleteHandler::handleRequest",
                Map.of("SESSIONS_TABLE", sessionsTable.getTableName()));
        sessionsTable.grantReadWriteData(therapySessionDeleteLambda);

        // appointment Lambda
        Function appointmentCreateLambda = LambdaFactory.createJavaLambda(
                this,
                "AppointmentCreate",
                "com.myorg.handlers.AppointmentCreateHandler::handleRequest",
                Map.of("APPOINTMENTS_TABLE", appointmentTable.getTableName(), "SESSIONS_TABLE", sessionsTable.getTableName())
        );
        sessionsTable.grantReadWriteData(appointmentCreateLambda);
        appointmentTable.grantReadWriteData(appointmentCreateLambda);

        Function appointmentUpdateLambda = LambdaFactory.createJavaLambda(
                this, "AppointmentUpdate",
                "com.myorg.handlers.AppointmentUpdateHandler::handleRequest",
                Map.of("APPOINTMENTS_TABLE", appointmentTable.getTableName(), "MAPPINGS_TABLE", mappingsTable.getTableName())
        );
        appointmentTable.grantReadWriteData(appointmentUpdateLambda);
        mappingsTable.grantReadWriteData(appointmentUpdateLambda);

        // Mappings lambda
        Function mappingsGetClientLambda = LambdaFactory.createJavaLambda(this, "MappingsGetClient",
                "com.myorg.handlers.MappingsGetClientHandlers::handleRequest",
                Map.of("MAPPINGS_TABLE", mappingsTable.getTableName()));
        mappingsTable.grantReadData(mappingsGetClientLambda);

        Function mappingsGetTherapistLambda = LambdaFactory.createJavaLambda(this, "MappingsGetTherapist",
                "com.myorg.handlers.MappingsGetTherapistHandler::handleRequest",
                Map.of("MAPPINGS_TABLE", mappingsTable.getTableName()));
        mappingsTable.grantReadData(mappingsGetTherapistLambda);

        Function mappingsJournalAccessRequestLambda = LambdaFactory.createJavaLambda(this, "MappingsJournalAccessRequest",
                "com.myorg.handlers.MappingsJournalAccessRequestHandler::handleRequest",
                Map.of("MAPPINGS_TABLE", mappingsTable.getTableName()));
        mappingsTable.grantReadWriteData(mappingsJournalAccessRequestLambda);

        Function mappingsUpdateLambda = LambdaFactory.createJavaLambda(this, "MappingsUpdate",
                "com.myorg.handlers.MappingsUpdateHandler::handleRequest",
                Map.of("MAPPINGS_TABLE", mappingsTable.getTableName()));
        mappingsTable.grantReadWriteData(mappingsUpdateLambda);
        mappingsTable.grantReadWriteData(appointmentUpdateLambda);

        // Messages lambda
        Function messagesSendLambda = LambdaFactory.createJavaLambda(this, "MessagesSend",
                "com.myorg.handlers.MessageSendHandler::handleRequest",
                Map.of("MESSAGES_TABLE", messagesTable.getTableName()));
        messagesTable.grantReadWriteData(messagesSendLambda);

        Function messagesGetLambda = LambdaFactory.createJavaLambda(this, "MessagesGet",
                "com.myorg.handlers.MessageGetHandler::handleRequest",
                Map.of("MESSAGES_TABLE", messagesTable.getTableName()));
        messagesTable.grantReadData(messagesGetLambda);

        // API gateway setup
        RestApi api = RestApi.Builder.create(this, "TherapyJournalApi")
                .restApiName("Therapy Journal Service")
                .description("This service serves therapy journal APIs")
                .build();

        var therapists = api.getRoot().addResource("therapists");
        var therapistIdRes = therapists.addResource("{therapistId}");
        var sessions = therapistIdRes.addResource("sessions");
        sessions.addMethod("POST", new LambdaIntegration(therapySessionCreateLambda));
        sessions.addMethod("GET", new LambdaIntegration(therapySessionListLambda));
        var session = sessions.addResource("{sessionId}");
        session.addMethod("PUT", new LambdaIntegration(therapySessionUpdateLambda));
        session.addMethod("DELETE", new LambdaIntegration(therapySessionDeleteLambda));

        var sessionResources = api.getRoot().addResource("sessions");
        var sessionResource = sessionResources.addResource("{sessionId}");
        var appointmentResource = sessionResource.addResource("appointments");
        appointmentResource.addMethod("POST", new LambdaIntegration(appointmentCreateLambda));

        var appointmentsResource = api.getRoot().addResource("appointments").addResource("{appointmentId}");
        appointmentsResource.addMethod("PUT", new LambdaIntegration(appointmentUpdateLambda));

        var clients = api.getRoot().addResource("clients");
        var clientIdRes = clients.addResource("{clientId}");
        var clientMappings = clientIdRes.addResource("mappings");
        clientMappings.addMethod("GET", new LambdaIntegration(mappingsGetClientLambda));

        var therapistMappings = therapistIdRes.addResource("mappings");
        therapistMappings.addMethod("GET", new LambdaIntegration(mappingsGetTherapistLambda));

        var clientTherapistMappings = api.getRoot().addResource("client-therapist-mappings");
        var mappingIdRes = clientTherapistMappings.addResource("{mappingId}");
        var journalAccessRequest = mappingIdRes.addResource("journal-access-request");
        journalAccessRequest.addMethod("POST", new LambdaIntegration(mappingsJournalAccessRequestLambda));
        mappingIdRes.addMethod("PUT", new LambdaIntegration(mappingsUpdateLambda));

        var messagesResource = api.getRoot().addResource("messages");
        messagesResource.addMethod("POST", new LambdaIntegration(messagesSendLambda));
        messagesResource.addMethod("GET", new LambdaIntegration(messagesGetLambda));
    }
}
