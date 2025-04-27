package dev.group.cybershield.login.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserDTO {
    private Integer userId;

    @NotBlank(message = "Email is required", groups = {UserDTO.class})
    private String email;

    @NotBlank(message = "Password is required", groups = {UserDTO.class})
    private String password;
}
