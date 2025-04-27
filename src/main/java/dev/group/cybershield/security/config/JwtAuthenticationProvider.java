package dev.group.cybershield.security.config;

import dev.group.cybershield.common.exception.BadRequestException;
import dev.group.cybershield.security.model.Role;
import dev.group.cybershield.security.service.JwtService;
import dev.group.cybershield.security.service.RolePermissionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class JwtAuthenticationProvider implements AuthenticationProvider {

    protected static final Log log = LogFactory.getLog(JwtAuthenticationProvider.class);

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RolePermissionService rolePermissionService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Authentication jwtAuthentication = null;
        try{
            log.info("JwtAuthenticationProvider_authenticate_started");
            String jwtToken = (String) authentication.getPrincipal();
            if (jwtService.isValidJwtToken(jwtToken,false)) {
                String username = jwtService.extractUsernameFromJwt(jwtToken);
                Set<Role> roleEnums = rolePermissionService.convertToRoleEnums(jwtService.extractRoleFromJwt(jwtToken));
                Set<GrantedPermission> grantedPermissionsSet = rolePermissionService.getGrantedPermissions(roleEnums);
                log.info("JwtAuthenticationProvider_authenticate_ended");
                jwtAuthentication = new JwtAutheticationToken(jwtToken, username, grantedPermissionsSet, true);
            }
        }catch (BadRequestException e){
            log.error("Error in authetication provider : "+e.getMessage());
            throw e;
        }catch (Exception e){
            log.error("Error in authetication provider : "+e.getMessage());
            throw new AuthenticationException(e.getMessage()){};
        }
        return jwtAuthentication;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return JwtAutheticationToken.class.isAssignableFrom(authentication);
    }
}
