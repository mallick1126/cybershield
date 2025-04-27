package dev.group.cybershield.security.config;


import dev.group.cybershield.security.model.Permission;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class GrantedPermission implements GrantedAuthority {

    Permission permission;

    @Override
    public String getAuthority() {
        return this.permission.name();
    }
}
