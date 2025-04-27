package dev.group.cybershield.security.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SignUpReq {

    @NotBlank(message = "Name is mandatory !")
    private String name;

    @Email
    @NotBlank(message = "Email is mandatory !")
    private String email;

    @Size(max = 10, min = 10, message = "Mobile number should have only 10 digits")
    private String mobileNumber;

    @NotBlank(message = "Password is mandatory !")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[\\W_]).{8,}$",
    message = "Password should contain at least one uppercase, one lowercase, one digit and one special character and minimum length of 8 characters")
    private String password;

}
