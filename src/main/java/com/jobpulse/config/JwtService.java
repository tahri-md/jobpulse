package com.jobpulse.config;


import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    @Value("${jwt.secret}")
    private  String key;
    private int EXPIRATION_TIME = 1000 * 60 *60;
     private Key getSigningKey() {
        return Keys.hmacShaKeyFor(key.getBytes());
    }

    public String generateToken(String username) {
        return Jwts.builder()
                    .setSubject(username)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis()+EXPIRATION_TIME))
                    .signWith(getSigningKey(),SignatureAlgorithm.HS256)
                    .compact();               
    }
    public String extractUsername(String jwt) {
        return Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(jwt)
                    .getBody()
                    .getSubject();
    }
}