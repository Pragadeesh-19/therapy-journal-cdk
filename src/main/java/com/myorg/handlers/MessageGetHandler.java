package com.myorg.handlers;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MessageGetHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final DynamoDB dynamoDB;
    private final String messagesTable;
    private final Gson gson = new Gson();

    public MessageGetHandler() {
        this.dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient());
        this.messagesTable = System.getenv("MESSAGES_TABLE");
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> stringObjectMap, Context context) {
        try {
            Map<String,String> queryParams = (Map) stringObjectMap.get("queryStringParameters");
            String participantId = queryParams.get("participantId");

            List<Map<String, Object>> messages = new ArrayList<>();

            Index senderIndex = dynamoDB.getTable(messagesTable).getIndex("senderMessageGSI");
            QuerySpec senderQuery = new QuerySpec()
                    .withKeyConditionExpression("senderId = :pid")
                    .withValueMap(Map.of(":pid", participantId));
            senderIndex.query(senderQuery).forEach(item -> messages.add(item.asMap()));

            Index receiverIndex = dynamoDB.getTable(messagesTable).getIndex("receiverMessageGSI");
            QuerySpec receiverQuery = new QuerySpec()
                    .withKeyConditionExpression("receiverId = :pid")
                    .withValueMap(Map.of(":pid", participantId));
            receiverIndex.query(receiverQuery).forEach(item -> messages.add(item.asMap()));

            // Sort messages chronologically
            messages.sort((a, b) ->
                    ((String)a.get("timestamp")).compareTo((String)b.get("timestamp")));

            return Map.of(
                    "statusCode", 200,
                    "body", gson.toJson(Map.of("data", messages))
            );

        } catch (Exception e) {
            return Map.of(
                    "statusCode", 400,
                    "body", gson.toJson(Map.of("error", "Invalid query"))
            );
        }
    }
}
