package org.agra.agra_backend.payload;

import jakarta.validation.constraints.NotBlank;

public class SendTicketMessageRequest {

    @NotBlank
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

