package ru.itq.documents.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Документ с историей изменений")
public class DocumentWithHistoryDto {
    private DocumentDto document;
    private List<DocumentHistoryDto> history;
}
