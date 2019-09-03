package com.katalon.plugin.testrail;

import com.rallydev.rest.RallyRestApi;

public class RallyConnector {
    private String url;

    private String apiKey;

    private String workspace;

    private RallyRestApi rallyRestApi;

    public RallyConnector(String url, String apiKey) {
        this.url = url;
        this.apiKey = apiKey;
    }

    public RallyConnector(String url, String apiKey, String workspace) {
        this.url = url;
        this.apiKey = apiKey;
        this.workspace = workspace;
    }
}
