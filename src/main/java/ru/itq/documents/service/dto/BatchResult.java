package ru.itq.documents.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Результат пакетной операции")
public class BatchResult {
    @Schema(description = "ID документа")
    private Long id;

    @Schema(description = "Успешно ли")
    private boolean success;

    @Schema(description = "Код ошибки", example = "NOT_FOUND")
    private String errorCode;

    @Schema(description = "Сообщение об ошибке")
    private String message;
}
