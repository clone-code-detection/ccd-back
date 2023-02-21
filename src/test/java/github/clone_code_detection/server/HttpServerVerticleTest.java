package github.clone_code_detection.server;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

//https://vertx.io/docs/vertx-junit5/java/
@ExtendWith(VertxExtension.class)
class HttpServerVerticleTest {

    public static final String EXPECTED =
            "We are two students from Ho Chi Minh " + "city " + "University of " + "Science.";

    @Test
    void testHttpServer(Vertx vertx , VertxTestContext vertxTestContext) {
        Injector injector = Guice.createInjector();
        HttpServerVerticle httpServerVerticle = injector.getInstance(HttpServerVerticle.class);
        DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject());
        Checkpoint serverSucceeded = vertxTestContext.checkpoint();
        Checkpoint allSucceeded = vertxTestContext.checkpoint();
        Checkpoint infoSucceeded = vertxTestContext.checkpoint();
        vertx.deployVerticle(httpServerVerticle , options , id -> {
            serverSucceeded.flag();

            HttpClient client = vertx.createHttpClient();
            client.request(HttpMethod.GET , 8080 , "localhost" , "/api/info/all")
                    .compose(req -> req.send()
                            .compose(HttpClientResponse::body))
                    .onSuccess(buffer -> vertxTestContext.verify(() -> {
                        assertThat(buffer.toString()).isEqualTo(EXPECTED);
                        allSucceeded.flag();
                    }));
            client.request(HttpMethod.GET , 8080 , "localhost" , "/api/info/authors/1")
                    .compose(req -> req.send()
                            .compose(HttpClientResponse::body))
                    .onSuccess(buffer -> vertxTestContext.verify(() -> {
                        assertThat(buffer.toString()).isEqualTo("Ha Hai Nguyen");
                        infoSucceeded.flag();
                    }));
        });
    }
}