package com.katalon.plugin.rally;

import org.eclipse.jface.preference.PreferencePage;

import com.katalon.platform.api.extension.PluginPreferencePage;

public class RallyPluginPreferencePage implements PluginPreferencePage {

    @Override
    public String getName() {
        return "Rally";
    }

    @Override
    public String getPageId() {
        return "com.katalon.plugin.rally.RallyPluginPreferencePage";
    }

    @Override
    public Class<? extends PreferencePage> getPreferencePageClass() {
        return RallyPreferencePage.class;
    }

}
