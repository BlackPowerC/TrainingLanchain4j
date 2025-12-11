package org.blackpowerc.training.chatbot;

import dev.langchain4j.service.SystemMessage;

public interface LLMAssistant
{
    @SystemMessage("Tu es un assistant chatbot convivial pour les questions et les réponses.")
    String chat(String message) ;
}
