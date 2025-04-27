package dev.group.cybershield.security.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import dev.group.cybershield.common.LocalCache;
import dev.group.cybershield.common.exception.BadRequestException;
import dev.group.cybershield.common.utils.CommonUtils;
import dev.group.cybershield.security.model.Role;
import dev.group.cybershield.security.model.UserInfoModel;
import dev.group.cybershield.security.oauth.OauthModels.OauthAuthorizeModel;
import dev.group.cybershield.security.oauth.OauthModels.OauthCallbackModel;
import dev.group.cybershield.security.oauth.OauthModels.OauthTokenModel;
import dev.group.cybershield.security.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;

@Slf4j
@Service
public class OauthService {

    @Autowired
    private OauthClients oauthClients;

    @Autowired
    private LocalCache localCache;

    @Autowired
    private JwtService jwtService;

    @SneakyThrows
    public OauthAuthorizeModel getAuthorizeModel(String provider) {
        OauthClients.ClientInfo clientInfo = oauthClients.getClients().get(provider);
        if (clientInfo == null) {
            throw new BadRequestException("Provider not found");
        }

        String cachedKey = CommonUtils.generateOpaqueToken();
        String state = getState();
        String responseType = getGrantOrResponseType(clientInfo.getGrantType(), true);
        String codeVerifier = null;

        MultiValueMap<String, String> queryParameters = new LinkedMultiValueMap<>();
        queryParameters.add("response_type", responseType);
        queryParameters.add("client_id", clientInfo.getClientsId());
        queryParameters.add("redirect_uri", clientInfo.getRedirectUrl());
        queryParameters.add("scope", clientInfo.getScope());
        queryParameters.add("state", state);

        if ("authorization_code_with_pkce".equalsIgnoreCase(clientInfo.getGrantType())) {
            Map<String, String> codeVerifierMap = getCodeChallenge();
            codeVerifier = codeVerifierMap.get("codeVerifier");
            queryParameters.add("code_challenge", codeVerifierMap.get("codeChallenge"));
            queryParameters.add("code_challenge_method", codeVerifierMap.get("codeChallengeMethod"));
        }

        String authUrl = UriComponentsBuilder.fromHttpUrl(clientInfo.getAuthUrl())
                .queryParams(queryParameters)
                .build()
                .encode()
                .toUriString();

        OauthAuthorizeModel oauthAuthorizeModel = OauthAuthorizeModel.builder().provider(provider).authorizeUrl(authUrl).state(state).codeVerifier(codeVerifier).cachedKey(cachedKey).build();
        this.cacheAuthorizeModel(oauthAuthorizeModel);

        return oauthAuthorizeModel;
    }

    @SneakyThrows
    public OauthTokenModel getOauthTokenModel(OauthCallbackModel oauthCallbackModel, OauthAuthorizeModel oauthAuthorizeModel) {
        OauthClients.ClientInfo clientInfo = oauthClients.getClients().get(oauthCallbackModel.getProvider());
        if (clientInfo == null) {
            throw new BadRequestException("Provider not found");
        }
        String tokenUrl = UriComponentsBuilder.fromHttpUrl(clientInfo.getTokenUrl()).build().encode().toUriString();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        httpHeaders.setBasicAuth(clientInfo.getClientsId(), clientInfo.getClientSecret());

        MultiValueMap<String, String> httpBody = new LinkedMultiValueMap<>();
        httpBody.add("client_id", clientInfo.getClientsId());
        httpBody.add("client_secret", clientInfo.getClientSecret());
        httpBody.add("redirect_uri", clientInfo.getRedirectUrl());
        httpBody.add("grant_type", getGrantOrResponseType(clientInfo.getGrantType(), false));
        httpBody.add("code", oauthCallbackModel.getCode());

        if ("authorization_code_with_pkce".equalsIgnoreCase(clientInfo.getGrantType())) {
            String codeVerifier = oauthAuthorizeModel.getCodeVerifier();
            log.info("from_cache_codeVerifier : {}", codeVerifier);
            httpBody.add("code_verifier", codeVerifier);
        }

        try {
            HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(httpBody, httpHeaders);
            log.info("tokenResponseBody : {}", tokenRequest);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<JsonNode> tokenResponse = restTemplate.postForEntity(tokenUrl, tokenRequest, JsonNode.class);
            JsonNode tokenResponseBody = tokenResponse.getBody();
            log.info("tokenResponseBody : {}", tokenResponseBody);
            return buildOauthTokenModel(tokenResponseBody, oauthCallbackModel.getProvider());
        } catch (RestClientException e) {
            log.error("Error while getting the token from {}", oauthAuthorizeModel.getProvider());
            throw e;
        }
    }

