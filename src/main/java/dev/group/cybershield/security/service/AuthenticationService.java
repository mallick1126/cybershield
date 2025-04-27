package dev.group.cybershield.security.service;

import dev.group.cybershield.common.constants.CommonConstants;
import dev.group.cybershield.common.constants.Constants;
import dev.group.cybershield.common.exception.BadRequestException;
import dev.group.cybershield.security.model.*;
import dev.group.cybershield.security.oauth.OauthModels.OauthAuthorizeModel;
import dev.group.cybershield.security.oauth.OauthModels.OauthCallbackModel;
import dev.group.cybershield.security.oauth.OauthModels.OauthTokenModel;
import dev.group.cybershield.security.oauth.OauthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class AuthenticationService {

    @Autowired
    private UserInfoModelService userInfoModelService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private OauthService oauthService;

    @Autowired
    private RolePermissionService rolePermissionService;

    public LoginRes getToken(String username, String password) {
        UserInfoModel userFoundAtOurEnd = userInfoModelService.loadUserByUsername(username);
        if (!passwordEncoder.matches(password, userFoundAtOurEnd.getPassword())) {
            throw new RuntimeException("Invalid password !");
        }

        String accessToken = jwtService.createJwtAccessToken(userFoundAtOurEnd);
        ZonedDateTime accessTokenExpiry = jwtService.extractExpiryFromJwt(accessToken);

        String refreshToken = jwtService.createJwtRefreshToken(userFoundAtOurEnd);
        ZonedDateTime refreshTokenExpiry = jwtService.extractExpiryFromJwt(refreshToken);

        return LoginRes.builder()
                .username(username)
                .token_type(Constants.bearerTokenKey).accessToken(accessToken)
                .acExpiresIn(accessTokenExpiry)
                .username(userFoundAtOurEnd.getEmail())
                .refreshToken(refreshToken)
                .rfExpiresIn(refreshTokenExpiry)
                .build();
    }

    public LoginRes getToken(String refreshToken) {
        jwtService.isValidJwtToken(refreshToken, true);

        String username = jwtService.extractUsernameFromJwt(refreshToken);
        List<String> roles = jwtService.extractRoleFromJwt(refreshToken);
        Set<Role> roleEnums = rolePermissionService.convertToRoleEnums(roles);
        UserInfoModel userInfoFromRfToken = UserInfoModel.builder()
                .email(username).roles(roleEnums).build();

        String acToken = jwtService.createJwtAccessToken(userInfoFromRfToken);
        ZonedDateTime acTokenExpiry = jwtService.extractExpiryFromJwt(acToken);

        String rfToken = jwtService.createJwtRefreshToken(userInfoFromRfToken);
        ZonedDateTime rfTokenExpiry = jwtService.extractExpiryFromJwt(rfToken);

        return LoginRes.builder()
                .username(username)
                .token_type(Constants.bearerTokenKey).accessToken(acToken)
                .acExpiresIn(acTokenExpiry)
                .refreshToken(rfToken)
                .rfExpiresIn(rfTokenExpiry)
                .build();
    }

    public LoginRes getToken(String authCode, String returnedState, String authCachedKey) {
        if (!oauthService.keyExists(authCachedKey)) {
            throw new BadRequestException("Invalid key provided !");
        }

        OauthAuthorizeModel cachedAuthorizeModel = oauthService.getCachedAuthorizeModel(authCachedKey);
        OauthCallbackModel oauthCallbackModel = OauthCallbackModel.builder().provider(cachedAuthorizeModel.getProvider()).code(authCode).state(returnedState).build();

        if (!oauthService.verifyState(oauthCallbackModel, cachedAuthorizeModel)) {
            throw new BadRequestException("Invalid state provided !");
        }

        OauthTokenModel oauthTokenModel = oauthService.getOauthTokenModel(oauthCallbackModel, cachedAuthorizeModel);
        UserInfoModel userInfoModelFromOauth = oauthService.getUserInfoModel(oauthTokenModel);

        String acToken = jwtService.createJwtAccessToken(userInfoModelFromOauth);
        ZonedDateTime acTokenExpiry = jwtService.extractExpiryFromJwt(acToken);

        String rfToken = jwtService.createJwtRefreshToken(userInfoModelFromOauth);
        ZonedDateTime rfTokenExpiry = jwtService.extractExpiryFromJwt(rfToken);

        log.info("oauthCallbackModel : {}", oauthCallbackModel);
        log.info("oauthTokenModel : {}", oauthTokenModel);

        return LoginRes.builder()
                .username(userInfoModelFromOauth.getEmail())
                .token_type(Constants.bearerTokenKey)
                .accessToken(acToken)
                .acExpiresIn(acTokenExpiry)
                .refreshToken(rfToken)
                .rfExpiresIn(rfTokenExpiry).build();
    }

    public void setTokensInResponseCookies(LoginRes loginRes, HttpServletRequest request, HttpServletResponse response) {
        if (loginRes == null || !StringUtils.hasText(loginRes.getAccessToken())) {
            throw new BadRequestException("Token not generated !");
        }
        String domain = request.getServerName();

        String accessTokenCookieName = Constants.accessTokenNamePrefix + request.getServerName();
        int accessTokenCookieExpiry = (int) (loginRes.getAcExpiresIn().toEpochSecond() - ZonedDateTime.now().toEpochSecond());
        Cookie accessTokenCookie = new Cookie(accessTokenCookieName, loginRes.getAccessToken());
        accessTokenCookie.setMaxAge(accessTokenCookieExpiry);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setDomain(domain);
        accessTokenCookie.setAttribute("SameSite", "Lax");
        response.addCookie(accessTokenCookie);

        if (StringUtils.hasText(loginRes.getRefreshToken())) {
            String refreshTokenCookieName = Constants.refreshTokenNamePrefix + request.getServerName();
            Cookie refreshTokenCookie = new Cookie(refreshTokenCookieName, loginRes.getRefreshToken());
            refreshTokenCookie.setMaxAge((int) (loginRes.getRfExpiresIn().toEpochSecond() - ZonedDateTime.now().toEpochSecond()));
            refreshTokenCookie.setPath(request.getRequestURI());
            refreshTokenCookie.setDomain(domain);
            refreshTokenCookie.setAttribute("SameSite", "Lax");
            response.addCookie(refreshTokenCookie);
        }
    }

    public SignUpRes register(SignUpReq signUpReq) throws Exception{
        try{
            UserInfoModel userInfoModel = UserInfoModel.builder()
                    .email(signUpReq.getEmail())
                    .password(passwordEncoder.encode(signUpReq.getPassword()))
                    .mobileNumber(signUpReq.getMobileNumber())
                    .name(signUpReq.getName())
                    .status(CommonConstants.STATUS_A)
                    .roles(Set.of(Role.STANDARD_USER))
                    .build();

            UserInfoModel addedUser = userInfoModelService.addUser(userInfoModel);

            String acToken = jwtService.createJwtAccessToken(addedUser);
            ZonedDateTime acTokenExpiry = jwtService.extractExpiryFromJwt(acToken);

            String rfToken = jwtService.createJwtRefreshToken(addedUser);
            ZonedDateTime rfTokenExpiry = jwtService.extractExpiryFromJwt(rfToken);

            log.info("addedUser : {}", addedUser);

            return SignUpRes.builder()
                    .id(addedUser.getId())
                    .email(addedUser.getEmail())
                    .name(addedUser.getName())
                    .mobileNumber(addedUser.getMobileNumber())
                    .token_type(Constants.bearerTokenKey)
                    .accessToken(acToken)
                    .acExpiresIn(acTokenExpiry)
                    .refreshToken(rfToken)
                    .rfExpiresIn(rfTokenExpiry).build();

        }catch (Exception e){
            log.error("Exception occured at the register {}",e.getMessage());
            throw e;
        }
    }
}
