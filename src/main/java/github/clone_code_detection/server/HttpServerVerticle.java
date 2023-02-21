package github.clone_code_detection.server;

import com.google.inject.Inject;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;

public class HttpServerVerticle extends AbstractVerticle {
    private final Logger logger = LoggerFactory.getLogger(HttpServerVerticle.class);
    private final Router authorInfoRouter;
    private final Router cstProcessRouter;

    @Inject
    public HttpServerVerticle(@Named("author") Router authorInfoRouter ,
                              @Named("cst") Router cstProcessRouter) {
        this.authorInfoRouter = authorInfoRouter;
        this.cstProcessRouter = cstProcessRouter;
    }

    private Router createServerRouter() {
        Router router = Router.router(this.vertx);
        router.get("/api/info/*")
                .subRouter(authorInfoRouter);
        router.get("/api/cst/*")
                .subRouter(cstProcessRouter);
        router.get("/*")
                .handler(this::invalidRequestHandler);
        return router;
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        // Binds config
        JsonObject config = this.config();
        ConfigServer configServer = config
                .mapTo(ConfigServer.class);

        this.vertx.createHttpServer()
                .requestHandler(createServerRouter())
                .listen(configServer.getPort())
                .onSuccess(httpServer -> logger.info("Server listening on port {}" ,
                        httpServer.actualPort()))
                .onFailure(throwable -> logger.error("Failed to deploy server" , throwable));

        super.start(startPromise);
    }

    private void invalidRequestHandler(RoutingContext routingContext) {
        logger.warn("Invalid request path, {}" , routingContext.request()
                .uri());
        routingContext.response()
                .setStatusCode(HttpResponseStatus.NOT_FOUND.code())
                .end(HttpResponseStatus.NOT_FOUND.reasonPhrase());
    }
}