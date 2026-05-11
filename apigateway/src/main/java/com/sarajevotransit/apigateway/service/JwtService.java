package com.sarajevotransit.apigateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

@Service
public class JwtService {

    private final RSAPublicKey publicKey;

    public JwtService(@Value("${jwt.public-key}") Resource publicKeyResource) throws IOException {
        this.publicKey = RsaKeyConverters.x509().convert(publicKeyResource.getInputStream());
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUserId(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
