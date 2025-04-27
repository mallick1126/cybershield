package dev.group.cybershield.security.model;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class UserInfoModel {

    private int id;

    private String name;

    private String email;

    private String mobileNumber;

    private String password;

    private String status;

    private Set<Role> roles;

}
