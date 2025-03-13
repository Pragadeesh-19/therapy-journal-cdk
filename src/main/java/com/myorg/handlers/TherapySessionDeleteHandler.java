package com.myorg.handlers;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;

import java.util.Map;

public class TherapySessionDeleteHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final DynamoDB dynamoDB;
    private final String sessionsTable;
    private final Gson gson = new Gson();

    public TherapySessionDeleteHandler() {
        this.dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient());
        this.sessionsTable = System.getenv("SESSIONS_TABLE");
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> stringObjectMap, Context context) {
        try {
            Map<String,String> pathParams = (Map) stringObjectMap.get("pathParameters");
            String therapistId = pathParams.get("therapistId");
            String sessionId = pathParams.get("sessionId");

            // Get startTime from query params
            Map<String,String> queryParams = (Map) stringObjectMap.get("queryStringParameters");
            String startTime = queryParams.get("startTime");
            String sortKey = startTime + "#" + sessionId;

            // Delete item
            DeleteItemSpec deleteSpec = new DeleteItemSpec()
                    .withPrimaryKey("therapistId", therapistId, "startTime#sessionId", sortKey);

            dynamoDB.getTable(sessionsTable).deleteItem(deleteSpec);

            return Map.of(
                    "statusCode", 204,
                    "body", ""
            );
        } catch (Exception e) {
            return Map.of(
                    "statusCode", 404,
                    "body", gson.toJson(Map.of("error", "Session not found"))
            );

        }
    }
}
