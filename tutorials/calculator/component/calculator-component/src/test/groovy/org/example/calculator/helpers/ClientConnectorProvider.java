package org.example.calculator.helpers;

import org.opendolphin.core.client.ClientDolphin;
import org.opendolphin.core.client.comm.ClientConnector;
import org.opendolphin.core.client.comm.HttpClientConnector;
import org.opendolphin.core.client.comm.UiThreadHandler;
import org.opendolphin.core.comm.Codec;

import javax.inject.Inject;
import javax.inject.Provider;

public class ClientConnectorProvider implements Provider<ClientConnector> {
    @Inject
    private ClientDolphin clientDolphin;

    @Inject
    private Codec codec;

    @Inject
    private UiThreadHandler uiThreadHandler;

    @Override
    public ClientConnector get() {
        String url = "http://localhost:8080/openmdm/dolphin/v1";
        ClientConnector connector = new HttpClientConnector(clientDolphin, url);
        connector.setCodec(codec);
        connector.setUiThreadHandler(uiThreadHandler);
        clientDolphin.setClientConnector(connector);
        return connector;
    }
}
