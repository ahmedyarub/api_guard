package org.example.responder;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import static org.example.responder.Util.utilToLower;
import static org.springframework.web.servlet.function.RequestPredicates.GET;

@Configuration
public class RouterController {
    @Bean
    public RouterFunction<ServerResponse> route() {
        return RouterFunctions
                .route(
                        GET("/router/lower/{param}"),
                        request -> ServerResponse.ok()
                                .body(utilToLower(request.pathVariable("param")))
                );
    }
}
