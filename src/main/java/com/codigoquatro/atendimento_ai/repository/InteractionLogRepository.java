package com.codigoquatro.atendimento_ai.repository;

import com.codigoquatro.atendimento_ai.model.InteractionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InteractionLogRepository extends JpaRepository<InteractionLog, Long> {

}
