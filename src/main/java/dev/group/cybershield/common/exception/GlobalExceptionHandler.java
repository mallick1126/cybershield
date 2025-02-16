package dev.group.cybershield.common.exception;

import dev.group.cybershield.common.constants.CommonConstants;
import dev.group.cybershield.common.global.ResponseDTO;
import dev.group.cybershield.common.utils.ResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

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
                        fieldError -> fieldError.getDefaultMessage()
                ));
        logExceptions(ex, request);
        return ResponseUtil.sendErrorResponse("VALIDATION_ERROR", errorMessage, HttpStatus.BAD_REQUEST, request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDTO> handleGeneralException(Exception ex, HttpServletRequest request) {
        logExceptions(ex, request);
        return ResponseUtil.sendErrorResponse("INTERNAL_SERVER_ERROR", ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, request.getRequestURI());
    }

}
