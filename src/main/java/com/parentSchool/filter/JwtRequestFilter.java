package com.parentSchool.filter;

import com.parentSchool.service.JwtService;
import com.parentSchool.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtRequestFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    private static List<String> PUBLIC_URLS = List.of("/api/auth/login",
            "/api/auth/register",
            "/api/organigramme/public",
            "/h2-console");
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();
        
        if(PUBLIC_URLS.contains(path) || path.startsWith("/h2-console")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String jwt = null;
        String email = null;

        final String authHeader = request.getHeader("Authorization");
        if(authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
        }
        
        if(jwt == null) {
            Cookie[] cookies = request.getCookies();
            if(cookies != null) {
                for(Cookie cookie : cookies) {
                    if("jwt".equals(cookie.getName())) {
                        jwt = cookie.getValue();
                        break;
                    }
                }
            }
        }
        
        if(jwt != null && !jwt.trim().isEmpty()) {
            try {
                email = jwtService.extractEmail(jwt);
                if(email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                    if(jwtService.validateToken(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (Exception e) {
                log.warn("Token JWT invalide: " + e.getMessage());
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
