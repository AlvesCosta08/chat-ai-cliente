package com.codigoquatro.atendimento_ai.repository;

import com.codigoquatro.atendimento_ai.model.InteractionLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.username=sa",
        "spring.datasource.password=password"
})
class InteractionLogRepositoryTest {

    @Autowired
    private InteractionLogRepository repository;

    @Test
    void shouldSaveAndFindInteractionLog() {
        // Arrange
        InteractionLog log = new InteractionLog("Pergunta teste", "Resposta teste");

        // Act
        InteractionLog saved = repository.save(log);
        InteractionLog found = repository.findById(saved.getId()).orElse(null);

        // Assert
        assertNotNull(found);
        assertEquals("Pergunta teste", found.getQuestion());
        assertEquals("Resposta teste", found.getAnswer());
    }

    @Test
    void shouldReturnEmptyWhenIdDoesNotExist() {
        // Act
        var found = repository.findById(-1L);

        // Assert
        assertTrue(found.isEmpty());
    }
}
