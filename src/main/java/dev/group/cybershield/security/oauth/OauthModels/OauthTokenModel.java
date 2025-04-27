package dev.group.cybershield.security.oauth.OauthModels;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OauthTokenModel {
    private String provider;
    private String accessToken;
    private String tokenType;
    private String scope;
    private String expiresIn;
    private String idToken;
    private String refreshToken;
}
