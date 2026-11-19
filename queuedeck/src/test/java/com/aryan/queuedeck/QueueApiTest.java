package com.aryan.queuedeck;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Boots the real application (capacity 2, FIFO) and drives it over HTTP via MockMvc. */
@SpringBootTest(properties = {"queuedeck.policy=FIFO", "queuedeck.capacity=2"})
@AutoConfigureMockMvc
class QueueApiTest {

    @Autowired
    MockMvc mvc;

    @BeforeEach
    void reset() throws Exception {
        mvc.perform(delete("/api/jobs")).andExpect(status().isNoContent());
    }

    private void enqueue(String name) throws Exception {
        mvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void jobsComeBackInFifoOrderThenQueueIsEmpty() throws Exception {
        enqueue("first");
        enqueue("second");
        mvc.perform(post("/api/jobs/next"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("first"));
        mvc.perform(post("/api/jobs/next"))
                .andExpect(jsonPath("$.name").value("second"));
        mvc.perform(post("/api/jobs/next"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.empty").value(true));
    }

    @Test
    void thirdJobIsRejectedWhenQueueIsFull() throws Exception {
        enqueue("a");
        enqueue("b");
        mvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"c\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.rejected").value(true));
    }

    @Test
    void invalidJobsAreRejected() throws Exception {
        mvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"priority\":11}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void statsAndHealthEndpointsRespond() throws Exception {
        enqueue("a");
        mvc.perform(get("/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policy").value("FIFO"))
                .andExpect(jsonPath("$.capacity").value(2))
                .andExpect(jsonPath("$.depth").value(1));
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
