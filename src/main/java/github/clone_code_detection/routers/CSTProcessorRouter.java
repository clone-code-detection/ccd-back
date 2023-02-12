package github.clone_code_detection.routers;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.RouterImpl;

public class CSTProcessorRouter extends RouterImpl {
    public CSTProcessorRouter(Vertx vertx) {
        super(vertx);
        this.get("/analyze")
                .handler(getHandler());
    }

    //TODO: Implement
    private static Handler<RoutingContext> getHandler() {
        return routingContext -> routingContext.json(null);
    }
}