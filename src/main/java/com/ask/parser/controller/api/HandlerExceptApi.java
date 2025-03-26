package com.ask.parser.controller.api;

import com.ask.config.monitor.ServiceSecurityRequest;
import com.ask.exception.Except;
import com.ask.exception.Except4Support;
import com.ask.exception.Except4SupportDocumented;
import com.ask.exception.ExceptInfoUser;
import com.ask.parser.except.Msg;
import com.ask.parser.controller.*;
import com.ask.config.monitor.MonitorErrorsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author user
 */
@RestControllerAdvice(annotations = RestController.class)
public class HandlerExceptApi {

    @Autowired
    private MonitorErrorsService monitorErrorsService;
    @Autowired
    private ServiceSecurityRequest serviceSecurityRequest;

    private final static Logger logger = Logger.getLogger(HandlerExceptApi.class.getName());

    @ExceptionHandler({Except4Support.class, Except4SupportDocumented.class})
    public ResponseEntity<ApiResponseDto> handleExcept(HttpServletRequest req, Except ex) {
        Long actionId = Contr.getActionIdFromReq(req);
        RemoteClientDto kRemote = new RemoteClientDto(req);

        String message = String.format("[Exception in %s]. IP: %s, actionId: %s, message: %s",
                req.getRequestURI(),
                kRemote.getClientIp(),
                (actionId != null) ? actionId.toString() : "N/A",
                ex.getMessage4Support());
        logger.severe(message);

        monitorErrorsService.addError(ex.getMessage4Monitor());

        ApiResponseDto response = new ApiResponseDto(
                ApiResponseDto.STATUS_ERROR,
                new ErrorDetailsDto(ex.getCodeId(), ex.getErrorCode(),Except4Support.ENG_INTERNAL_ERROR )
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler({ExceptAccess.class})
    public ResponseEntity<ApiResponseDto> handleAccess(HttpServletRequest req, ExceptAccess ex) {
        RemoteClientDto kRemote = new RemoteClientDto(req);

        Long actionId = Contr.getActionIdFromReq(req);
        String message = String.format("[ExceptAccess in %s]. IP: %s, actionId: %s, message: %s",
                req.getRequestURI(),
                kRemote.getClientIp(),
                (actionId != null) ? actionId.toString() : "N/A",
                ex.getMessage4Support());
        logger.warning(message);

        ApiResponseDto response = new ApiResponseDto(
                ApiResponseDto.STATUS_ERROR,
                new ErrorDetailsDto(ex.getCodeId(), ex.getErrorCode(), Except4Support.ENG_INTERNAL_ERROR)
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler({RuntimeException.class, Throwable.class})
    public ResponseEntity<ApiResponseDto> handleUnknown(HttpServletRequest req, Throwable ex) {
        RemoteClientDto kRemote = new RemoteClientDto(req);

        Long actionId = Contr.getActionIdFromReq(req);
        String message = String.format("[Uncatched exception in %s]. IP: %s, actionId: %s, message: %s",
                req.getRequestURI(),
                kRemote.getClientIp(),
                (actionId != null) ? actionId.toString() : "N/A",
                ex.getMessage());

        Except4SupportDocumented except4Support = new Except4SupportDocumented("ErrApiHandler_08", message, ex);
        logger.severe(except4Support.getMessage4Support());
        monitorErrorsService.addError(except4Support.getMessage4Monitor());

        ApiResponseDto response = new ApiResponseDto(
                ApiResponseDto.STATUS_ERROR,
                new ErrorDetailsDto(except4Support.getCodeId(), except4Support.getErrorCode(), Except4Support.ENG_INTERNAL_ERROR
        ));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler({HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<ApiResponseDto> handleUnsupportedMethod(HttpServletRequest req, HttpRequestMethodNotSupportedException ex) {
        RemoteClientDto kRemote = new RemoteClientDto(req);

        Long actionId = Contr.getActionIdFromReq(req);
        String message = String.format("[Exception in %s]. IP: %s, actionId: %s, message: %s",
                req.getRequestURI(),
                kRemote.getClientIp(),
                (actionId != null) ? actionId.toString() : "N/A",
                ex.getMessage());
        logger.fine(message);
        ExceptInfoUser exceptInfoUser = new ExceptInfoUser(Msg.i().getMessageHttpMethodNotSupported(req.getMethod()));


        ApiResponseDto response = new ApiResponseDto(
                ApiResponseDto.STATUS_ERROR,
                new ErrorDetailsDto(exceptInfoUser.getMessage())
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @ExceptionHandler({RequestRejectedException.class, InvalidPropertyException.class, MultipartException.class})
    public ResponseEntity<ApiResponseDto> handlePotentialError(HttpServletRequest req, RuntimeException ex) {
        RemoteClientDto kRemote = new RemoteClientDto(req);

        Long actionId = Contr.getActionIdFromReq(req);
        String message = String.format("[Potential hacker attack in %s]. IP: %s, actionId: %s, message: %s",
                req.getRequestURI(),
                kRemote.getClientIp(),
                (actionId != null) ? actionId.toString() : "N/A",
                ex.getMessage());

        boolean isError = serviceSecurityRequest.isNeedError();
        Except4SupportDocumented except4Support = new Except4SupportDocumented("ErrApiHandler_06", message);
        if (isError) {
            logger.log(Level.SEVERE, except4Support.getMessage4Support());
            monitorErrorsService.addError(except4Support.getMessage4Monitor());
            ApiResponseDto response = new ApiResponseDto(
                    ApiResponseDto.STATUS_ERROR,
                    new ErrorDetailsDto(except4Support.getCodeId(), except4Support.getErrorCode(), Except4Support.ENG_INTERNAL_ERROR)
            );
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
        }

        logger.log(Level.WARNING, except4Support.getMessage4Support());
        ApiResponseDto response = new ApiResponseDto(
                ApiResponseDto.STATUS_ERROR,
                new ErrorDetailsDto(except4Support.getCodeId(), except4Support.getErrorCode(), Except4Support.ENG_INTERNAL_ERROR)
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ExceptSessionExpired.class) //todo не дергать ТП по этому поводу. Перебросить пользователя на стр. авторизации
    public ResponseEntity<?> handleSessionExpired(HttpServletRequest req, Authentication authentication, ExceptSessionExpired e) {
        if (authentication != null && authentication.isAuthenticated()) {
            return ResponseEntity.ok().build();
        } else {
            RemoteClientDto kRemote = new RemoteClientDto(req);

            Long actionId = Contr.getActionIdFromReq(req);
            String message = String.format("[Exception in %s]. IP: %s, actionId: %s, message: %s",
                    req.getRequestURI(),
                    kRemote.getClientIp(),
                    (actionId != null) ? actionId.toString() : "N/A",
                    e.getMessage());

            Except4SupportDocumented except4Support = new Except4SupportDocumented("ErrApiHandler_05", message);
            logger.log(Level.WARNING, except4Support.getMessage4Support());
            monitorErrorsService.addError(except4Support.getMessage4Monitor());

            ApiResponseDto response = new ApiResponseDto(
                    ApiResponseDto.STATUS_ERROR,
                    new ErrorDetailsDto(except4Support.getCodeId(), except4Support.getErrorCode(), Except4Support.ENG_INTERNAL_ERROR)
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponseDto> handleBindException(HttpServletRequest req, BindException ex) {
        RemoteClientDto kRemote = new RemoteClientDto(req);

        Long actionId = Contr.getActionIdFromReq(req);
        String message = String.format("[Exception in %s]. IP: %s, actionId: %s, message: %s",
                req.getRequestURI(),
                kRemote.getClientIp(),
                (actionId != null) ? actionId.toString() : "N/A",
                ex.getMessage());

        BindingResult bindingResult = ex.getBindingResult();
        String errorMessages = bindingResult.getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));

        Except4SupportDocumented except4Support = new Except4SupportDocumented("ErrApiHandler_04", message);
        logger.severe(except4Support.getMessage4Support());
        monitorErrorsService.addError(except4Support.getMessage4Monitor());

        ApiResponseDto response = new ApiResponseDto(
                ApiResponseDto.STATUS_ERROR,
                new ErrorDetailsDto(except4Support.getCodeId(), except4Support.getErrorCode(), "BindException: " + errorMessages)
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponseDto> handleConstrViolationException(HttpServletRequest req, ConstraintViolationException ex) {
        RemoteClientDto kRemote = new RemoteClientDto(req);

        Long actionId = Contr.getActionIdFromReq(req);
        String msg4Support = String.format("[Exception in %s]. IP: %s, actionId: %s, msg4Support: %s",
                req.getRequestURI(),
                kRemote.getClientIp(),
                (actionId != null) ? actionId.toString() : "N/A",
                ex.getMessage());
        StringBuilder sb = new StringBuilder();
        Iterator<ConstraintViolation<?>> iterator =  ex.getConstraintViolations().iterator();
        while(iterator.hasNext()) {
            ConstraintViolation<?> next = iterator.next();
            sb.append(next.getPropertyPath())
                    .append(" = ")
                    .append(next.getMessage());
            if (iterator.hasNext()) {
                sb.append(", ");
            }
        }

        Except4Support except4Support = new Except4Support("ErrApiHandler_010", sb.toString(), msg4Support); // todo не ошибка для саппорта
        logger.severe(except4Support.getMessage4Support());
        monitorErrorsService.addError(except4Support.getMessage4Monitor());

        ApiResponseDto response = new ApiResponseDto(
                ApiResponseDto.STATUS_ERROR,
                new ErrorDetailsDto(except4Support.getCodeId(), except4Support.getCodeId(), except4Support.getMessage4User())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class})
    public ResponseEntity<ApiResponseDto> handleValidationException(HttpServletRequest req, MethodArgumentNotValidException ex) {
        RemoteClientDto kRemote = new RemoteClientDto(req);

        Long actionId = Contr.getActionIdFromReq(req);
        String msg4Support = String.format("[Exception in %s]. IP: %s, actionId: %s, msg4Support: %s",
                req.getRequestURI(),
                kRemote.getClientIp(),
                (actionId != null) ? actionId.toString() : "N/A",
                ex.getMessage());
        var fieldErrors = ex.getBindingResult().getFieldErrors();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fieldErrors.size(); i++) {
            sb.append(fieldErrors.get(i).getField())
                    .append(" = ")
                    .append(fieldErrors.get(i).getDefaultMessage());
            if (i < fieldErrors.size() - 1) {
                sb.append(", ");
            }
        }
        Except4SupportDocumented except4Support = new Except4SupportDocumented("ErrApiHandler_03", sb.toString(), msg4Support);
        logger.severe(except4Support.getMessage4Support());
        monitorErrorsService.addError(except4Support.getMessage4Monitor());

        ApiResponseDto response = new ApiResponseDto(
                ApiResponseDto.STATUS_ERROR,
                new ErrorDetailsDto(except4Support.getCodeId(), except4Support.getCodeId(), except4Support.getMessage4User())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<ApiResponseDto> handleNumberFormatError(HttpServletRequest req, NumberFormatException ex) {
        RemoteClientDto kRemote = new RemoteClientDto(req);

        Long actionId = Contr.getActionIdFromReq(req);
        String message = String.format("[Number format Exception in %s]. IP: %s, actionId: %s, message: %s",
                req.getRequestURI(),
                kRemote.getClientIp(),
                (actionId != null) ? actionId.toString() : "N/A",
                ex.getMessage());


        Except4SupportDocumented except4Support = new Except4SupportDocumented("ErrApiHandler_02", message);
        logger.severe(except4Support.getMessage4Support());
        monitorErrorsService.addError(except4Support.getMessage4Monitor());

        ApiResponseDto response = new ApiResponseDto(
                ApiResponseDto.STATUS_ERROR,
                new ErrorDetailsDto(except4Support.getCodeId(), except4Support.getErrorCode(), Except4Support.ENG_INTERNAL_ERROR)
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(InvalidDataAccessResourceUsageException.class)
    public ResponseEntity<ApiResponseDto> handleDbError(HttpServletRequest req, InvalidDataAccessResourceUsageException ex) {
        RemoteClientDto kRemote = new RemoteClientDto(req);

        Long actionId = Contr.getActionIdFromReq(req);
        String message = String.format("[Exception in %s]. IP: %s, actionId: %s, message: %s",
                req.getRequestURI(),
                kRemote.getClientIp(),
                (actionId != null) ? actionId.toString() : "N/A",
                ex.getMessage());


        Except4SupportDocumented except4Support = new Except4SupportDocumented("ErrApiHandler_01",message);
        logger.severe(except4Support.getMessage4Support());
        monitorErrorsService.addError(except4Support.getMessage4Monitor());

        ApiResponseDto response = new ApiResponseDto(
                ApiResponseDto.STATUS_ERROR,
                new ErrorDetailsDto(except4Support.getCodeId(), except4Support.getErrorCode(), Except4Support.ENG_INTERNAL_ERROR)
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponseDto> handleIllegalException(HttpServletRequest req, IllegalArgumentException ex) {
        RemoteClientDto kRemote = new RemoteClientDto(req);
        Long actionId = Contr.getActionIdFromReq(req);
        String message = String.format("[IllegalArgumentException in %s]. IP: %s, actionId: %s, message: %s",
                req.getRequestURI(),
                kRemote.getClientIp(),
                (actionId != null) ? actionId.toString() : "N/A",
                ex.getMessage());

        Except4Support except4Support = new Except4Support("ErrApiHandler_10",message);
        ApiResponseDto response = new ApiResponseDto(
                ApiResponseDto.STATUS_ERROR,
                new ErrorDetailsDto(except4Support.getCodeId(), except4Support.getErrorCode(), Except4Support.ENG_INTERNAL_ERROR)
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
