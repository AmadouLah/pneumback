package com.pneumaliback.www.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationRechercheDTO {
    private Long userId;
    private String query;
    private String type;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isRead;
}
