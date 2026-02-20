package ru.itq.documents.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@Builder
@Schema(description = "Документ")
public class DocumentDto {
    @Schema(description = "Внутренний ID")
    private Long id;

    @Schema(description = "Уникальный номер документа", example = "DOC-00000001")
    private String number;

    @Schema(description = "Автор документа")
    private String author;

    @Schema(description = "Название документа")
    private String title;

    @Schema(description = "Статус документа", example = "DRAFT")
    private DocumentStatusDto status;

    @Schema(description = "Дата создания")
    private LocalDateTime createdAt;

    @Schema(description = "Дата последнего обновления")
    private LocalDateTime updatedAt;
}
