package dev.group.cybershield.security.oauth.OauthModels;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OauthCallbackModel {
    private String provider;
    private String code;
    private String state;
}
