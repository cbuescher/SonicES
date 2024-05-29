package org.soc.sonices;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.RestClient;

import java.util.Properties;

public class ESClient {
    private final ElasticsearchClient client;

    // Logger Object
    public static Logger LOGGER = LogManager.getLogger(ESClient.class);

    public ESClient(Properties configuration) {
        String serverUrl = configuration.getProperty("server_url", "localhost");
        String apiKey = configuration.getProperty("api_key", "");

        RestClient restClient = RestClient
                .builder(HttpHost.create(serverUrl))
                .setDefaultHeaders(new Header[]{
                        new BasicHeader("Authorization", "ApiKey " + apiKey)
                })
                .build();

        // Create the transport with a Jackson mapper
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());

        // And create the API client
        this.client = new ElasticsearchClient(transport);
    }

    public ElasticsearchClient getClient() {
        return client;
    }

}
