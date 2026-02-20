package ru.itq.documents.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import ru.itq.documents.service.entity.Document;

@Data
@Builder
@Schema(description = "Результат теста конкурентного утверждения")
public class ConcurrentApproveResult {
    @Schema(description = "ID документа")
    private Long documentId;
    
    @Schema(description = "Количество успешных попыток")
    private int successCount;
    
    @Schema(description = "Количество конфликтов")
    private int conflictCount;
    
    @Schema(description = "Количество ошибок")
    private int errorCount;
    
    @Schema(description = "Финальный статус документа")
    private Document.DocumentStatus finalStatus;
    
    @Schema(description = "Количество записей в реестре")
    private int registryEntriesCount;
}

