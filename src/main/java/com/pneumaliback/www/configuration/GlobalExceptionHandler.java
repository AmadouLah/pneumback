package com.pneumaliback.www.configuration;

import com.pneumaliback.www.add.ErrorResponse;
import com.pneumaliback.www.security.exceptions.TokenExpiredException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;

import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.hibernate.StaleStateException;
import jakarta.persistence.OptimisticLockException;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponse> handleNullPointerException(NullPointerException e) {
        log.error("NullPointerException occurred: ", e);
        ErrorResponse error = new ErrorResponse("Erreur interne: valeur null inattendue", false);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("IllegalArgumentException occurred: ", e);
        ErrorResponse error = new ErrorResponse(e.getMessage(), false);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> erreurs = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(err -> {
                    if (err instanceof FieldError fe) {
                        return fe.getField() + ": " + fe.getDefaultMessage();
                    }
                    return err.getDefaultMessage();
                })
                .collect(Collectors.toList());
        String message = String.join("; ", erreurs);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException ex) {
        String msg = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(msg));
    }

    @ExceptionHandler({ OptimisticLockException.class, StaleStateException.class })
    public ResponseEntity<ErrorResponse> handleOptimisticLockException(Exception e) {
        log.warn("Conflit de concurrence détecté: {}", e.getMessage());
        ErrorResponse error = new ErrorResponse(
                "Opération en cours ailleurs. Veuillez réessayer dans quelques secondes.", false);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        log.error("RuntimeException occurred: ", e);
        ErrorResponse error = new ErrorResponse("Erreur interne du serveur", false);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.error("File size exceeded: ", e);
        ErrorResponse error = new ErrorResponse("Taille de fichier trop importante", false);
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected exception occurred: ", e);
        ErrorResponse error = new ErrorResponse("Erreur inattendue", false);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // Gestionnaires d'exceptions de sécurité
    @ExceptionHandler(com.pneumaliback.www.security.exceptions.AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleCustomAuthenticationException(
            com.pneumaliback.www.security.exceptions.AuthenticationException e) {
        log.error("Authentication exception: ", e);
        ErrorResponse error = new ErrorResponse(e.getMessage(), false);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleTokenExpiredException(TokenExpiredException e) {
        log.error("Token expired exception: ", e);
        ErrorResponse error = new ErrorResponse("Token expiré", false);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException e) {
        log.error("Bad credentials exception: ", e);
        ErrorResponse error = new ErrorResponse("Email ou mot de passe incorrect", false);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLockedException(LockedException e) {
        log.error("Account locked exception: ", e);
        ErrorResponse error = new ErrorResponse("Compte verrouillé. Réessayez plus tard.", false);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.error("Access denied exception: ", e);
        ErrorResponse error = new ErrorResponse("Accès refusé. Vous n'avez pas les permissions nécessaires.", false);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleSpringAuthenticationException(
            org.springframework.security.core.AuthenticationException e) {
        log.error("Spring authentication exception: ", e);
        ErrorResponse error = new ErrorResponse("Erreur d'authentification", false);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
}
