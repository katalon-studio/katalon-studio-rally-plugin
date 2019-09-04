package com.katalon.plugin.rally;

import com.rallydev.rest.RallyRestApi;

import java.net.URI;
import java.net.URISyntaxException;

public class RallyConnector {
    private String url;

    private String apiKey;

    private String workspace;

    private RallyRestApi rallyRestApi;

    public RallyConnector(String url, String apiKey, String workspace) throws URISyntaxException {
        this.url = url;
        this.apiKey = apiKey;
        this.workspace = workspace;
        this.rallyRestApi = new RallyRestApi(new URI(url), apiKey);

    }
}
