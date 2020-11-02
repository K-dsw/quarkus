package io.quarkus.rest.server.runtime.providers.exceptionmappers;

import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.core.Response;

import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.server.runtime.spi.QuarkusRestExceptionMapper;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticator;
import io.vertx.ext.web.RoutingContext;

/**
 * TODO: We'll probably need to make QuarkusRestExceptionMapper work in an async manner as
 * this implementation blocks
 */
public class AuthenticationFailedExceptionMapper implements QuarkusRestExceptionMapper<AuthenticationFailedException> {

    @Override
    public Response toResponse(AuthenticationFailedException exception) {
        return doToResponse(CDI.current().select(CurrentVertxRequest.class).get().getCurrent());
    }

    @Override
    public Response toResponse(AuthenticationFailedException exception, QuarkusRestRequestContext ctx) {
        return doToResponse(ctx.getContext());
    }

    private Response doToResponse(RoutingContext routingContext) {
        if (routingContext != null) {
            HttpAuthenticator authenticator = routingContext.get(HttpAuthenticator.class.getName());
            if (authenticator != null) {
                ChallengeData challengeData = authenticator.getChallenge(routingContext)
                        .await().indefinitely();
                Response.ResponseBuilder status = Response.status(challengeData.status);
                if (challengeData.headerName != null) {
                    status.header(challengeData.headerName.toString(), challengeData.headerContent);
                }
                return status.build();
            }
        }
        return Response.status(Response.Status.UNAUTHORIZED).entity("Not Authenticated").build();
    }
}