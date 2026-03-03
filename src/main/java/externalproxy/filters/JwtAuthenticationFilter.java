package externalproxy.filters;

import externalproxy.domain.Admin;
import externalproxy.service.AdminService;
import externalproxy.service.auth.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter{


        private final AntPathMatcher pathMatcher = new AntPathMatcher();

        private final JwtService jwtService;
        private final AdminService adminService;

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            String path = request.getRequestURI();
            String method = request.getMethod();
            // Skip JWT processing for public endpoints (permitAll in SecurityConfig),
            // so an expired/invalid Authorization header doesn't break public calls.
            boolean skip = pathMatcher.match("/api/auth/**", path);
            if (pathMatcher.match("/api/reviews/**", path) || pathMatcher.match("/api/auth/**", path)) {
                log.debug("JWT_FILTER shouldNotFilter method={}, uri={}, skip={}", method, path, skip);
            }
            return skip;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain)
                throws ServletException, IOException {

            String method = request.getMethod();
            String uri = request.getRequestURI();
            String contentType = request.getContentType();
            final String authHeader = request.getHeader("Authorization");
            final String jwt;
            final String userEmail;
            boolean authHeaderPresent = StringUtils.isNotEmpty(authHeader);
            log.debug("JWT_FILTER enter method={}, uri={}, contentType={}, authHeaderPresent={}",
                    method, uri, contentType, authHeaderPresent);

            if (StringUtils.isEmpty(authHeader) || !StringUtils.startsWith(authHeader, "Bearer ")) {
                log.debug("JWT_FILTER skip method={}, uri={}, reason=no_bearer_token", method, uri);
                filterChain.doFilter(request, response);
                return;
            }

            jwt = authHeader.substring(7);
            log.debug("JWT_FILTER token extracted method={}, uri={}, tokenPrefix={}", method, uri, tokenPrefix(jwt));
            try {
                userEmail = jwtService.extractUserEmail(jwt);
            }catch (ExpiredJwtException e) {
                log.warn("JWT_FILTER reject method={}, uri={}, reason=token_expired, message={}",
                        method, uri, e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setCharacterEncoding("UTF-8");
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"TOKEN_EXPIRED\"}");
                return;
            } catch (JwtException | IllegalArgumentException e) {
                log.warn("JWT_FILTER reject method={}, uri={}, reason=invalid_token, message={}",
                        method, uri, e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setCharacterEncoding("UTF-8");
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"INVALID_TOKEN\"}");
                return;
            }

            if (StringUtils.isNotEmpty(userEmail) && SecurityContextHolder.getContext().getAuthentication() == null) {
                Admin admin = adminService.loadUserByUsername(userEmail);
                if (jwtService.isTokenValid(jwt, admin)) {
                    log.info("JWT_FILTER authenticated method={}, uri={}, userEmail={}", method, uri, userEmail);
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            admin, null, admin.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    context.setAuthentication(authToken);
                    log.debug("JWT_FILTER authorities method={}, uri={}, authorities={}",
                            method, uri, admin.getAuthorities());
                    SecurityContextHolder.setContext(context);
                }
            }

            filterChain.doFilter(request, response);
        }

        private String tokenPrefix(String jwt) {
            if (StringUtils.isEmpty(jwt)) {
                return "<empty>";
            }
            int len = Math.min(jwt.length(), 8);
            return jwt.substring(0, len) + "...";
        }
}
