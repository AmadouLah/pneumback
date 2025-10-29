package com.pneumaliback.www.dto;

import com.pneumaliback.www.entity.Message;

public record MessageDTO(Long authorId, Long recipientId, String content) {
    public Message toEntity() {
        Message m = new Message();
        m.setContent(content);
        return m;
    }
}
