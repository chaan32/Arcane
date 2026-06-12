package com.arcane.Arcane.common.Exception.Handler;

import com.arcane.Arcane.common.Logging.ApiLogSupport;
import com.arcane.Arcane.common.Exception.Fail.*;
import com.arcane.Arcane.common.Exception.Normal.CannotFoundChampion;
import com.arcane.Arcane.common.Exception.Normal.CannotFoundComment;
import com.arcane.Arcane.common.Exception.Normal.CannotFoundPatchNote;
import com.arcane.Arcane.common.Exception.RiotAPI.CannotFoundSummoner;
import com.arcane.Arcane.common.Exception.Normal.CannotFoundUser;
import com.arcane.Arcane.common.Exception.RiotAPI.IsPresentLoginId;
import com.arcane.Arcane.common.Exception.RiotAPI.RiotInternalError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IsPresentLoginId.class)
    public ResponseEntity<?> handleIsPresentLoginId(IsPresentLoginId e) {
        logHandledException(HttpStatus.CONFLICT, e);
        Map<String, Object> error = new HashMap<>();
        error.put("enroll", "fail");
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(CannotFoundSummoner.class)
    public ResponseEntity<?> handleCannotFindSummoner(CannotFoundSummoner e) {
        logHandledException(HttpStatus.NOT_FOUND, e);
        Map<String, Object> error = new HashMap<>();
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(CannotFoundUser.class)
    public ResponseEntity<?> handleCannotFindUser(CannotFoundUser e) {
        logHandledException(HttpStatus.NOT_FOUND, e);
        Map<String, Object> error = new HashMap<>();
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(CannotFoundPatchNote.class)
    public ResponseEntity<?> handleCannotFindPatchNote(CannotFoundPatchNote e) {
        logHandledException(HttpStatus.NOT_FOUND, e);
        Map<String, Object> error = new HashMap<>();
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(CannotFoundComment.class)
    public ResponseEntity<?> handleCannotFindComment(CannotFoundComment e) {
        logHandledException(HttpStatus.NOT_FOUND, e);
        Map<String, Object> error = new HashMap<>();
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(EditFail.class)
    public ResponseEntity<?> editFailComment(EditFail e) {
        logHandledException(HttpStatus.FORBIDDEN, e);
        Map<String, Object> error = new HashMap<>();
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(DeleteFail.class)
    public ResponseEntity<?> deleteFailComment(DeleteFail e) {
        logHandledException(HttpStatus.FORBIDDEN, e);
        Map<String, Object> error = new HashMap<>();
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(SignUpFail.class)
    public ResponseEntity<?> signUpFail(SignUpFail e) {
        logHandledException(HttpStatus.NOT_ACCEPTABLE, e);
        Map<String, Object> error = new HashMap<>();
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(error);
    }

    @ExceptionHandler(CannotSignUp.class)
    public ResponseEntity<?> cannotSignUp(CannotSignUp e) {
        logHandledException(HttpStatus.NOT_ACCEPTABLE, e);
        Map<String, Object> error = new HashMap<>();
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(error);
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<?> sqlException(SQLException e) {
        logHandledException(HttpStatus.INTERNAL_SERVER_ERROR, e);
        Map<String, Object> error = new HashMap<>();
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> illegalArgument(IllegalArgumentException e) {
        logHandledException(HttpStatus.BAD_REQUEST, e);
        Map<String, Object> error = new HashMap<>();
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> illegalState(IllegalStateException e) {
        logHandledException(HttpStatus.FORBIDDEN, e);
        Map<String, Object> error = new HashMap<>();
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(CannotFoundChampion.class)
    public ResponseEntity<?> cannotFoundChampion(CannotFoundChampion e) {
        logHandledException(HttpStatus.NOT_FOUND, e);
        Map<String, Object> error = new HashMap<>();
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(PositionError.class)
    public ResponseEntity<?> positionError(PositionError e) {
        logHandledException(HttpStatus.NOT_FOUND, e);
        Map<String, Object> error = new HashMap<>();
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(RiotInternalError.class)
    public ResponseEntity<?> riotInternalError(RiotInternalError e) {
        logHandledException(HttpStatus.INTERNAL_SERVER_ERROR, e);
        Map<String, Object> error = new HashMap<>();
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(TooManyRequestFail.class)
    public ResponseEntity apiRequestLimit(TooManyRequestFail e){
        logHandledException(HttpStatus.TOO_MANY_REQUESTS, e);
        Map<String, Object> error = new HashMap<>();
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
    }

    private void logHandledException(HttpStatus status, Exception exception) {
        HttpServletRequest request = ApiLogSupport.currentRequest();

        if (status.is5xxServerError()) {
            log.error(
                    ApiLogSupport.EXCEPTION_HANDLED,
                    status.value(),
                    ApiLogSupport.method(request),
                    ApiLogSupport.uri(request),
                    ApiLogSupport.exceptionName(exception),
                    ApiLogSupport.exceptionMessage(exception),
                    exception
            );
            return;
        }

        log.warn(
                ApiLogSupport.EXCEPTION_HANDLED,
                status.value(),
                ApiLogSupport.method(request),
                ApiLogSupport.uri(request),
                ApiLogSupport.exceptionName(exception),
                ApiLogSupport.exceptionMessage(exception)
        );
    }

}
