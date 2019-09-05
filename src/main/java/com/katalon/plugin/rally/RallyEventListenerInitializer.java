package com.katalon.plugin.rally;

import com.katalon.platform.api.Application;
import com.katalon.platform.api.controller.TestCaseController;
import com.katalon.platform.api.exception.ResourceException;
import com.katalon.platform.api.model.Integration;
import com.katalon.platform.api.model.ProjectEntity;
import com.katalon.platform.api.model.TestCaseEntity;
import com.katalon.platform.api.service.ApplicationManager;
import com.rallydev.rest.util.QueryFilter;
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
                                    + "\nTotal test cases: " + Integer.toString(testSuiteSummary.getTotalTestCases())
                                    + "\nTotal passes: " + Integer.toString(testSuiteSummary.getTotalPasses())
                                    + "\nTotal failures: " + Integer.toString(testSuiteSummary.getTotalFailures())
                                    + "\nTotal errors: " + Integer.toString(testSuiteSummary.getTotalErrors())
                                    + "\nTotal skipped: " + Integer.toString(testSuiteSummary.getTotalSkipped()));
                    System.out.println("Rally: Summary message has been successfully sent");

                    RallyConnector connector = new RallyConnector(
                            preferences.getString(RallyConstant.PREF_RALLY_URL, ""),
                            preferences.getString(RallyConstant.PREF_RALLY_API_KEY, ""),
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
                            QueryFilter filter = new QueryFilter(RallyConstant.RALLY_FIELD_FORMATTED_ID,
                                    "=", rallyTCFormattedId);
                            String testCaseRef = connector.query(RallyConstant.RALLY_TYPE_TEST_CASE, filter);
                            connector.createTestCaseResult(testSuiteContext.getSourceId(), testCaseRef, testCaseExecutionContext);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        });
    }
}
