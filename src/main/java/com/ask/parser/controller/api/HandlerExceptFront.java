package com.ask.parser.controller.api;

import com.ask.exception.Except4Support;
import com.ask.exception.Except4SupportDocumented;
import com.ask.exception.ExceptInfoUser;
import com.ask.config.monitor.MonitorErrorsService;
import com.ask.config.monitor.ServiceSecurityRequest;
import com.ask.parser.except.Msg;
import com.ask.parser.controller.*;
import com.ask.parser.controller.api.response.ErrorResponse;
import com.ask.parser.service.ActionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestControllerAdvice
public class HandlerExceptFront {

    @Autowired
    private MonitorErrorsService monitorErrorsService;
    @Autowired
    private ServiceSecurityRequest serviceSecurityRequest;
    @Autowired
    private ActionService actionService;

    private final static Logger logger = Logger.getLogger(HandlerExceptFront.class.getName());

    @ExceptionHandler({Except4Support.class})
    public ResponseEntity<ErrorResponse> handleSupportException(HttpServletRequest req, Except4Support ex) {
        Long actionId = Contr.getActionIdFromReq(req);
        RemoteClientDto kRemote = new RemoteClientDto(req);

        String message = String.format("[Exception in %s]. IP: %s, actionId: %s, message: %s",
                req.getRequestURI(),
                kRemote.getClientIp(),
                (actionId != null) ? actionId.toString() : "N/A",
                ex.getMessage4Support());
        logger.severe(message);

        ErrorResponse errorResponse = new ErrorResponse(ex);
        actionService.closeActionWithError(actionId);
        monitorErrorsService.addError(ex.getMessage4Monitor());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(Except4SupportDocumented.class)
    public ResponseEntity<ErrorResponse> handleSupportDocumentedException(HttpServletRequest req, Except4SupportDocumented ex) {
        Long actionId = Contr.getActionIdFromReq(req);
        RemoteClientDto kRemote = new RemoteClientDto(req);

        String message = String.format("[Exception in %s]. IP: %s, actionId: %s, message: %s",
                req.getRequestURI(),
                kRemote.getClientIp(),
                (actionId != null) ? actionId.toString() : "N/A",
                ex.getMessage4Support());
        logger.severe(message);

        ErrorResponse errorResponse = new ErrorResponse(ex.getMessage());
        actionService.closeActionWithError(actionId);
        monitorErrorsService.addError(ex.getMessage4Monitor());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler({ExceptAccess.class})
    public ResponseEntity<ErrorResponse> handleAccessException(HttpServletRequest req, ExceptAccess ex) {
        Long actionId = Contr.getActionIdFromReq(req);
        RemoteClientDto kRemote = new RemoteClientDto(req);

        String message = String.format("[ExceptAccess in %s]. IP: %s, actionId: %s, message: %s",
                req.getRequestURI(),
                kRemote.getClientIp(),
                (actionId != null) ? actionId.toString() : "N/A",
                ex.getMessage4Support());
        logger.severe(message);

        ErrorResponse errorResponse = new ErrorResponse(new ExceptInfoUser(Msg.i().getMessage(message)));
        actionService.closeActionWithError(actionId);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler({RuntimeException.class, Throwable.class})
    public ResponseEntity<ErrorResponse> handleUnknownException(HttpServletRequest req, Throwable ex) {
        Long actionId = Contr.getActionIdFromReq(req);
        RemoteClientDto kRemote = new RemoteClientDto(req);

        String message = String.format("[RuntimeException in %s]. IP: %s, actionId: %s, message: %s",
                req.getRequestURI(),
                kRemote.getClientIp(),
                (actionId != null) ? actionId.toString() : "N/A",
                ex.getMessage());
        logger.severe(message);

        Except4SupportDocumented kEx = new Except4SupportDocumented("ErrFrontHandler_02", message, ex);
        ResponseEntity<ErrorResponse> response = handleSupportDocumentedException(req, kEx);

        actionService.closeActionWithError(Contr.getActionIdFromReq(req));
        return response;
    }

    @ExceptionHandler({HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<ErrorResponse> handleUnsupportedMethodException(HttpServletRequest req, HttpRequestMethodNotSupportedException ex) {
        ErrorResponse response = new ErrorResponse(new ExceptInfoUser(Msg.i().getMessage(ex.getMessage())));
        actionService.closeActionWithError(Contr.getActionIdFromReq(req));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler({RequestRejectedException.class, InvalidPropertyException.class, MultipartException.class})
    public ResponseEntity<ErrorResponse> handlePotentialErrorException(HttpServletRequest req, RuntimeException ex) {
        RemoteClientDto kRemote = new RemoteClientDto(req);
        Long actionId = Contr.getActionIdFromReq(req);
        String message = String.format("[RuntimeException in %s]. IP: %s, actionId: %s, message: %s",
                req.getRequestURI(),
                kRemote.getClientIp(),
                (actionId != null) ? actionId.toString() : "N/A",
                ex.getMessage());

        actionService.closeActionWithError(actionId);

        boolean isError = serviceSecurityRequest.isNeedError();
        if (isError) {
            logger.log(Level.SEVERE, message);
            monitorErrorsService.addError("Blocking a suspicious request");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new ErrorResponse(new ExceptInfoUser(Msg.i().getMessage(ex.getMessage()))));
        }

        ErrorResponse response = new ErrorResponse(new ExceptInfoUser(Msg.i().getMessage(ex.getMessage())));
        logger.log(Level.WARNING, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ExceptSessionExpired.class)
    public ResponseEntity<?> handleSessionExpiredException(HttpServletRequest req, Authentication authentication, ExceptSessionExpired e) {
        actionService.closeActionWithError(Contr.getActionIdFromReq(req));
        if (authentication != null && authentication.isAuthenticated()) {
            return ResponseEntity.ok().build();
        } else {
            ErrorResponse errorResponse = new ErrorResponse(new ExceptInfoUser(Msg.i().getMessage(e.getMessage())));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(HttpServletRequest req, BindException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        ExceptInfoUser exceptInfoUser = new ExceptInfoUser(errors);
        actionService.closeActionWithError(Contr.getActionIdFromReq(req));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(exceptInfoUser));
    }
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstrViolationException(HttpServletRequest req, ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(error -> errors.put(error.getPropertyPath().toString(), error.getMessage()));
        ExceptInfoUser exceptInfoUser = new ExceptInfoUser(errors);
        actionService.closeActionWithError(Contr.getActionIdFromReq(req));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(exceptInfoUser));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class})
    public ResponseEntity<ErrorResponse> handleValidationException(HttpServletRequest req, MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        ExceptInfoUser exceptInfoUser = new ExceptInfoUser(errors);
        actionService.closeActionWithError(Contr.getActionIdFromReq(req));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(exceptInfoUser));
    }

    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<ErrorResponse> handleNumberFormatException(HttpServletRequest req, NumberFormatException ex) {
        ExceptInfoUser e = new ExceptInfoUser(Msg.i().getMessage(ex.getMessage()));
        actionService.closeActionWithError(Contr.getActionIdFromReq(req));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e));
    }

    @ExceptionHandler(InvalidDataAccessResourceUsageException.class)
    public ResponseEntity<ErrorResponse> handleDatabaseErrorException(HttpServletRequest req, InvalidDataAccessResourceUsageException ex) {
        RemoteClientDto kRemote = new RemoteClientDto(req);
        Long actionId = Contr.getActionIdFromReq(req);
        String message = String.format("[RuntimeException in %s]. IP: %s, actionId: %s, message: %s",
                req.getRequestURI(),
                kRemote.getClientIp(),
                (actionId != null) ? actionId.toString() : "N/A",
                ex.getMessage());

        Except4SupportDocumented kEx = new Except4SupportDocumented("ErrFrontHandler_01", message);
        logger.severe(message);

        ErrorResponse errorResponse = new ErrorResponse(kEx); //todo дописать метод: вызвал getMessage
        actionService.closeActionWithError(actionId);
        monitorErrorsService.addError(kEx.getMessage4Monitor());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalException(HttpServletRequest req,IllegalArgumentException ex) {
        ExceptInfoUser exceptInfoUser = new ExceptInfoUser(Msg.i().getMessage(ex.getMessage()));

        actionService.closeActionWithError(Contr.getActionIdFromReq(req));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(exceptInfoUser));
    }
}
