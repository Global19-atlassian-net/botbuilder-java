// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.bot.builder;

import com.microsoft.bot.connector.Attachments;
import com.microsoft.bot.connector.ConnectorClient;
import com.microsoft.bot.connector.Conversations;
import com.microsoft.bot.connector.authentication.AppCredentials;
import com.microsoft.bot.restclient.RestClient;
import com.microsoft.bot.restclient.credentials.ServiceClientCredentials;

public class MockConnectorClient implements ConnectorClient {

    private AppCredentials credentials;
    private String userAgent;

    public MockConnectorClient(String userAgent, AppCredentials credentials) {
        this.userAgent = userAgent;
        this.credentials = credentials;
    }

    private MemoryConversations conversations = new MemoryConversations();

    @Override
    public RestClient getRestClient() {
        return null;
    }

    @Override
    public String baseUrl() {
        return null;
    }

    @Override
    public ServiceClientCredentials credentials() {
        return credentials;
    }

    @Override
    public String getUserAgent() {
        return userAgent;
    }

    @Override
    public String getAcceptLanguage() {
        return null;
    }

    @Override
    public void setAcceptLanguage(String acceptLanguage) {

    }

    @Override
    public int getLongRunningOperationRetryTimeout() {
        return 0;
    }

    @Override
    public void setLongRunningOperationRetryTimeout(int timeout) {

    }

    @Override
    public boolean getGenerateClientRequestId() {
        return false;
    }

    @Override
    public void setGenerateClientRequestId(boolean generateClientRequestId) {

    }

    @Override
    public Attachments getAttachments() {
        return null;
    }

    @Override
    public Conversations getConversations() {
        return conversations;
    }

    @Override
    public void close() throws Exception {

    }
}
