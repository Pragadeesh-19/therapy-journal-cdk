package com.myorg.handlers;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;

import java.util.Map;

public class TherapySessionUpdateHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final DynamoDB dynamoDB;
    private final String sessionsTable;
    private final Gson gson = new Gson();

    public TherapySessionUpdateHandler() {
        this.dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient());
        this.sessionsTable = System.getenv("SESSIONS_TABLE");
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> stringObjectMap, Context context) {
        try {
            Map<String,String> pathParams = (Map) stringObjectMap.get("pathParameters");
            String therapistId = pathParams.get("therapistId");
            String sessionId = pathParams.get("sessionId");

            Map<String,Object> requestBody = gson.fromJson((String) stringObjectMap.get("body"), Map.class);
            String startTime = (String) requestBody.get("startTime");
            String sortKey = startTime + "#" + sessionId;

            UpdateItemSpec updateSpec = new UpdateItemSpec()
                    .withPrimaryKey("therapistId", therapistId, "startTime#sessionId", sortKey)
                    .withUpdateExpression("set endTime = :e, privateNotes = :p, sharedNotes = :s, #st = :st")
                    .withNameMap(Map.of("#st", "status"))
                    .withValueMap(Map.of(
                            ":e", requestBody.get("endTime"),
                            ":p", requestBody.get("privateNotes"),
                            ":s", requestBody.get("sharedNotes"),
                            ":st", requestBody.get("status")
                    ))
                    .withReturnValues(ReturnValue.ALL_NEW);

            Item updated = dynamoDB.getTable(sessionsTable).updateItem(updateSpec).getItem();

            return Map.of(
                    "statusCode", 200,
                    "body", gson.toJson(Map.of("data", updated.asMap()))
            );
        } catch (Exception e) {
            return Map.of(
                    "statusCode", 400,
                    "body", gson.toJson(Map.of("error", e.getMessage()))
            );
        }
    }
}
