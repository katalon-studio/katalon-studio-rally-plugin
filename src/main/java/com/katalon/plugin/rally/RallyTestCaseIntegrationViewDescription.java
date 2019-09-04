package com.katalon.plugin.rally;

import com.katalon.platform.api.exception.ResourceException;
import com.katalon.platform.api.extension.TestCaseIntegrationViewDescription;
import com.katalon.platform.api.model.ProjectEntity;
import com.katalon.platform.api.preference.PluginPreference;
import com.katalon.platform.api.service.ApplicationManager;

public class RallyTestCaseIntegrationViewDescription implements TestCaseIntegrationViewDescription {

    @Override
    public String getName() {
        return "Rally";
    }

    @Override
    public Class<? extends TestCaseIntegrationView> getTestCaseIntegrationView() {
        return RallyTestCaseIntegrationView.class;
    }

    @Override
    public boolean isEnabled(ProjectEntity projectEntity) {
        try {
            PluginPreference pluginPreference = ApplicationManager.getInstance()
                    .getPreferenceManager()
                    .getPluginPreference(projectEntity.getId(), RallyConstant.PLUGIN_ID);
            if (pluginPreference == null) {
                return false;
            }
            return pluginPreference.getBoolean(RallyConstant.PREF_RALLY_ENABLED, false);
        } catch (ResourceException e) {
            return false;
        }
    }
}