    public OauthTokenModel buildOauthTokenModel(JsonNode jsonNode, String provider) {
        if (jsonNode == null || provider == null) {
            return null;
        }
        OauthTokenModel oauthTokenModel = OauthTokenModel.builder().provider(provider).build();
        if (jsonNode.hasNonNull("access_token")) {
            oauthTokenModel.setAccessToken(jsonNode.get("access_token").asText());
        }
        if (jsonNode.hasNonNull("token_type")) {
            oauthTokenModel.setTokenType(jsonNode.get("token_type").asText());
        }
        if (jsonNode.hasNonNull("scope")) {
            oauthTokenModel.setScope(jsonNode.get("scope").asText());
        }
        if (jsonNode.hasNonNull("expires_in")) {
            oauthTokenModel.setExpiresIn(jsonNode.get("expires_in").asText());
        }
        if (jsonNode.hasNonNull("id_token")) {
            oauthTokenModel.setIdToken(jsonNode.get("id_token").asText());
        }
        if (jsonNode.hasNonNull("refresh_token")) {
            oauthTokenModel.setRefreshToken(jsonNode.get("refresh_token").asText());
        }
        if (!StringUtils.hasText(oauthTokenModel.getAccessToken())) {
            log.error("Invalid response while getting token from {}", provider);
            throw new RuntimeException("Invalid response while getting token from " + provider);
        }
        return oauthTokenModel;
    }

    @SneakyThrows
    public UserInfoModel getUserInfoModel(OauthTokenModel oauthTokenModel) {
        if (StringUtils.hasText(oauthTokenModel.getIdToken())) {
            return buildUserInfoModelFromIdToken(oauthTokenModel);
        }

        OauthClients.ClientInfo clientInfo = oauthClients.getClients().get(oauthTokenModel.getProvider());
        if (clientInfo == null) {
            throw new BadRequestException("Provider not found");
        }

        try {
            String methodName = "getUserInfoModelFrom" + CommonUtils.toCamelCase(oauthTokenModel.getProvider());
            Method method = OauthService.class.getDeclaredMethod(methodName, OauthTokenModel.class, OauthClients.ClientInfo.class);
            method.setAccessible(true);
            return (UserInfoModel) method.invoke(new OauthService(), oauthTokenModel, clientInfo);
        } catch (InvocationTargetException e) {
            log.error("Unable to get the userInfo from {}", oauthTokenModel.getProvider());
            e.getCause().printStackTrace();
            throw e.getCause();
        } catch (Exception e) {
            log.error("Unable to get the userInfo from {}", oauthTokenModel.getProvider());
            e.printStackTrace();
            throw e;
        }
    }

    private UserInfoModel buildUserInfoModelFromIdToken(OauthTokenModel oauthTokenModel) {
        JsonNode payload = jwtService.extractPayloadFromJwt(oauthTokenModel.getIdToken(), JsonNode.class);
        UserInfoModel userInfoModel = UserInfoModel.builder().build();
        userInfoModel.setRoles(Set.of(Role.STANDARD_USER));
        if (payload.hasNonNull("email")) {
            userInfoModel.setEmail((String) payload.get("email").asText());
        }
        if (payload.hasNonNull("name")) {
            userInfoModel.setName((String) payload.get("name").asText());
        }
        if (StringUtils.hasText(userInfoModel.getEmail()) && StringUtils.hasText(userInfoModel.getName())) {
            return userInfoModel;
        }
        throw new RuntimeException("Not able to fetch required details from " + oauthTokenModel.getProvider());
    }

