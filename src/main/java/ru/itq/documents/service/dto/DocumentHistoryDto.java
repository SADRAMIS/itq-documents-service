package ru.itq.documents.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "История изменения документа")
public class DocumentHistoryDto {
    @Schema(description = "ID записи")
    private Long id;

    @Schema(description = "Кто выполнил действие")
    private String initiator;

    @Schema(description = "Время действия")
    private LocalDateTime actionTime;

    @Schema(description = "Тип действия", example = "SUBMIT")
    private ActionTypeDto action;

    @Schema(description = "Комментарий")
    private String comment;
}
