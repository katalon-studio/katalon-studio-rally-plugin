package com.katalon.plugin.rally;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.katalon.platform.api.extension.ToolItemDescription;
import com.katalon.platform.api.service.ApplicationManager;
import com.katalon.platform.api.ui.DialogActionService;

public class RallyToolItemDescription implements ToolItemDescription {

    private static final Bundle BUNDLE = FrameworkUtil.getBundle(RallyToolItemDescription.class);

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
        String iconPath = IconResolver.resolve(BUNDLE, "icons/icon.png", "icons-v2/rally.svg");
        return "platform:/plugin/" + RallyConstant.PLUGIN_ID + "/" + iconPath;
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
