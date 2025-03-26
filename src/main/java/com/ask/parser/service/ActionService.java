package com.ask.parser.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ask.exception.Except4Support;
import com.ask.exception.Except4SupportDocumented;
import com.ask.parser.conf.js.ConfJs;
import com.ask.parser.controller.Contr;
import com.ask.parser.controller.api.ApiRequestDto;
import com.ask.parser.controller.api.ApiResponseDto;
import com.ask.parser.controller.api.ErrorDetailsDto;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

@Service
public class ActionService {
    @Autowired
    private RestTemplate restTemplate;
        
    private static final String ACTION_ID = "action_id";
    private static final String ACTION_NAME = "action_name";
    private static final String SESSION_ID = "session_id";
    private static final String USER_ID = "user_id";
    private static final String IS_ERROR = "is_error";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ask");
    
    public Long createAction(String sessionId, String userId, String actionName) {
        if (sessionId == null) 
            throw new Except4SupportDocumented("ErrActionService001", "Error create action",
                "Session ID is null");        
        if (userId == null)
            throw new Except4SupportDocumented("ErrActionService002", "Error create action",
                    "User ID is null");        
        String formattedDate = LocalDateTime.now().format(formatter);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put(ApiRequestDto.DATE, formattedDate);
        Map<String, String> dataMap = new HashMap<>();
        dataMap.put(ACTION_NAME, actionName);
        dataMap.put(SESSION_ID, sessionId);
        dataMap.put(USER_ID, userId);
        requestBody.put(ApiRequestDto.DATA, dataMap);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(ConfJs.getInstance().getApp().getActionServiceUrlCreate(), HttpMethod.POST, requestEntity, String.class);
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new Except4SupportDocumented("ErrActionService003", "Error creating action",
        String.format("Failed to create action, response status: %s, body: %s",
                response.getStatusCode(), response.getBody()));
            }
            String responseBody = response.getBody();
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            validateOkResponse(responseMap);
            String status = (String) responseMap.get(ApiResponseDto.STATUS);
            if ("ERROR".equals(status)) {
                String errorMessageFields = processErrorFields(responseMap);
                throw new Except4SupportDocumented("ErrActionService004", "Error to create action",
                    "Action creating failed with error: " + errorMessageFields);
            } 
            if ("OK".equals(status)) {
                List<Map<String, Object>> resultList = (List<Map<String, Object>>) responseMap.get(ApiResponseDto.RESULT);
                Long actionId = Long.valueOf((Integer) resultList.get(0).get(ACTION_ID));
                return actionId;
            } 
            throw new Except4SupportDocumented("ErrActionService005", "Not valid response from action service",
                    String.format("Not valid response status from action service: %s", status));
            
        } catch (RestClientException ex) {
            throw new Except4SupportDocumented("ErrActionService006", "Error to create action",
                    String.format("Error to create action by url: '%s'", ConfJs.getInstance().getApp().getActionServiceUrlCreate()), ex);
        } catch (JsonProcessingException ex) {
            throw new Except4SupportDocumented("ErrActionService007", "JSON parsing error",
                    String.format("Error parsing response JSON: %s", ex.getMessage()), ex);
        }
    }

    public void closeAction(Long actionId) {
        if (actionId != null) {
            closeActionProcess(actionId, false);
            return;
        }
        throw new Except4SupportDocumented("ErrActionService008", "Error close action",
"action id is null");
    }

    public void closeActionWithError(Long actionId) {
        if (actionId != null) {
            closeActionProcess(actionId, true);
        }
    }

    private void closeActionProcess(Long actionId, Boolean isError) {
        if (actionId == null)
            throw new Except4SupportDocumented("ErrActionService010", "Error while closing action process",
                    "action id is null");
        String formattedDate = LocalDateTime.now().format(formatter);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put(ApiRequestDto.DATE, formattedDate);
        Map<String, Boolean> dataMap = new HashMap<>();
        dataMap.put(IS_ERROR, isError);
        requestBody.put(ApiRequestDto.DATA, dataMap);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(Contr.REQ_ACTION_ID, String.valueOf(actionId));
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    ConfJs.getInstance().getApp().getActionServiceUrlClose(),
                    HttpMethod.PATCH,
                    requestEntity,
                    String.class);
            if (response.getStatusCode() != HttpStatus.OK) 
                    throw new Except4SupportDocumented("ErrActionService010", "Error closing action",
            String.format("Failed to close action, response status: %s, body: %s",
                    response.getStatusCode(), response.getBody()));            
            String responseBody = response.getBody();
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            validateOkResponse(responseMap);
            String status = (String) responseMap.get(ApiResponseDto.STATUS);
            if ("ERROR".equals(status)) {
                String errorMessageFields = processErrorFields(responseMap);
                throw new Except4SupportDocumented("ErrActionService011", "Error to close action",
                    "Action closing failed with error: " + errorMessageFields);
            } 
            if ("OK".equals(status)) 
                return;            
            throw new Except4SupportDocumented("ErrActionService012", "Not valid response from action service",
                    String.format("Not valid response status from action service: %s", status));
            
        } catch (RestClientException ex) {
            throw new Except4SupportDocumented("ErrActionService013", "Error to close action",
                    String.format("Error to close action by url: '%s'", ConfJs.getInstance().getApp().getActionServiceUrlClose()), ex);
        } catch (JsonProcessingException ex) {
            throw new Except4SupportDocumented("ErrActionService014", "JSON parsing error",
                    String.format("Error parsing response JSON: %s", ex.getMessage()), ex);
        }
    }

    private void validateOkResponse(Map<String, Object> responseMap) throws Except4Support {
        List<String> missingFields = new ArrayList<>();
        if (responseMap.get(ApiResponseDto.STATUS) == null)
            missingFields.add(ApiResponseDto.STATUS);
        if (responseMap.get(ApiRequestDto.DATE) == null)
            missingFields.add(ApiRequestDto.DATE);
        List<Map<String, String>> resultList = (List<Map<String, String>>) responseMap.get(ApiResponseDto.RESULT);
        if (resultList == null || resultList.isEmpty())
            missingFields.add(ApiResponseDto.RESULT);
        
        if (!missingFields.isEmpty()) {
            throw new Except4SupportDocumented("ErrActionService015", "Fields are empty",
                    "The following fields are missing from the request: " + String.join(", ", missingFields));
        }
    }

    private String processErrorFields(Map<String, Object> responseMap) throws Except4Support {
        List<Map<String, String>> resultList = (List<Map<String, String>>) responseMap.get(ApiResponseDto.RESULT);
        Map<String, String> errorResult = resultList.get(0);
        List<String> missingFields = new ArrayList<>();
        if (errorResult.get(ErrorDetailsDto.ERROR_ID) == null)
            missingFields.add(ErrorDetailsDto.ERROR_ID);
        if (errorResult.get(ErrorDetailsDto.ERROR_CODE) == null)
            missingFields.add(ErrorDetailsDto.ERROR_CODE);
        if (errorResult.get(ErrorDetailsDto.ERROR_MESSAGE) == null)
            missingFields.add(ErrorDetailsDto.ERROR_MESSAGE);
        if (!missingFields.isEmpty()) {
            throw new Except4SupportDocumented("ErrActionService016", "Fields are empty",
                    "The following error fields are missing: " + String.join(", ", missingFields));
        }
        String errorMessageFields = String.format("id=%s, code=%s, message=%s",errorResult.get(ErrorDetailsDto.ERROR_ID), errorResult.get(ErrorDetailsDto.ERROR_CODE), errorResult.get(ErrorDetailsDto.ERROR_MESSAGE));
        return errorMessageFields;
    }

}
