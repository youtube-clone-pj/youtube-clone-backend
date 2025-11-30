package com.youtube.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtube.common.exception.BaseException;
import com.youtube.notification.event.NotificationCreatedEvent;
import com.youtube.notification.exception.NotificationErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebPushPayloadConverter {

    private final ObjectMapper objectMapper;
    
    public String toPayload(final NotificationCreatedEvent event) {
        try {
            final Map<String, Object> payload = Map.of(
                    "notification", Map.of(
                            "title", event.title(),
                            "icon", event.thumbnailUrl() != null ? event.thumbnailUrl() : "",
                            "data", Map.of(
                                    "url", event.deeplinkUrl(),
                                    "notificationId", event.notificationId()
                            )
                    )
            );
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("WebPush 페이로드 생성 실패 - notificationId: {}", event.notificationId(), e);
            throw new BaseException(NotificationErrorCode.WEB_PUSH_PAYLOAD_CREATION_FAILED, e);
        }
    }
}