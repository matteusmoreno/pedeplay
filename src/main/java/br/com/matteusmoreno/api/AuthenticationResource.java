package br.com.matteusmoreno.api;

import br.com.matteusmoreno.login.LoginRequest;
import br.com.matteusmoreno.login.LoginResponse;
import br.com.matteusmoreno.login.AuthenticationService;
import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/auth")
public class AuthenticationResource {

    private final AuthenticationService authService;

    public AuthenticationResource(AuthenticationService authService) {
        this.authService = authService;
    }

    @POST
    @Path("/login")
    public Response login(@Valid LoginRequest request) {
        String token = authService.login(request);
        return Response.ok(new LoginResponse(token)).build();
    }
}