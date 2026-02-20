package ru.itq.documents.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Пакетный запрос")
public class BatchRequest {
    @NotEmpty(message = "Список ID не может быть пустым")
    @Size(min = 1, max = 1000, message = "От 1 до 1000 документов")
    @Schema(description = "Список ID документов", example = "[1,2,3]")
    private List<Long> ids;

    @NotBlank(message = "Инициатор обязателен")
    @Size(max = 100)
    @Schema(description = "Кто выполнил операцию", example = "user123")
    private String initiator;

    private String comment;
}
