package github.clone_code_detection.util;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


@Configuration
public class BeanProvider {
    @Value("${elasticsearch.host}")
    String host;

    @Value("${elasticsearch.port}")
    Integer port;

    @Value("${thread_pool_executor.max-pool}")
    Integer executorMaxThread;
    @Value("${thread_pool_executor.capacity}")
    Integer executorQueueCapacity;

    @Bean
    public RestHighLevelClient getElasticsearchClient() {
        return new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, "http")).setRequestConfigCallback(builder -> builder
                .setConnectTimeout(60000)
                .setSocketTimeout(120000)));
    }

    @Bean
    public RestTemplate getMoodleClient() {
        DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory();
        defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);
        return new RestTemplateBuilder().uriTemplateHandler(defaultUriBuilderFactory)
                .build();
    }

    @Bean
    @Primary
    public ThreadPoolExecutor getThreadPoolExecutor() {
        ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(executorQueueCapacity);
        return new ThreadPoolExecutor(0, executorMaxThread, 60, TimeUnit.SECONDS, queue,
                new ThreadPoolExecutor.AbortPolicy());
    }

    @Bean
    @Qualifier("intra_project_query_threadpool")
    public ThreadPoolExecutor intraProjectQueryThreadPool() {
        ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(20);
        return new ThreadPoolExecutor(0, executorMaxThread, 60, TimeUnit.MINUTES, queue,
                new ThreadPoolExecutor.AbortPolicy());
    }
}