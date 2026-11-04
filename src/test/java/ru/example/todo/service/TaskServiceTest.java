package ru.example.todo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.example.todo.dto.TaskRequest;
import ru.example.todo.exception.TaskNotFoundException;
import ru.example.todo.model.Task;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskServiceTest {

    private TaskService service;

    @BeforeEach
    void setUp() {
        service = new TaskService();
    }

    @Test
    void createAssignsIdDateAndDefaults() {
        Task task = service.create(new TaskRequest("  Купить молоко  ", null));

        assertThat(task.getId()).isEqualTo(1L);
        assertThat(task.getTitle()).isEqualTo("Купить молоко");
        assertThat(task.isCompleted()).isFalse();
        assertThat(task.getCreatedAt()).isEqualTo(LocalDate.now());
    }

    @Test
    void createIncrementsIds() {
        service.create(new TaskRequest("a", false));
        Task second = service.create(new TaskRequest("b", true));

        assertThat(second.getId()).isEqualTo(2L);
        assertThat(second.isCompleted()).isTrue();
        assertThat(service.findAll()).hasSize(2);
    }

    @Test
    void findAllReturnsImmutableCopy() {
        service.create(new TaskRequest("a", false));

        assertThatThrownBy(() -> service.findAll().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void updateChangesTitleAndKeepsCompletedWhenNull() {
        Task created = service.create(new TaskRequest("old", true));

        Task updated = service.update(created.getId(), new TaskRequest("new", null));

        assertThat(updated.getTitle()).isEqualTo("new");
        assertThat(updated.isCompleted()).isTrue();
    }

    @Test
    void updateUnknownThrows() {
        assertThatThrownBy(() -> service.update(10L, new TaskRequest("x", true)))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("10");
    }

    @Test
    void deleteRemovesTask() {
        Task task = service.create(new TaskRequest("a", false));

        service.delete(task.getId());

        assertThat(service.findAll()).isEmpty();
    }

    @Test
    void deleteUnknownThrows() {
        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void clearResetsStorageAndIds() {
        service.create(new TaskRequest("a", false));
        service.clear();

        assertThat(service.findAll()).isEmpty();
        assertThat(service.create(new TaskRequest("b", false)).getId()).isEqualTo(1L);
    }
}
