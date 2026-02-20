package ru.itq.documents.service.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Тип действия")
public enum ActionTypeDto {
    SUBMIT, APPROVE;

    @JsonValue
    public String getValue() {
        return name();
    }

    @JsonCreator
    public static ActionTypeDto fromValue(String value) {
        return valueOf(value.toUpperCase());
    }
}
