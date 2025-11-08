package com.youtube.live.interaction.livestreaming.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * WebSocket 에러 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private String message;
    private LocalDateTime timestamp;

    public ErrorResponse(final String message) {
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
}
