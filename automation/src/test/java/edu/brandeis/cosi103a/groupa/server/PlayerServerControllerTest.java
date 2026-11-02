package edu.brandeis.cosi103a.groupa.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import edu.brandeis.cosi.atg.cards.Card;
import edu.brandeis.cosi.atg.decisions.BuyDecision;
import edu.brandeis.cosi103a.groupa.network.DecisionRequest;
import edu.brandeis.cosi103a.groupa.network.LogEventRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class PlayerServerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void decide_returns200AndDecision() throws Exception {
        DecisionRequest request = new DecisionRequest(
            null,
            ImmutableList.of(new BuyDecision(Card.Type.METHOD)),
            null,
            "player-1"
        );

        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/decide")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.decision").exists());
    }

    @Test
    public void decide_missingPlayerUuid_returns400() throws Exception {
        DecisionRequest request = new DecisionRequest(
            null,
            ImmutableList.of(new BuyDecision(Card.Type.METHOD)),
            null,
            null
        );

        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/decide")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void logEvent_returns200WithEmptyBody() throws Exception {
        LogEventRequest request = new LogEventRequest(null, null, "player-1");
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/log-event")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk());
    }
}
