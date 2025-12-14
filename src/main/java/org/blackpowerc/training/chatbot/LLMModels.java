package org.blackpowerc.training.chatbot;

public enum LLMModels
{
    // https://ollama.com/library/qwen2.5-coder
    QWEN2_5_CODER_7B("qwen2.5-coder:7b"),
    // https://ollama.com/library/qwen2.5
    QWEN2_5_3B("qwen2.5:3b"),
    // https://ollama.com/library/llama3.2
    LLAMA3_2_1B("llama3.2:1b"),
    // https://ollama.com/library/granite3-moe
    GRANITE3_MOE_3B("granite3-moe:3b"),
    // https://ollama.com/library/mistral
    MISTRAL_7B("mistral:7b") ;

    private final String modelName ;

    LLMModels(String modelName) {
        this.modelName = modelName ;
    }

    public String modelName() {
        return modelName ;
    }

    @Override
    public String toString() {
        return this.modelName ;
    }
}
