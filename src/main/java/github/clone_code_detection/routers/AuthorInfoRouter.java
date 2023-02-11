package github.clone_code_detection.routers;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.RouterImpl;

import java.util.ArrayList;
import java.util.List;

public class AuthorInfoRouter extends RouterImpl {
    final static List<String> authors = new ArrayList<String>() {{
        add("Quan Trong Dinh");
        add("Ha Hai Nguyen");
    }};
    final static String statusMessage =
            String.format("Invalid id, must be integer and in range (0, %d)" , authors.size());

    private static final Handler<RoutingContext> getAuthorHandler = routingContext -> {
        String idAsString = routingContext.request()
                .getParam("id");
        try {
            int id = Integer.parseInt(idAsString);
            routingContext.end(authors.get(id));
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            routingContext.response()
                    .setStatusCode(404)
                    .setStatusMessage(statusMessage);
        }
    };
    private static final Handler<RoutingContext> welcomeHandler =
            routingContext -> routingContext.end(
                    "We are two students from Ho Chi Minh city University of Science.");

    public AuthorInfoRouter(Vertx vertx) {
        super(vertx);
        this.get("/all")
                .handler(welcomeHandler);
        this.get("/authors/:id")
                .handler(getAuthorHandler);
    }
}