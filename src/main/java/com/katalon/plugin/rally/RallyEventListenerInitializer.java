package com.katalon.plugin.rally;

import com.katalon.platform.api.Application;
import com.katalon.platform.api.controller.TestCaseController;
import com.katalon.platform.api.model.Integration;
import com.katalon.platform.api.model.ProjectEntity;
import com.katalon.platform.api.model.TestCaseEntity;
import com.katalon.platform.api.service.ApplicationManager;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.event.Event;

import com.katalon.platform.api.event.EventListener;
import com.katalon.platform.api.event.ExecutionEvent;
import com.katalon.platform.api.execution.TestSuiteExecutionContext;
import com.katalon.platform.api.extension.EventListenerInitializer;
import com.katalon.platform.api.preference.PluginPreference;


public class RallyEventListenerInitializer implements EventListenerInitializer, RallyComponent {

    @Override
    public void registerListener(EventListener listener) {
        listener.on(Event.class, event -> {
            try {
                PluginPreference preferences = getPluginStore();
                boolean isIntegrationEnabled = preferences.getBoolean(RallyConstant.PREF_RALLY_ENABLED, false);
                if (!isIntegrationEnabled) {
                    return;
                }
                if (ExecutionEvent.TEST_SUITE_FINISHED_EVENT.equals(event.getTopic())) {
                    ExecutionEvent eventObject = (ExecutionEvent) event.getProperty("org.eclipse.e4.data");

                    TestSuiteExecutionContext testSuiteContext = (TestSuiteExecutionContext) eventObject
                            .getExecutionContext();
                    TestSuiteStatusSummary testSuiteSummary = TestSuiteStatusSummary.of(testSuiteContext);
                    System.out.println("Rally: Start sending summary message to channel:");
                    System.out.println(
                            "Summary execution result of test suite: " + testSuiteContext.getSourceId()
                                    + "\nTotal test cases: " + testSuiteSummary.getTotalTestCases()
                                    + "\nTotal passes: " + testSuiteSummary.getTotalPasses()
                                    + "\nTotal failures: " + testSuiteSummary.getTotalFailures()
                                    + "\nTotal errors: " + testSuiteSummary.getTotalErrors()
                                    + "\nTotal skipped: " + testSuiteSummary.getTotalSkipped());

                    RallyConnector connector = new RallyConnector(
                            preferences.getString(RallyConstant.PREF_RALLY_URL, ""),
                            preferences.getString(RallyConstant.PREF_RALLY_API_KEY, "")
                    );
                    connector.setWorkspaceRef(
                            preferences.getString(RallyConstant.PREF_RALLY_WORKSPACE, "")
                    );

                    Application application = ApplicationManager.getInstance();
                    ProjectEntity project = application.getProjectManager().getCurrentProject();
                    TestCaseController controller = application.getControllerManager().getController(TestCaseController.class);
                    testSuiteContext.getTestCaseContexts().forEach(testCaseExecutionContext -> {
                        try {
                            TestCaseEntity testCaseEntity = controller.getTestCase(project, testCaseExecutionContext.getId());
                            Integration integration = testCaseEntity.getIntegration(RallyConstant.INTEGRATION_ID);
                            if (integration == null) {
                                return;
                            }
                            String rallyTCFormattedId = integration.getProperties().get(RallyConstant.INTEGRATION_TESTCASE_ID);
                            if (!StringUtils.isEmpty(rallyTCFormattedId)) {
                                String testCaseRef = connector.query(RallyConstant.RALLY_TYPE_TEST_CASE,
                                        rallyTCFormattedId);
                                if (!StringUtils.isEmpty(testCaseRef)) {
                                    connector.createTestCaseResult(testCaseRef, testCaseExecutionContext);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace(System.out);
                        }
                    });
                    System.out.println("Rally: Summary message has been successfully sent");
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        });
    }
}
