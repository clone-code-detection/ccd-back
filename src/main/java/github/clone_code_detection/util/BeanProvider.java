package github.clone_code_detection.util;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;


@Configuration
public class BeanProvider {
    @Value("${elasticsearch.host}")
    String host;

    @Value("${elasticsearch.port}")
    Integer port;

    @Bean
    public ElasticsearchClient getElasticsearchClient() {
        RestClient restClient = RestClient.builder(new HttpHost(host, port))
                                          .build();
        ElasticsearchTransport transport =
                new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}