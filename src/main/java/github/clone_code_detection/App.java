package github.clone_code_detection;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.google.inject.Guice;
import com.google.inject.Injector;
import github.clone_code_detection.server.HttpServerVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.jackson.DatabindCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hello world!
 */
public class App {
    static private final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        DatabindCodec.mapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES , false);
        Injector injector = Guice.createInjector();

        Vertx vertx = injector.getInstance(Vertx.class);
        HttpServerVerticle httpServerVerticle = injector.getInstance(HttpServerVerticle.class);
        //TODO: Deploy on all core to maximize performance
        vertx.deployVerticle(httpServerVerticle)
                .onSuccess(s -> {
                    logger.info("Successfully deploying verticle");
                })
                .onFailure(e -> {
                    logger.error("Failed to deploy verticle" , e);
                });
    }
}
