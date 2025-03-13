package com.myorg.handlers;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class AppointmentCreateHandler  implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final DynamoDB dynamoDB;
    private final String sessionsTable;
    private final String appointmentsTable;
    private final Gson gson;

    public AppointmentCreateHandler() {
        this.dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient());
        this.sessionsTable = System.getenv("SESSIONS_TABLE");
        this.appointmentsTable = System.getenv("APPOINTMENTS_TABLE");
        this.gson = new Gson();
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> stringObjectMap, Context context) {
        try {
            Map<String,String> pathParams = (Map) stringObjectMap.get("pathParameters");
            String sessionId = pathParams.get("sessionId");

            // Parse request body
            Map<String,Object> requestBody = gson.fromJson((String) stringObjectMap.get("body"), Map.class);
            String clientId = (String) requestBody.get("clientId");
            String therapistId = (String) requestBody.get("therapistId");
            String startTime = (String) requestBody.get("startTime");

            String sessionSortKey = startTime + "#" + sessionId;
            Item session = dynamoDB.getTable(sessionsTable)
                    .getItem("therapistId", therapistId, "startTime#sessionId", sessionSortKey);

            if (session == null) {
                return Map.of(
                        "statusCode", 404,
                        "body", gson.toJson(Map.of("error", "Session not found"))
                );
            }

            // Create appointment
            String appointmentId = UUID.randomUUID().toString();
            Item item = new Item()
                    .withPrimaryKey("appointmentId", appointmentId)
                    .withString("sessionId", sessionId)
                    .withString("clientId", clientId)
                    .withString("therapistId", therapistId)
                    .withString("status", "requested")
                    .withString("requestedAt", Instant.now().toString());


            dynamoDB.getTable(appointmentsTable).putItem(new PutItemSpec().withItem(item));

            return Map.of(
                    "statusCode", 201,
                    "body", gson.toJson(Map.of("appointmentId", appointmentId))
            );
        } catch (Exception e) {
            return Map.of(
                    "statusCode", 400,
                    "body", gson.toJson(Map.of("error", e.getMessage()))
            );
        }
    }
}
