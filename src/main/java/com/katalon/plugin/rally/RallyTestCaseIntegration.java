package com.katalon.plugin.rally;

import com.katalon.platform.api.model.Integration;

import java.util.HashMap;
import java.util.Map;

public class RallyTestCaseIntegration implements Integration {
    private String testCaseId;

    public void setTestCaseId(String testCaseId) {
        this.testCaseId = testCaseId;
    }

    @Override
    public String getName() {
        return RallyConstant.INTEGRATION_ID;
    }

    @Override
    public Map<String, String> getProperties() {
        HashMap<String, String> props = new HashMap<>();
        props.put(RallyConstant.INTEGRATION_TESTCASE_ID, testCaseId);
        return props;
    }
}
