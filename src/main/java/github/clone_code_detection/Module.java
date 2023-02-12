package github.clone_code_detection;

import com.google.inject.AbstractModule;
import io.vertx.core.Vertx;

public class Module extends AbstractModule {
    @Override
    protected void configure() {
        Vertx vertx = Vertx.vertx();
        bind(Vertx.class).toInstance(vertx);
    }
}
