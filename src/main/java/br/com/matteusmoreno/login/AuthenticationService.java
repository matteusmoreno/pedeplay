package br.com.matteusmoreno.login;

import br.com.matteusmoreno.domain.artist.Artist;
import br.com.matteusmoreno.exception.InvalidCredentialsException;
import br.com.matteusmoreno.security.SecurityService;
import br.com.matteusmoreno.security.TokenService;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AuthenticationService {

    private final SecurityService securityService;
    private final TokenService tokenService;

    public AuthenticationService(SecurityService securityService, TokenService tokenService) {
        this.securityService = securityService;
        this.tokenService = tokenService;
    }

    public String login(LoginRequest request) {
        Artist artist = Artist.<Artist>find("email", request.email())
                .firstResultOptional()
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password."));

        if (!securityService.checkPassword(request.password(), artist.password)) {
            throw new InvalidCredentialsException("Invalid email or password.");
        }

        return tokenService.generateToken(artist);
    }
}