package com.katalon.plugin.rally;

import com.google.gson.JsonObject;
import com.katalon.platform.api.execution.TestCaseExecutionContext;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.CreateRequest;
import com.rallydev.rest.request.GetRequest;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.response.CreateResponse;
import com.rallydev.rest.response.GetResponse;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.util.QueryFilter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class RallyConnector {
    private String url;

    private String apiKey;

    private String workspace;

    private RallyRestApi rallyRestApi;

    private String userRef;

    private static String RALLY_FIELD_REF = "_ref";

    public RallyConnector(String url, String apiKey, String workspace) throws URISyntaxException, IOException {
        this.url = url;
        this.apiKey = apiKey;
        this.workspace = workspace;
        this.rallyRestApi = new RallyRestApi(new URI(url), apiKey);

        GetResponse getResponse = rallyRestApi.get(new GetRequest("/user"));
        this.userRef = getResponse.getObject().get(RALLY_FIELD_REF).getAsString();
    }

    public String query(String type, QueryFilter filter) throws IOException {
        System.out.println("Rally: Query " + type);
        QueryRequest queryRequest = new QueryRequest(type);
        queryRequest.setQueryFilter(filter);
        QueryResponse queryResponse = rallyRestApi.query(queryRequest);
        String ref = queryResponse.getResults().get(0).getAsJsonObject().get(RALLY_FIELD_REF).getAsString();
        return ref;
    }

    public void createTestCaseResult(String build, String testCaseRef, TestCaseExecutionContext context) throws IOException {
        System.out.println("Create Test Case Result...");
        JsonObject newTestCaseResult = new JsonObject();
        String status = context.getTestCaseStatus();
        ZonedDateTime zdt = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(context.getStartTime()),
                ZoneId.systemDefault()
        );
        long duration = context.getEndTime() - context.getStartTime();
        System.out.println("StartTime " + zdt.format(DateTimeFormatter.ISO_INSTANT));
        System.out.println("Duration " + duration);
        System.out.println("StackTrace " + context.getMessage());
        newTestCaseResult.addProperty("Build", build);
        newTestCaseResult.addProperty("Verdict", RallyHelper.convertToRallyStatus(status));
        newTestCaseResult.addProperty("Notes", context.getMessage());
        newTestCaseResult.addProperty("Date", zdt.format(DateTimeFormatter.ISO_INSTANT));
        newTestCaseResult.addProperty("Duration", duration);
        newTestCaseResult.addProperty("Tester", this.userRef);
        newTestCaseResult.addProperty("TestCase", testCaseRef);

        CreateRequest createRequest = new CreateRequest("testcaseresult", newTestCaseResult);
        CreateResponse createResponse = rallyRestApi.create(createRequest);
        if (createResponse.wasSuccessful()) {
            System.out.println("Successfully.");
        } else {
            String[] createErrors;
            createErrors = createResponse.getErrors();
            System.out.println("Error occurred creating Test Case Result: ");
            for (String error : createErrors) {
                System.out.println(error);
            }
        }
    }
}
