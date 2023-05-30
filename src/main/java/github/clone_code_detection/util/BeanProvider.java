package github.clone_code_detection.util;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;


@Configuration
public class BeanProvider {
    @Value("${elasticsearch.host}")
    String host;

    @Value("${elasticsearch.port}")
    Integer port;

    @Value("${moodle.host}")
    String moodleHost;
    @Value("${moodle.port}")
    Integer moodlePort;
    @Value("${moodle.protocol}")
    String moodleProtocol;

    @Bean
    public RestHighLevelClient getElasticsearchClient() {
        return new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, "http")));
    }

    @Bean
    public RestTemplate getMoodleClient() {
        String uri = String.format("%s://%s:%d/moodle", moodleProtocol, moodleHost, moodlePort);
        DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory();
        defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);
        return new RestTemplateBuilder().rootUri(uri).uriTemplateHandler(defaultUriBuilderFactory).build();
    }
}