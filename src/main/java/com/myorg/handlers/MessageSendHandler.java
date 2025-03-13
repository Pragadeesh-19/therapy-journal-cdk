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

public class MessageSendHandler implements RequestHandler<Map<String,Object>, Map<String, Object>> {

    private final DynamoDB dynamoDB;
    private final String messagesTable;
    private final Gson gson = new Gson();

    public MessageSendHandler() {
        this.dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient());
        this.messagesTable = System.getenv("MESSAGES_TABLE");
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> stringObjectMap, Context context) {
        try {
            Map<String,Object> requestBody = gson.fromJson((String) stringObjectMap.get("body"), Map.class);

            String senderId = (String) requestBody.get("senderId");
            String receiverId = (String) requestBody.get("receiverId");
            String content = (String) requestBody.get("content");

            // Generate timestamp
            String timestamp = Instant.now().toString().replace(":", "-");
            String messageId = UUID.randomUUID().toString();
            String conversationId = senderId.compareTo(receiverId) < 0
                    ? senderId + "#" + receiverId
                    : receiverId + "#" + senderId;
            String sortKey = timestamp + "#" + messageId;

            // Create item
            Item item = new Item()
                    .withPrimaryKey("conversationId", conversationId, "timestamp#messageId", sortKey)
                    .withString("messageId", messageId)
                    .withString("senderId", senderId)
                    .withString("receiverId", receiverId)
                    .withString("content", content)
                    .withString("timestamp", timestamp);

            dynamoDB.getTable(messagesTable).putItem(new PutItemSpec().withItem(item));

            return Map.of(
                    "statusCode", 201,
                    "body", gson.toJson(Map.of("data", item.asMap()))
            );
        } catch (Exception e) {
            context.getLogger().log("SEND ERROR: " + e.getMessage());
            return Map.of(
                    "statusCode", 500,
                    "body", gson.toJson(Map.of("error", e.toString()))
            );
        }
    }
}
