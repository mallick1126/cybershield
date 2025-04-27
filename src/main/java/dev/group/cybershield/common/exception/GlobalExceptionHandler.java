package dev.group.cybershield.common.exception;

import dev.group.cybershield.common.constants.CommonConstants;
import dev.group.cybershield.common.global.ResponseDTO;
import dev.group.cybershield.common.utils.ResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.naming.AuthenticationException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private void logExceptions(Exception e, HttpServletRequest request) {
        log.error(CommonConstants.EXCEPTION_AT_STR + "{}", request.getRequestURI());
        log.error(CommonConstants.EXCEPTION_MSG_STR + "{}", e.getMessage());
        log.error(CommonConstants.EXCEPTION_TRACE_STR + "{}", Arrays.toString(e.getStackTrace()));
        e.printStackTrace();
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ResponseDTO> handleBadRequestException(BadRequestException ex, HttpServletRequest request) {
        logExceptions(ex, request);
        return ResponseUtil.sendErrorResponse("BAD_REQUEST", ex.getMessage(), HttpStatus.BAD_REQUEST, request.getRequestURI());
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ResponseDTO> handleUsernameNotFoundException(UsernameNotFoundException ex, HttpServletRequest request) {
        logExceptions(ex, request);
        return ResponseUtil.sendErrorResponse("BAD_REQUEST", ex.getMessage(), HttpStatus.BAD_REQUEST, request.getRequestURI());
    }

    @ExceptionHandler({NotFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ResponseDTO> handleNotFoundExceptions(Exception ex, HttpServletRequest request) {
        String errorMessage = (ex instanceof NoHandlerFoundException)
                ? "The requested URL was not found: " + ((NoHandlerFoundException) ex).getRequestURL()
                : ex.getMessage();
        logExceptions(ex, request);
        return ResponseUtil.sendErrorResponse("NOT_FOUND", errorMessage, HttpStatus.NOT_FOUND, request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseDTO> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage(),
                        (existing, replacement) -> existing + ", " + replacement
                ));
        logExceptions(ex, request);
        return ResponseUtil.sendErrorResponse("VALIDATION_ERROR", errorMessage, HttpStatus.BAD_REQUEST, request.getRequestURI());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<String> handleAuthenticationException(AuthenticationException e, HttpServletRequest request) {
        System.out.println("AuthenticationException : " + e.getMessage());
        logExceptions(e,request);
        return new ResponseEntity<String>("AuthenticationException request : " + e.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<String> handleAccessDeniedException(AuthorizationDeniedException e, HttpServletRequest request) {
        System.out.println("AuthorizationDeniedException : " + e.getMessage());
        logExceptions(e,request);
        return new ResponseEntity<String>("AuthorizationDeniedException request : " + e.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e, HttpServletRequest request) {
        Throwable cause = e.getCause();
        if (e instanceof BadCredentialsException) {
            System.out.println("Exception Bad request2 : " + e.getMessage());
            logExceptions(e,request);
            return new ResponseEntity<String>("Exception Bad request2 : " + e.getMessage(), HttpStatus.BAD_REQUEST);
        } else if (e instanceof AuthenticationException) {
            System.out.println("AuthenticationException2 : " + e.getMessage());
            logExceptions(e,request);
            return new ResponseEntity<String>("AuthenticationException2 request : " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        } else if (e instanceof AuthorizationDeniedException) {
            System.out.println("AccessDeniedException2 : " + e.getMessage());
            logExceptions(e,request);
            return new ResponseEntity<String>("AccessDeniedException2 request : " + e.getMessage(), HttpStatus.FORBIDDEN);
        } else {
            System.out.println("Uncaught Exception : " + e.getMessage());
            logExceptions(e,request);
            return new ResponseEntity<String>("Uncaught Exception: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
