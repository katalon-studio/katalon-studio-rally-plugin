package com.katalon.plugin.rally;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.katalon.platform.api.execution.TestCaseExecutionContext;
import com.katalon.plugin.rally.model.RallyField;
import com.katalon.plugin.rally.model.RallyWorkspace;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.CreateRequest;
import com.rallydev.rest.request.GetRequest;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.response.CreateResponse;
import com.rallydev.rest.response.GetResponse;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.util.QueryFilter;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RallyConnector {
    private String workspaceRef;

    private RallyRestApi rallyRestApi;

    private String userRef;

    public RallyConnector(String url, String apiKey) throws URISyntaxException, IOException {
        this.rallyRestApi = new RallyRestApi(new URI(url), apiKey);

        GetResponse getResponse = rallyRestApi.get(new GetRequest("/user"));
        this.userRef = getResponse.getObject().get(RallyField.REF).getAsString();
    }

    public String getWorkspaceRef() {
        return workspaceRef;
    }

    public void setWorkspaceRef(String workspaceRef) {
        this.workspaceRef = workspaceRef;
    }

    public List<RallyWorkspace> getWorkspaces() throws IOException {
        GetResponse getResponse = rallyRestApi.get(new GetRequest("/workspace"));
        JsonArray wpJsonArray = getResponse.getObject().get(RallyField.RESULTS).getAsJsonArray();
        List<RallyWorkspace> result = new ArrayList<>();
        wpJsonArray.forEach(wp -> {
            JsonObject wpJson = wp.getAsJsonObject();
            RallyWorkspace workspace = new RallyWorkspace();
            workspace.setName(wpJson.get(RallyField.NAME).getAsString());
            workspace.setRef(wpJson.get(RallyField.REF).getAsString());
            result.add(workspace);
        });
        return result;
    }

    public String query(String type, String formattedId) throws IOException {
        System.out.printf("Rally: Query %s with ID %s\n", type, formattedId);
        QueryFilter filter = new QueryFilter(RallyConstant.RALLY_FIELD_FORMATTED_ID,
                "=", formattedId);
        QueryRequest queryRequest = new QueryRequest(type);
        queryRequest.setQueryFilter(filter);
        if (!StringUtils.isEmpty(workspaceRef)) {
            queryRequest.setWorkspace(workspaceRef);
        }
        QueryResponse queryResponse = rallyRestApi.query(queryRequest);
        JsonArray results = queryResponse.getResults();
        if (results.size() > 0) {
            return results.get(0).getAsJsonObject().get(RallyField.REF).getAsString();
        } else {
            System.out.printf("Cannot find %s with ID %s\n", type, formattedId);
            return "";
        }
    }

    public void createTestCaseResult(String testCaseRef, TestCaseExecutionContext context) throws IOException {
        System.out.println("Rally: Create Test Case Result for " + context.getSourceId());
        JsonObject newTestCaseResult = new JsonObject();
        String status = context.getTestCaseStatus();
        ZonedDateTime zdt = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(context.getStartTime()),
                ZoneId.systemDefault()
        );
        long duration = context.getEndTime() - context.getStartTime();
        float durationMinute = duration / (1000F * 60F);
        newTestCaseResult.addProperty("Build", context.getSourceId());
        newTestCaseResult.addProperty("Verdict", RallyHelper.convertToRallyStatus(status));
        newTestCaseResult.addProperty("Notes", context.getMessage());
        newTestCaseResult.addProperty("Date", zdt.format(DateTimeFormatter.ISO_INSTANT));
        newTestCaseResult.addProperty("Duration", String.format("%.2f", durationMinute));
        newTestCaseResult.addProperty("Tester", this.userRef);
        newTestCaseResult.addProperty("TestCase", testCaseRef);
        if (!StringUtils.isEmpty(workspaceRef)) {
            newTestCaseResult.addProperty("Workspace", workspaceRef);
        }

        CreateRequest createRequest = new CreateRequest("testcaseresult", newTestCaseResult);
        CreateResponse createResponse = rallyRestApi.create(createRequest);
        if (!createResponse.wasSuccessful()) {
            String[] createErrors;
            createErrors = createResponse.getErrors();
            System.out.println("Error occurred creating Test Case Result: ");
            for (String error : createErrors) {
                System.out.println(error);
            }
        }
    }

    public void close() throws IOException {
        rallyRestApi.close();
    }
}
