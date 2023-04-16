package github.clone_code_detection.util;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class BeanProvider {
    @Value("${elasticsearch.host}")
    String host;

    @Value("${elasticsearch.port}")
    Integer port;

    @Bean
    public RestHighLevelClient getElasticsearchClient() {
        return new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, "http")));
    }
}