package dev.group.cybershield.security.config;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;


public class JwtAutheticationToken extends AbstractAuthenticationToken {

    private final String jwtToken;

    private final String username;

    public JwtAutheticationToken(String jwtToken) {
        super(null);
        super.setAuthenticated(false);
        this.username = null;
        this.jwtToken = jwtToken;
    }

    public JwtAutheticationToken(String jwtToken, String username, boolean isAutheticated) {
        super(null);
        this.jwtToken = jwtToken;
        this.username = username;
        super.setAuthenticated(isAutheticated);
    }

    public JwtAutheticationToken(String jwtToken, String username, Collection<? extends GrantedAuthority> authorities, boolean isAutheticated) {
        super(authorities);
        this.jwtToken = jwtToken;
        this.username = username;
        super.setAuthenticated(isAutheticated);
    }

    @Override
    public Object getCredentials() {
        return this.username;
    }

    @Override
    public Object getPrincipal() {
        return this.jwtToken;
    }

}
