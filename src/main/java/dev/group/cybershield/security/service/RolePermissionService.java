package dev.group.cybershield.security.service;


import dev.group.cybershield.security.config.GrantedPermission;
import dev.group.cybershield.security.model.Permission;
import dev.group.cybershield.security.model.Role;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Data
public class RolePermissionService {

    public  Map<Role, Set<Permission>> roleToPermissionMap = new HashMap<>();

    @PostConstruct
    public void givePermissionsToRole(){
        roleToPermissionMap.put(Role.STANDARD_USER,Set.of(Permission.ALL));
    }

    public Set<Role> convertToRoleEnums(List<String> roleStr){
        if(roleStr == null || roleStr.isEmpty()){
            return null;
        }
        return roleStr.stream()
                .map(String::toUpperCase)
                .map(Role::valueOf)
                .collect(Collectors.toSet());
    }

    public Set<Permission> getPermissionOfRoles(Set<Role> roleEnums){
        return roleEnums.stream()
                .map(role -> this.getRoleToPermissionMap().get(role))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    public Set<GrantedPermission> getGrantedPermissions(Set<Role> roleEnums){
        Set<Permission> permissionEnums = getPermissionOfRoles(roleEnums);
        return permissionEnums.stream()
                .map(GrantedPermission::new)
                .collect(Collectors.toSet());
    }

}
