package com.myorg.handlers;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;

import java.util.Map;
import java.util.UUID;

public class AppointmentUpdateHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final DynamoDB dynamoDB;
    private final String appointmentsTable;
    private final String mappingsTable;
    private Gson gson = new Gson();

    public AppointmentUpdateHandler() {
        this.dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient());
        this.appointmentsTable = System.getenv("APPOINTMENTS_TABLE");
        this.mappingsTable = System.getenv("MAPPINGS_TABLE");
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> stringObjectMap, Context context) {
        try {
            Map<String,String> pathParams = (Map) stringObjectMap.get("pathParameters");
            String appointmentId = pathParams.get("appointmentId");

            Map<String,Object> requestBody = gson.fromJson((String) stringObjectMap.get("body"), Map.class);
            String newStatus = (String) requestBody.get("status");

            if ("confirmed".equals(newStatus)) {
                Item appointment = dynamoDB.getTable(appointmentsTable).getItem("appointmentId", appointmentId);

                String mappingId = UUID.randomUUID().toString();
                Item mapping = new Item()
                        .withPrimaryKey("mappingId", mappingId)
                        .withString("clientId", appointment.getString("clientId"))
                        .withString("therapistId", appointment.getString("therapistId"))
                        .withBoolean("isMapped", true)
                        .withString("status", "active")
                        .withBoolean("journalAccessRequested", false)
                        .withBoolean("journalAccess", false);

                dynamoDB.getTable(mappingsTable).putItem(new PutItemSpec().withItem(mapping));

                UpdateItemSpec updateAppointment = new UpdateItemSpec()
                        .withPrimaryKey("appointmentId", appointmentId)
                        .withUpdateExpression("set mappingId = :mid")
                        .withValueMap(Map.of(":mid", mappingId));

                dynamoDB.getTable(appointmentsTable).updateItem(updateAppointment);
            }

            Table table = dynamoDB.getTable(appointmentsTable);

            UpdateItemSpec updateSpec = new UpdateItemSpec()
                    .withPrimaryKey("appointmentId", appointmentId)
                    .withUpdateExpression("set #st = :val")
                    .withNameMap(Map.of("#st", "status"))
                    .withValueMap(Map.of(":val", newStatus));

            table.updateItem(updateSpec);

            return Map.of(
                    "statusCode", 200,
                    "body", gson.toJson(Map.of("message", "Appointment updated"))
            );
        } catch (Exception e) {
            return Map.of(
                    "statusCode", 400,
                    "body", gson.toJson(Map.of("error", e.getMessage()))
            );
        }
    }
}
