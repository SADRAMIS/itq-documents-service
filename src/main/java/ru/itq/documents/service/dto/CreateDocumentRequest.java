package ru.itq.documents.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Запрос на создание документа. Клиент отправляет JSON с title и content.
 * Мы проверяем, что они не пустые и title не слишком длинный.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDocumentRequest {

    @NotBlank(message = "Author is required")
    @Size(max = 100, message = "Author too long")
    private String author;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title too long")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;
}
