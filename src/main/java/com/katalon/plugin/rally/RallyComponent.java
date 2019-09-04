package com.katalon.plugin.rally;

import com.katalon.platform.api.exception.ResourceException;
import com.katalon.platform.api.preference.PluginPreference;
import com.katalon.platform.api.service.ApplicationManager;

public interface RallyComponent {
    default PluginPreference getPluginStore() throws ResourceException {
        PluginPreference pluginStore = ApplicationManager.getInstance().getPreferenceManager().getPluginPreference(
                ApplicationManager.getInstance().getProjectManager().getCurrentProject().getId(),
                RallyConstant.PLUGIN_ID);
        return pluginStore;
    }
}
