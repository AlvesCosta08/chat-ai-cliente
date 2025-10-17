package com.codigoquatro.atendimento_ai.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KnowledgeEntry {

    @JsonProperty("question")
    private String question;

    @JsonProperty("answer")
    private String answer;

    public KnowledgeEntry() {
        System.out.println("KnowledgeEntry instanciado");
    }

    public KnowledgeEntry(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
