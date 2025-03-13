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

public class MappingsUpdateHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final DynamoDB dynamoDB;
    private final String mappingsTable;
    private final Gson gson = new Gson();

    public MappingsUpdateHandler() {
        this.dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient());
        this.mappingsTable = System.getenv("MAPPINGS_TABLE");
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> stringObjectMap, Context context) {
        try {
            Map<String,String> pathParams = (Map) stringObjectMap.get("pathParameters");
            String mappingId = pathParams.get("mappingId");

            Item existing = dynamoDB.getTable(mappingsTable)
                    .getItem("mappingId", mappingId);
            if (existing == null) {
                return Map.of(
                        "statusCode", 404,
                        "body", gson.toJson(Map.of("error", "mapping not found"))
                );
            }

            // Request body
            Map<String,Object> requestBody = gson.fromJson((String) stringObjectMap.get("body"), Map.class);
            if (!requestBody.containsKey("journalAccess") || !requestBody.containsKey("status")) {
                return Map.of(
                        "statusCode", 400,
                        "body", gson.toJson(Map.of("error", "Missing required fields"))
                );
            }

            // Update logic
            UpdateItemSpec updateSpec = new UpdateItemSpec()
                    .withPrimaryKey("mappingId", mappingId)
                    .withUpdateExpression("set journalAccess = :ja, #st = :st")
                    .withNameMap(Map.of("#st", "status"))
                    .withValueMap(Map.of(
                            ":ja", requestBody.get("journalAccess"),
                            ":st", requestBody.get("status")
                    ))
                    .withReturnValues(ReturnValue.ALL_NEW);

            Item updated = dynamoDB.getTable(mappingsTable)
                    .updateItem(updateSpec)
                    .getItem();

            return Map.of(
                    "statusCode", 200,
                    "body", gson.toJson(Map.of("data", updated.asMap()))
            );
        }catch (Exception e) {
            return Map.of(
                    "statusCode", 500,
                    "body", gson.toJson(Map.of(
                            "error", "Update failed",
                            "details", e.getMessage()
                    ))
            );
        }
    }
}
