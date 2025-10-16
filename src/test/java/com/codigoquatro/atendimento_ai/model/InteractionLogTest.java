package com.codigoquatro.atendimento_ai.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

public class InteractionLogTest {

    @Test
    public void testInteractionLogConstructorAndGetters() {
        String question = "Como redefinir senha?";
        String answer = "Acesse login e clique em esqueci senha.";

        InteractionLog log = new InteractionLog(question, answer);

        assertEquals(question, log.getQuestion());
        assertEquals(answer, log.getAnswer());
        assertNotNull(log.getCreatedAt());
        assertNull(log.getId());
    }

    @Test
    public void testInteractionLogSetters() {
        InteractionLog log = new InteractionLog();
        log.setQuestion("Pergunta teste");
        log.setAnswer("Resposta teste");
        log.setId(1L);
        log.setCreatedAt(LocalDateTime.now());

        assertEquals("Pergunta teste", log.getQuestion());
        assertEquals("Resposta teste", log.getAnswer());
        assertEquals(1L, log.getId());
    }
}
