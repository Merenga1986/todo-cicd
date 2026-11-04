package ru.example.todo.service;

import org.springframework.stereotype.Service;
import ru.example.todo.dto.TaskRequest;
import ru.example.todo.exception.TaskNotFoundException;
import ru.example.todo.model.Task;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Хранение задач в памяти (List&lt;Task&gt;), без БД.
 */
@Service
public class TaskService {

    private final List<Task> tasks = new ArrayList<>();
    private final AtomicLong idSequence = new AtomicLong(0);

    public synchronized List<Task> findAll() {
        return List.copyOf(tasks);
    }

    public synchronized Task findById(Long id) {
        return tasks.stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    public synchronized Task create(TaskRequest request) {
        Task task = new Task(
                idSequence.incrementAndGet(),
                request.title().trim(),
                Boolean.TRUE.equals(request.completed()),
                LocalDate.now());
        tasks.add(task);
        return task;
    }

    public synchronized Task update(Long id, TaskRequest request) {
        Task task = findById(id);
        task.setTitle(request.title().trim());
        if (request.completed() != null) {
            task.setCompleted(request.completed());
        }
        return task;
    }

    public synchronized void delete(Long id) {
        Task task = findById(id);
        tasks.remove(task);
    }

    /** Нужен для изоляции тестов. */
    public synchronized void clear() {
        tasks.clear();
        idSequence.set(0);
    }
}
