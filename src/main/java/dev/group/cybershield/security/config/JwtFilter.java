package dev.group.cybershield.security.config;


import dev.group.cybershield.common.utils.CommonUtils;
import dev.group.cybershield.common.constants.Constants;
import dev.group.cybershield.common.exception.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
public class JwtFilter extends OncePerRequestFilter {

    protected static final Log log = LogFactory.getLog(JwtFilter.class);

    @Autowired
    private AuthenticationManager autheticationManager;

    @Autowired
    private AuthenticationEntryPoint jwtAutheticationEntrypoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            log.info("JwtFilter_started");
            if (!isExcluded(request.getRequestURI())) {
                String jwtTokenFromRequest = CommonUtils.getToken(request);
                if (StringUtils.hasText(jwtTokenFromRequest)) {
                    JwtAutheticationToken authenticationObject = new JwtAutheticationToken(jwtTokenFromRequest);
                    Authentication authObj = autheticationManager.authenticate(authenticationObject);
                    SecurityContextHolder.getContext().setAuthentication(authObj);
                    filterChain.doFilter(request, response);
                } else {
                    throw new UnauthorizedException("Token not found");
                }
            } else {
                filterChain.doFilter(request, response);
            }
            log.info("JwtFilter_ended");
        } catch (AuthenticationException e) {
            log.error("JwtFilter_authetication_error : " + e.getMessage());
            jwtAutheticationEntrypoint.commence(request, response, e);
        } catch (UnauthorizedException e) {
            log.error("JwtFilter_UnauthorizedException : " + e.getMessage());
            response.setStatus(401);
            response.getOutputStream().write((" At Jwt Filter UnauthorizedException : " + e.getMessage()).getBytes());
        } catch (Exception e) {
            log.error("JwtFilter_authetication_error2 : " + e.getMessage());
            response.setStatus(500);
            response.getOutputStream().write((" At Jwt Filter Internal Server Error : " + e.getMessage()).getBytes());
        }
    }

    private boolean isExcluded(String uri) {
        AntPathMatcher matcher = new AntPathMatcher();
        for (String excludedUrlPatterns : Constants.excludedUrls) {
            if (matcher.match(excludedUrlPatterns, uri)) {
                return true;
            }
        }
        return false;
    }

}
