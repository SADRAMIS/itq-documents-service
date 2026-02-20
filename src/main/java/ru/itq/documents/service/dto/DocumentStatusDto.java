package ru.itq.documents.service.dto;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Статусы документа")
public enum DocumentStatusDto {
    DRAFT("DRAFT"),
    SUBMITTED("SUBMITTED"),
    APPROVED("APPROVED");

    private final String value;

    DocumentStatusDto(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static DocumentStatusDto fromValue(String value) {
        for (DocumentStatusDto status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + value);
    }
}

