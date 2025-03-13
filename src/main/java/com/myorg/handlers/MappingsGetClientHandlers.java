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

public class MappingsGetClientHandlers implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final DynamoDB dynamoDB;
    private final String mappingsTable;
    private final Gson gson = new Gson();

    public MappingsGetClientHandlers() {
        this.dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient());
        this.mappingsTable = System.getenv("MAPPINGS_TABLE");
    }


    @Override
    public Map<String, Object> handleRequest(Map<String, Object> stringObjectMap, Context context) {
        try {
            Map<String,String> pathParams = (Map) stringObjectMap.get("pathParameters");
            String clientId = pathParams.get("clientId");

            // Query GSI
            QuerySpec querySpec = new QuerySpec()
                    .withKeyConditionExpression("clientId = :v_id")
                    .withValueMap(Map.of(":v_id", clientId));

            List<Map<String, Object>> mappings = new ArrayList<>();
            dynamoDB.getTable(mappingsTable)
                    .getIndex("clientMappingGSI")
                    .query(querySpec)
                    .forEach(item -> mappings.add(item.asMap()));

            return Map.of(
                    "statusCode", 200,
                    "body", gson.toJson(Map.of("data", mappings))
            );
        } catch (Exception e) {
            return Map.of(
                    "statusCode", 404,
                    "body", gson.toJson(Map.of("error", "No mappings found"))
            );
        }
    }
}
