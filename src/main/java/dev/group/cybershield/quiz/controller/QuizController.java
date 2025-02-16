package dev.group.cybershield.quiz.controller;

import dev.group.cybershield.common.global.ResponseDTO;
import dev.group.cybershield.common.utils.ResponseUtil;
import dev.group.cybershield.quiz.model.QuizDTO;
import dev.group.cybershield.quiz.service.QuizService;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PostMapping("/v1.0/getQuiz")
    public ResponseEntity<ResponseDTO> getQuiz(@RequestBody @Validated({QuizDTO.GetQuizGroup.class}) QuizDTO reqBody) throws Exception {
        try {
            String endPoint = "getTest";
            Timestamp landingTime = Timestamp.valueOf(LocalDateTime.now());
            QuizDTO response = quizService.getQuiz(reqBody);
            log.info("getTest_API {}", response);
            return ResponseUtil.sendResponse(response, landingTime, HttpStatus.OK, endPoint);
        } catch (Exception e) {
            log.error("unable_to_load_test_getTest_API: {}", e.getMessage());
            throw e;
        }
    }

    @PostMapping("/v1.0/submitQuiz")
    public ResponseEntity<ResponseDTO> submitQuiz(@RequestBody @Validated({QuizDTO.SubmitQuizGroup.class}) QuizDTO reqBody) throws Exception {
        try {
            String endPoint = "submitTest";
            Timestamp landingTime = Timestamp.valueOf(LocalDateTime.now());
            QuizDTO response = quizService.submitQuiz(reqBody);
            log.info("submitTest_API {} ", response);
            return ResponseUtil.sendResponse(response, landingTime, HttpStatus.OK, endPoint);
        } catch (Exception e) {
            log.error("submitTest_API_error: {}", e.getMessage());
            throw e;
        }
    }

    @PostMapping("/v1.0/viewQuiz")
    public ResponseEntity<ResponseDTO> viewQuiz(@RequestBody @Validated({QuizDTO.ViewQuizGroup.class}) QuizDTO reqBody) throws Exception {
        try {
            String endPoint = "viewTest";
            Timestamp landingTime = Timestamp.valueOf(LocalDateTime.now());
            QuizDTO response = quizService.viewQuiz(reqBody);
            log.info("viewTest_API {}", response);
            return ResponseUtil.sendResponse(response, landingTime, HttpStatus.OK, endPoint);
        } catch (Exception e) {
            log.error("unable to load test questions by viewTest_API: {}", e.getMessage());
            throw e;
        }
    }
}
