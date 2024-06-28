package br.com.cardoso.model;

import java.time.LocalDateTime;

public class ValidationEvent {

    private final boolean isSuccess;
    private final LocalDateTime timestamp;

    public ValidationEvent(boolean isSuccess) {
        this.isSuccess = isSuccess;
        this.timestamp = LocalDateTime.now();
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
