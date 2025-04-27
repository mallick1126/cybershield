package dev.group.cybershield.security.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.util.StringUtils;

@Data
@Builder
public class LoginReq {
    String username;
    String password;
    String refreshToken;
    String authCode;
    String returnedState;
    String authCachedKey;

    public boolean isUsingUsernameAndPassword(){
        return StringUtils.hasText(this.getUsername()) && StringUtils.hasText(this.getPassword());
    }

    public boolean isUsingRefreshToken(){
        return StringUtils.hasText(this.getRefreshToken());
    }

    public boolean isUsingOauth(){
        return StringUtils.hasText(this.authCode) && StringUtils.hasText(this.returnedState) && StringUtils.hasText(this.authCachedKey);
    }
}
