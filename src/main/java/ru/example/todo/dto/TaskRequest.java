package ru.example.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Входные данные для создания и обновления задачи.
 * id и createdAt задаёт сервер, клиент передаёт только title и completed.
 */
public record TaskRequest(
        @NotBlank(message = "title не должен быть пустым")
        @Size(max = 255, message = "title не длиннее 255 символов")
        String title,

        Boolean completed
) {
}
