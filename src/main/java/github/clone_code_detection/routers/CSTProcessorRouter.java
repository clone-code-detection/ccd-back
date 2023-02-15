package github.clone_code_detection.routers;

import github.clone_code_detection.ExtendListener;
import github.clone_code_detection.ITokenInsightExtractor;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.RouterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class CSTProcessorRouter extends RouterImpl {
    private static final ITokenInsightExtractor extendListener = new ExtendListener();
    private static final Logger logger = LoggerFactory.getLogger(CSTProcessorRouter.class);

    private static final Handler<RoutingContext> routingContextHandler =
            routingContext -> {
                routingContext.request()
                        .body()
                        .map(Buffer::toString)
                        .map(extendListener::getTokenInsights)
                        .otherwise(new ArrayList<>())
                        .onSuccess(tokenInsights -> routingContext.end(tokenInsights.toString()))
                        .onFailure(throwable -> {
                            logger.error("Error handling request" , throwable);
                        });
            };


    public CSTProcessorRouter(Vertx vertx) {
        super(vertx);
        this.get("/analyze")
                .handler(routingContextHandler);
    }
}