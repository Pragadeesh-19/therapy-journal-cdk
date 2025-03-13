package com.myorg.handlers;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TherapySessionListHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final DynamoDB dynamoDB;
    private final String sessionsTable;
    private final Gson gson = new Gson();

    public TherapySessionListHandler() {
        this.dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient());
        this.sessionsTable = System.getenv("SESSIONS_TABLE");
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> stringObjectMap, Context context) {
        try {
            Map<String,String> pathParams = (Map) stringObjectMap.get("pathParameters");
            String therapistId = pathParams.get("therapistId");

            // Query sessions
            QuerySpec querySpec = new QuerySpec()
                    .withKeyConditionExpression("therapistId = :v_id")
                    .withValueMap(Map.of(":v_id", therapistId));

            List<Map<String, Object>> sessions = new ArrayList<>();
            dynamoDB.getTable(sessionsTable)
                    .query(querySpec)
                    .forEach(item -> sessions.add(item.asMap()));

            return Map.of(
                    "statusCode", 200,
                    "body", gson.toJson(Map.of("data", sessions))
            );
        } catch (Exception e) {
            return Map.of(
                    "statusCode", 500,
                    "body", gson.toJson(Map.of("error", e.getMessage()))
            );
        }
    }
}
