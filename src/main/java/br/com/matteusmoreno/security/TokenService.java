package br.com.matteusmoreno.security;

import br.com.matteusmoreno.domain.artist.Artist;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

@ApplicationScoped
public class TokenService {

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    public String generateToken(Artist artist) {
        Set<String> roles = new HashSet<>();
        roles.add("ARTIST");

        return Jwt.issuer(issuer)
                .upn(artist.email)
                .subject(artist.id.toString())
                .groups(roles)
                .expiresIn(Duration.ofHours(24))
                .sign();
    }
}