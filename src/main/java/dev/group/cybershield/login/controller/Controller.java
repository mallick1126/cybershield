package dev.group.cybershield.login.controller;

import dev.group.cybershield.common.global.ResponseDTO;
import dev.group.cybershield.common.utils.ResponseUtil;
import dev.group.cybershield.login.model.UserDTO;
import dev.group.cybershield.login.service.UserServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/login")
public class Controller {

    @Autowired
    UserServices userServices;

    @PostMapping(path = "/v1.0/verifyUser")
    public ResponseEntity<ResponseDTO> verifyUser(@RequestBody UserDTO reqBody) {
        try {
            String endPoint = "verifyUser";
            Timestamp landingTime = Timestamp.valueOf(LocalDateTime.now());
            Boolean response = userServices.verifyUser(reqBody);
            return ResponseUtil.sendResponse(response, landingTime, HttpStatus.OK, endPoint);
        } catch (Exception e) {
            log.error("unable to verify User by verifyUser_API: {}", e.getMessage());
            throw e;
        }

    }
}
