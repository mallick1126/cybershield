package dev.group.cybershield.common.constants;

public interface Constants {
    String[] excludedUrls = {"/public/**","/cybershield/public/**","/error/**","/callback/**","/favicon.ico"};
    String accessTokenNamePrefix = "_token_ac_";
    String refreshTokenNamePrefix = "_token_rf_";
    String bearerTokenKey = "Bearer";
}
