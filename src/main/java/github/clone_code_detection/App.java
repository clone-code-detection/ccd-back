package github.clone_code_detection;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.google.inject.Guice;
import com.google.inject.Injector;
import github.clone_code_detection.server.HttpServerVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.jackson.DatabindCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hello world!
 */
public class App {
    static private final Logger logger = LoggerFactory.getLogger(App.class);
    static Module module = new Module();
    static Injector injector = Guice.createInjector(module);

    public static void main(String[] args) {
        setUp();
        deployOnAllCore(HttpServerVerticle.class);
    }

    private static void setUp() {
        DatabindCodec.mapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private static void deployOnAllCore(Class<? extends Verticle> clazz) {
        Vertx vertx = injector.getInstance(Vertx.class);
        for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
            Verticle verticle = injector.getInstance(clazz);
            vertx.deployVerticle(verticle)
                    .onSuccess(s -> {
                        logger.info("Successfully deploying verticle");
                    })
                    .onFailure(e -> {
                        logger.error("Failed to deploy verticle", e);
                    });
        }
    }
}
