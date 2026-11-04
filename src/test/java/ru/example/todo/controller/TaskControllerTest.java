package ru.example.todo.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.example.todo.service.TaskService;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TaskService service;

    @BeforeEach
    void setUp() {
        service.clear();
    }

    private void createTask(String title) throws Exception {
        mvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void createTaskReturns201AndTask() throws Exception {
        mvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Сделать ДЗ по CI/CD\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Сделать ДЗ по CI/CD")))
                .andExpect(jsonPath("$.completed", is(false)))
                .andExpect(jsonPath("$.createdAt", is(LocalDate.now().toString())));
    }

    @Test
    void createTaskWithBlankTitleReturns400() throws Exception {
        mvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.title").exists());
    }

    @Test
    void createTaskWithoutTitleReturns400() throws Exception {
        mvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTaskWithTooLongTitleReturns400() throws Exception {
        mvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + "a".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTaskWithInvalidJsonReturns400() throws Exception {
        mvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Некорректный JSON")));
    }

    @Test
    void getAllTasksReturnsList() throws Exception {
        createTask("Купить молоко");
        createTask("Сделать ДЗ");

        mvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title", is("Купить молоко")))
                .andExpect(jsonPath("$[1].title", is("Сделать ДЗ")));
    }

    @Test
    void getAllTasksWhenEmptyReturnsEmptyList() throws Exception {
        mvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getTaskByIdReturnsTask() throws Exception {
        createTask("Купить молоко");

        mvc.perform(get("/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Купить молоко")));
    }

    @Test
    void getUnknownTaskReturns404() throws Exception {
        mvc.perform(get("/tasks/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void updateTaskMarksCompleted() throws Exception {
        createTask("Купить молоко");

        mvc.perform(put("/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Купить молоко и хлеб\",\"completed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Купить молоко и хлеб")))
                .andExpect(jsonPath("$.completed", is(true)));
    }

    @Test
    void updateUnknownTaskReturns404() throws Exception {
        mvc.perform(put("/tasks/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"completed\":true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateTaskWithInvalidBodyReturns400() throws Exception {
        createTask("Купить молоко");

        mvc.perform(put("/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteTaskReturns204AndRemovesIt() throws Exception {
        createTask("Купить молоко");

        mvc.perform(delete("/tasks/1"))
                .andExpect(status().isNoContent());

        mvc.perform(get("/tasks"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void deleteUnknownTaskReturns404() throws Exception {
        mvc.perform(delete("/tasks/5"))
                .andExpect(status().isNotFound());
    }

    @Test
    void healthEndpointIsUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }
}
