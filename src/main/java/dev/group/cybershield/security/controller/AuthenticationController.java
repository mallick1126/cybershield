package dev.group.cybershield.security.controller;

import dev.group.cybershield.common.exception.BadRequestException;
import dev.group.cybershield.common.global.ResponseDTO;
import dev.group.cybershield.common.utils.CommonUtils;
import dev.group.cybershield.common.utils.ResponseUtil;
import dev.group.cybershield.security.model.LoginReq;
import dev.group.cybershield.security.model.LoginRes;
import dev.group.cybershield.security.model.SignUpReq;
import dev.group.cybershield.security.model.SignUpRes;
import dev.group.cybershield.security.oauth.OauthModels.OauthAuthorizeModel;
import dev.group.cybershield.security.oauth.OauthService;
import dev.group.cybershield.security.service.AuthenticationService;
import dev.group.cybershield.security.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@RestController
public class AuthenticationController {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private OauthService oauthService;

    @PostMapping("/public/token")
    public ResponseEntity<ResponseDTO> token(@RequestBody LoginReq loginReq,
                                          HttpServletRequest request,
                                          HttpServletResponse response) throws Exception{
        String endPoint = "/public/token";
        Timestamp landingTime = Timestamp.valueOf(LocalDateTime.now());
        try {
            LoginRes loginRes = LoginRes.builder().build();
            if (Objects.nonNull(loginReq) && loginReq.isUsingUsernameAndPassword()) {
                loginRes = authenticationService.getToken(loginReq.getUsername(), loginReq.getPassword());
            } else if (Objects.nonNull(loginReq) && loginReq.isUsingRefreshToken()) {
                loginRes = authenticationService.getToken(loginReq.getRefreshToken());
            } else if (Objects.nonNull(loginReq) && loginReq.isUsingOauth()) {
                Thread.sleep(5000);
                loginRes = authenticationService.getToken(CommonUtils.urlDecode(loginReq.getAuthCode()),
                        CommonUtils.urlDecode(loginReq.getReturnedState()),loginReq.getAuthCachedKey());
            }else{
                throw new BadRequestException("No credentials provided");
            }
            authenticationService.setTokensInResponseCookies(loginRes, request, response);
            return ResponseUtil.sendResponse(loginRes, landingTime, HttpStatus.OK, endPoint);
        } catch (Exception e) {
            System.out.println("Exception occurred while logging in user");
            e.printStackTrace();
            throw e;
        }
    }

    @GetMapping("/public/authorize/{provider}")
    public ResponseEntity<ResponseDTO> authorizeUrl(@PathVariable("provider") String provider) {
        try {
            String endPoint = "/authorize";
            Timestamp landingTime = Timestamp.valueOf(LocalDateTime.now());
            OauthAuthorizeModel oauthAuthorizeModel = oauthService.getAuthorizeModel(provider); // now generate the auth url for the provider
            return ResponseUtil.sendResponse(oauthAuthorizeModel,landingTime,HttpStatus.OK,endPoint);
        } catch (Exception e) {
            log.error("Error while generating authorizeUrl {}", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @PostMapping("/public/register")
    public ResponseEntity<ResponseDTO> register(@RequestBody @Valid SignUpReq signUpReq) throws Exception {
        String endPoint = "/register";
        Timestamp landingTime = Timestamp.valueOf(LocalDateTime.now());
        try{
            SignUpRes signUpRes = authenticationService.register(signUpReq);
            return ResponseUtil.sendResponse(signUpRes,landingTime,HttpStatus.OK,endPoint);
        }catch(Exception e){
            log.error("Error while registering the user");
            e.printStackTrace();
            throw e;
        }
    }


}
