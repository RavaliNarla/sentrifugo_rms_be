package com.bob.commonutil.util;

import com.bob.db.util.DBConstants;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private String jwtExpiration;


    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    // Access token uses userId (UUID) as subject
    public String generateAccessToken(UUID userId,String username) {
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim(DBConstants.USERNAME, username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + Long.parseLong(jwtExpiration)))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }


    public Cookie createCookie(String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);  // IMPORTANT for localhost
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", "None");
        return cookie;
    }


    public UUID getUserId(String token) {
        String subject = Jwts.parser()
                .setSigningKey(key())
                .parseClaimsJws(token)
                .getBody()
                .getSubject();

        return UUID.fromString(subject);
    }

    public String getUsername(String token) {
        return Jwts.parser()
                .setSigningKey(key())
                .parseClaimsJws(token)
                .getBody()
                .get(DBConstants.USERNAME, String.class);
    }

    public boolean validate(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
    public UUID extractUserIdFromCookie(HttpServletRequest request) {

        if (request == null || request.getCookies() == null) {
            return null;
        }

        //Appconstants addition
        for (Cookie cookie : request.getCookies()) {
            if (DBConstants.COOKIE_ACCESS_TOKEN.equals(cookie.getName())) {

                String token = cookie.getValue();

                if (validate(token)) {
                    return getUserId(token); // UUID from JWT subject
                }
            }
        }

        return null;
    }
}
