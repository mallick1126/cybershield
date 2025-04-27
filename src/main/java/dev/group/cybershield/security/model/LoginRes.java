package dev.group.cybershield.security.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginRes {
    private String username;
    private String accessToken;
    private String refreshToken;
    private ZonedDateTime acExpiresIn;
    private ZonedDateTime rfExpiresIn;
    private String token_type;
}
