package dev.group.cybershield.security.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SignUpRes {
    private int id;
    private String email;
    private String name;
    private String mobileNumber;
    private String accessToken;
    private String refreshToken;
    private ZonedDateTime acExpiresIn;
    private ZonedDateTime rfExpiresIn;
    private String token_type;
}