    private UserInfoModel getUserInfoModelFromGoogle(OauthTokenModel oauthTokenModel, OauthClients.ClientInfo clientInfo) {
        if (oauthTokenModel == null || clientInfo == null) {
            return null;
        }
        List<String> userInfoUrls = clientInfo.getUserInfoUrls();
        UserInfoModel userInfoModel = UserInfoModel.builder().build();
        for (String url : userInfoUrls) {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setBearerAuth(oauthTokenModel.getAccessToken());
            HttpEntity<Object> httpEntity = new HttpEntity<>(httpHeaders);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, httpEntity, JsonNode.class);
            JsonNode res = response.getBody();
            if (res != null && res.hasNonNull("email")) {
                userInfoModel.setEmail(res.get("email").asText());
            }
            if (res != null && res.hasNonNull("name")) {
                userInfoModel.setName(res.get("name").asText());
            }
            userInfoModel.setRoles(Set.of(Role.STANDARD_USER));
            if (StringUtils.hasText(userInfoModel.getEmail()) && StringUtils.hasText(userInfoModel.getName())) {
                return userInfoModel;
            }
        }
        throw new RuntimeException("Not able to fetch required details from " + oauthTokenModel.getProvider());
    }

    private UserInfoModel getUserInfoModelFromGithub(OauthTokenModel oauthTokenModel, OauthClients.ClientInfo clientInfo) {
        if (oauthTokenModel == null || clientInfo == null) {
            return null;
        }
        List<String> userInfoUrls = clientInfo.getUserInfoUrls();
        UserInfoModel userInfoModel = UserInfoModel.builder().build();
        for (String url : userInfoUrls) {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setBearerAuth(oauthTokenModel.getAccessToken());
            HttpEntity<Object> httpEntity = new HttpEntity<>(httpHeaders);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, httpEntity, JsonNode.class);
            JsonNode res = response.getBody();
            if (res != null && res.isArray()) {
                for (JsonNode eachObj : res) {
                    if (eachObj.hasNonNull("email") && eachObj.hasNonNull("primary") && eachObj.get("primary").asBoolean()) {
                        userInfoModel.setEmail(eachObj.get("email").asText());
                    }
                }
            }
            if (res != null && res.hasNonNull("email")) {
                userInfoModel.setEmail(res.get("email").asText());
            }
            if (res != null && res.hasNonNull("login")) {
                userInfoModel.setName(res.get("login").asText());
            }
            userInfoModel.setRoles(Set.of(Role.STANDARD_USER));
            if (StringUtils.hasText(userInfoModel.getEmail()) && StringUtils.hasText(userInfoModel.getName())) {
                return userInfoModel;
            }
        }
        throw new RuntimeException("Not able to fetch required details from " + oauthTokenModel.getProvider());
    }

    private UserInfoModel getUserInfoModelFromFacebook(OauthTokenModel oauthTokenModel, OauthClients.ClientInfo clientInfo) {
        if (oauthTokenModel == null || clientInfo == null) {
            return null;
        }
        List<String> userInfoUrls = clientInfo.getUserInfoUrls();
        UserInfoModel userInfoModel = UserInfoModel.builder().build();
        for (String url : userInfoUrls) {
            String urlWithParams = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("fields", "id,name,email,first_name,last_name,gender,birthday,hometown,location,picture,friends")
                    .build().toUriString();
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setBearerAuth(oauthTokenModel.getAccessToken());
            HttpEntity<Object> httpEntity = new HttpEntity<>(httpHeaders);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<JsonNode> response = restTemplate.exchange(urlWithParams, HttpMethod.GET, httpEntity, JsonNode.class);
            JsonNode res = response.getBody();
            if (res != null && res.hasNonNull("email")) {
                userInfoModel.setEmail(res.get("email").asText());
            }
            if (res != null && res.hasNonNull("name")) {
                userInfoModel.setName(res.get("name").asText());
            }
            userInfoModel.setRoles(Set.of(Role.STANDARD_USER));
            if (StringUtils.hasText(userInfoModel.getEmail()) && StringUtils.hasText(userInfoModel.getName())) {
                return userInfoModel;
            }
        }
        throw new RuntimeException("Not able to fetch required details from " + oauthTokenModel.getProvider());
    }

    public void cacheAuthorizeModel(OauthAuthorizeModel oauthAuthorizeModel) {
        try {
            String key = oauthAuthorizeModel.getCachedKey();
            localCache.getCached().put(key, oauthAuthorizeModel);
        } catch (Exception e) {
            log.info("Error while caching !");
            throw new RuntimeException(e);
        }
    }

    public OauthAuthorizeModel getCachedAuthorizeModel(HttpServletRequest request) {
        String key = CommonUtils.getToken(request);
        return (OauthAuthorizeModel) localCache.getCached().get(key);
    }

    public OauthAuthorizeModel getCachedAuthorizeModel(String key) {
        return (OauthAuthorizeModel) localCache.getCached().get(key);
    }

    public boolean keyExists(String key){
        return localCache.getCached().containsKey(key);
    }

    public String getState() {
        try {
            byte[] stateInByte = new byte[32];
            SecureRandom randomState = new SecureRandom();
            randomState.nextBytes(stateInByte);
            return Base64.getUrlEncoder().encodeToString(stateInByte);
        } catch (Exception e) {
            log.error("error in creating state : {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public boolean verifyState(OauthCallbackModel oauthCallbackModel, OauthAuthorizeModel cachedAuthorizeModel) {
        if (oauthCallbackModel != null && cachedAuthorizeModel != null) {
            return oauthCallbackModel.getState().equals(cachedAuthorizeModel.getState());
        }
        return false;
    }

    public Map<String, String> getCodeChallenge() {
        try {
            byte[] codeVerifierInBytes = new byte[32];
            SecureRandom randomState = new SecureRandom();
            randomState.nextBytes(codeVerifierInBytes);
            String codeVerifierBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifierInBytes);

            MessageDigest messageDigest = MessageDigest.getInstance("SHA256");
            byte[] codeChallengeInBytes = messageDigest.digest(codeVerifierBase64.getBytes(StandardCharsets.UTF_8));
            String codeChallengeBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(codeChallengeInBytes);

            Map<String, String> code = new HashMap<>();
            code.put("codeVerifier", codeVerifierBase64);
            code.put("codeChallenge", codeChallengeBase64);
            code.put("codeChallengeMethod", "S256");

            log.info("generated_codeVerifier : {}", codeVerifierBase64);
            log.info("generated_codeChallenge : {}", codeChallengeBase64);

            return code;
        } catch (Exception e) {
            log.error("error in creating codeChallenge : {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public String getGrantOrResponseType(String grantType, boolean isAuthUrl) {
        if (!StringUtils.hasText(grantType)) {
            return null;
        }
        return switch (grantType.toLowerCase()) {
            case "authorization_code", "authorization_code_with_pkce" -> isAuthUrl ? "code" : "authorization_code";
            case "implicit" -> "token";
            default -> null;
        };
    }

}
