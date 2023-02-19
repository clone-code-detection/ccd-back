package github.clone_code_detection.routers;

import co.elastic.clients.elasticsearch.indices.analyze.AnalyzeToken;
import github.clone_code_detection.repo.RepoElasticsearch;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.RouterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CSTProcessorRouter extends RouterImpl {
    private static final Logger logger = LoggerFactory.getLogger(CSTProcessorRouter.class);

    public CSTProcessorRouter(Vertx vertx , RepoElasticsearch repoElasticsearch) {
        super(vertx);
        Handler<RoutingContext> javaRoutingHandler =
                getHandlingRequest(repoElasticsearch::analyzeJavaDocument);
        this.get("/analyze/java")
                .handler(javaRoutingHandler);
        Handler<RoutingContext> cSharpRoutingHandler =
                getHandlingRequest(repoElasticsearch::analyzeCSharpTokens);
        this.get("/analyze/c-sharp")
                .handler(cSharpRoutingHandler);
    }

    private Handler<RoutingContext> getHandlingRequest(
            Function<String, Collection<AnalyzeToken>> analyzer) {
        return routingContext -> {
            routingContext.request()
                    .body()
                    .map(Buffer::toString)
                    .map(analyzer)
                    .map(analyzeTokens -> analyzeTokens.stream()
                            .map(AnalyzeToken::token)
                            .collect(Collectors.toList()))
                    .otherwise(new ArrayList<>())
                    .onSuccess(routingContext::json)
                    .onFailure(throwable -> {
                        logger.error("Error handling request" , throwable);
                    });
        };
    }
}