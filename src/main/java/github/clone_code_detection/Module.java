package github.clone_code_detection;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import github.clone_code_detection.repo.RepoElasticsearch;
import github.clone_code_detection.routers.AuthorInfoRouter;
import github.clone_code_detection.routers.CSTProcessorRouter;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

import javax.inject.Named;

public class Module extends AbstractModule {
    @Override
    protected void configure() {
        Vertx vertx = Vertx.vertx();
        bind(Vertx.class).toInstance(vertx);
    }

    @Provides
    @Inject
    @Named("cst")
    public Router getCstRouter(Vertx vertx , RepoElasticsearch repoElasticsearch) {
        return new CSTProcessorRouter(vertx , repoElasticsearch);
    }

    @Provides
    @Inject
    @Named("author")
    public Router getAuthorRouter(Vertx vertx) {
        return new AuthorInfoRouter(vertx);
    }

    @Provides
    public ElasticsearchClient getElasticsearchClient() {
        RestClient restClient = RestClient.builder(new HttpHost("localhost" , 9200))
                .build();

        ElasticsearchTransport transport =
                new RestClientTransport(restClient , new JacksonJsonpMapper());

        return new ElasticsearchClient(transport);
    }
}
