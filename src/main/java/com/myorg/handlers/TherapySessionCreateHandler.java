package com.myorg.handlers;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;

import java.util.Map;
import java.util.UUID;

public class TherapySessionCreateHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final DynamoDB dynamoDB;
    private final String sessionsTable;

    private final Gson gson = new Gson();

    public TherapySessionCreateHandler() {
        this.dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient());
        this.sessionsTable = System.getenv("SESSIONS_TABLE");
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> stringObjectMap, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Received event: " + stringObjectMap);
        try {

            Map<String, String> pathParams = (Map) stringObjectMap.get("pathParameters");
            String therapistId = pathParams.get("therapistId");
            String body = (String) stringObjectMap.get("body");
            Map<String, Object> requestBody = gson.fromJson(body, Map.class);

            String sessionId = requestBody.containsKey("sessionId")
                    ? (String) requestBody.get("sessionId")
                    : UUID.randomUUID().toString();

            String startTime = (String) requestBody.get("startTime");
            String sortKey = startTime + "#" + sessionId;

            Item item = new Item()
                    .withPrimaryKey("therapistId", therapistId, "startTime#sessionId", sortKey)
                    .withString("sessionId", sessionId)
                    .withString("startTime", startTime)
                    .withString("endTime", (String) requestBody.get("endTime"))
                    .withString("privateNotes", (String) requestBody.get("privateNotes"))
                    .withString("sharedNotes", (String) requestBody.get("sharedNotes"))
                    .withString("status", "available");


            Table table = dynamoDB.getTable(sessionsTable);
            table.putItem(new PutItemSpec().withItem(item));

            return Map.of(
                    "statusCode", 201,
                    "headers", Map.of("Content-Type", "application/json"),
                    "body", gson.toJson(Map.of(
                            "status", "success",
                            "data", item.asMap()
                    ))
            );
        } catch (Exception e) {
            return Map.of(
                    "statusCode", 500,
                    "body", gson.toJson(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ))
            );
        }
    }
}
