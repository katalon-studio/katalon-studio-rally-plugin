package com.katalon.plugin.rally;

import com.katalon.platform.api.extension.ToolItemDescription;
import com.katalon.platform.api.service.ApplicationManager;
import com.katalon.platform.api.ui.DialogActionService;

public class RallyToolItemDescription implements ToolItemDescription {

    @Override
    public String name() {
        return "Rally";
    }

    @Override
    public String toolItemId() {
        return RallyConstant.PLUGIN_ID + ".rallyToolItem";
    }

    @Override
    public String iconUrl() {
        return "platform:/plugin/" + RallyConstant.PLUGIN_ID + "/icons/icon.png";
    }

    @Override
    public void handleEvent() {
        ApplicationManager.getInstance().getUIServiceManager().getService(DialogActionService.class).openPluginPreferencePage(
                RallyConstant.PREF_PAGE_ID);
    }

    @Override
    public boolean isItemEnabled() {
        return true;
    }
}
