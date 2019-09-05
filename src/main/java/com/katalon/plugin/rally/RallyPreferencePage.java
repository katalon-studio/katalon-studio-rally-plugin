package com.katalon.plugin.rally;

import com.katalon.plugin.rally.model.RallyWorkspace;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Combo;

import com.katalon.platform.api.exception.ResourceException;
import com.katalon.platform.api.preference.PluginPreference;
import com.katalon.platform.api.service.ApplicationManager;
import com.katalon.platform.api.ui.UISynchronizeService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class RallyPreferencePage extends PreferencePage implements RallyComponent {

    private Button chckEnableIntegration;

    private Group grpAuthentication;

    private Group grpSelect;

    private Text txtApiKey;

    private Text txtUrl;

    private Combo cbbWorkspace;

    private Composite container;

    private Button btnTestConnection;

    private Label lblConnectionStatus;

    private Thread thread;

    private String workspaceRefFromConfig;

    List<RallyWorkspace> workspaces = new ArrayList<>();

    @Override
    protected Control createContents(Composite composite) {
        container = new Composite(composite, SWT.NONE);
        container.setLayout(new GridLayout(1, false));

        chckEnableIntegration = new Button(container, SWT.CHECK);
        chckEnableIntegration.setText("Using Rally");

        grpAuthentication = new Group(container, SWT.NONE);
        grpAuthentication.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        GridLayout glAuthentication = new GridLayout(2, false);
        glAuthentication.horizontalSpacing = 15;
        glAuthentication.verticalSpacing = 10;
        grpAuthentication.setLayout(glAuthentication);
        grpAuthentication.setText("Authentication");

        createLabel("URL");
        txtUrl = createTextbox();

        createLabel("Api Key");
        txtApiKey = createPasswordTextbox();

        createSelectGroup();

        btnTestConnection = new Button(grpAuthentication, SWT.PUSH);
        btnTestConnection.setText("Connect");
        btnTestConnection.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                connect(
                        txtUrl.getText(),
                        txtApiKey.getText()
                );
            }
        });

        lblConnectionStatus = new Label(grpAuthentication, SWT.NONE);
        lblConnectionStatus.setText("");
        lblConnectionStatus.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));

        handleControlModifyEventListeners();
        initializeInput();

        return container;
    }

    private void createSelectGroup() {
        grpSelect = new Group(container, SWT.NONE);
        grpSelect.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        GridLayout glGrpSelect = new GridLayout(2, false);
        glGrpSelect.horizontalSpacing = 15;
        glGrpSelect.verticalSpacing = 10;
        grpSelect.setLayout(glGrpSelect);
        grpSelect.setText("Select");

        Label lblWorkspace = new Label(grpSelect, SWT.NONE);
        lblWorkspace.setText("Workspace");
        lblWorkspace.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

        cbbWorkspace = new Combo(grpSelect, SWT.READ_ONLY);
        cbbWorkspace.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    private Text createTextbox() {
        Text text = new Text(grpAuthentication, SWT.BORDER);
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.widthHint = 200;
        text.setLayoutData(gridData);
        return text;
    }

    private Text createPasswordTextbox() {
        Text text = new Text(grpAuthentication, SWT.PASSWORD | SWT.BORDER);
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.widthHint = 200;
        text.setLayoutData(gridData);
        return text;
    }

    private void createLabel(String text) {
        Label label = new Label(grpAuthentication, SWT.NONE);
        label.setText(text);
        GridData gridData = new GridData(SWT.LEFT, SWT.TOP, false, false);
        label.setLayoutData(gridData);
    }

    private void connect(String url, String apiKey) {
        btnTestConnection.setEnabled(false);
        lblConnectionStatus.setForeground(lblConnectionStatus.getDisplay().getSystemColor(SWT.COLOR_BLACK));
        lblConnectionStatus.setText("Connecting...");
        thread = new Thread(() -> {
            try {
                // test connection here
                RallyConnector connector = new RallyConnector(url, apiKey);
                workspaces = connector.getWorkspaces();
                syncExec(() -> {
                    lblConnectionStatus
                            .setForeground(lblConnectionStatus.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
                    lblConnectionStatus.setText("Succeeded!");
                    String[] wpNames = workspaces.stream().map(RallyWorkspace::getName).toArray(String[]::new);
                    cbbWorkspace.setItems(wpNames);
                    if (!workspaces.isEmpty()) {
                        int idx = IntStream.range(0, workspaces.size())
                                .filter(i -> workspaceRefFromConfig.equals(workspaces.get(i).getRef()))
                                .findFirst()
                                .orElse(-1);
                        cbbWorkspace.select(idx);
                    }
                });
            } catch (Exception e) {
                System.err.println("Cannot connect to Rally.");
                e.printStackTrace(System.err);
                syncExec(() -> {
                    lblConnectionStatus
                            .setForeground(lblConnectionStatus.getDisplay().getSystemColor(SWT.COLOR_DARK_RED));
                    lblConnectionStatus.setText("Failed: " + e.getMessage());
                });
            } finally {
                syncExec(() -> btnTestConnection.setEnabled(true));
            }
        });
        thread.start();
    }

    void syncExec(Runnable runnable) {
        if (lblConnectionStatus != null && !lblConnectionStatus.isDisposed()) {
            ApplicationManager.getInstance()
                    .getUIServiceManager()
                    .getService(UISynchronizeService.class)
                    .syncExec(runnable);
        }
    }

    private void handleControlModifyEventListeners() {
        chckEnableIntegration.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                recursiveSetEnabled(grpAuthentication, chckEnableIntegration.getSelection());
                recursiveSetEnabled(grpSelect, chckEnableIntegration.getSelection());
            }
        });
    }

    public static void recursiveSetEnabled(Control ctrl, boolean enabled) {
        if (ctrl instanceof Composite) {
            Composite comp = (Composite) ctrl;
            for (Control c : comp.getChildren()) {
                recursiveSetEnabled(c, enabled);
                c.setEnabled(enabled);
            }
        } else {
            ctrl.setEnabled(enabled);
        }
    }

    @Override
    public boolean performOk() {
        if (!isControlCreated()) {
            return true;
        }

        try {
            PluginPreference pluginStore = getPluginStore();

            pluginStore.setBoolean(RallyConstant.PREF_RALLY_ENABLED, chckEnableIntegration.getSelection());
            pluginStore.setString(RallyConstant.PREF_RALLY_API_KEY, txtApiKey.getText());
            pluginStore.setString(RallyConstant.PREF_RALLY_URL, txtUrl.getText());
            int idxWorkspace = cbbWorkspace.getSelectionIndex();
            RallyWorkspace rallyWorkspace = workspaces.get(idxWorkspace);
            pluginStore.setString(RallyConstant.PREF_RALLY_WORKSPACE, rallyWorkspace.getRef());

            pluginStore.save();

            return true;
        } catch (ResourceException e) {
            MessageDialog.openWarning(getShell(), "Warning", "Unable to update Rally Integration Settings.");
            return false;
        }
    }

    private void initializeInput() {
        try {
            PluginPreference pluginStore = getPluginStore();

            chckEnableIntegration.setSelection(pluginStore.getBoolean(RallyConstant.PREF_RALLY_ENABLED, false));
            chckEnableIntegration.notifyListeners(SWT.Selection, new Event());

            String url = pluginStore.getString(RallyConstant.PREF_RALLY_URL, "");
            String apiKey = pluginStore.getString(RallyConstant.PREF_RALLY_API_KEY, "");
            txtUrl.setText(url);
            txtApiKey.setText(apiKey);
            workspaceRefFromConfig = pluginStore.getString(RallyConstant.PREF_RALLY_WORKSPACE, "");

            boolean isEnable = pluginStore.getBoolean(RallyConstant.PREF_RALLY_ENABLED, false);
            if (isEnable) {
                connect(url, apiKey);
            }

            container.layout(true, true);
        } catch (ResourceException e) {
            MessageDialog.openWarning(getShell(), "Warning", "Unable to update Rally Integration Settings.");
        }
    }
}
