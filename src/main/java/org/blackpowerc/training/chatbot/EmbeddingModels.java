package org.blackpowerc.training.chatbot;

public enum EmbeddingModels
{
    // https://huggingface.co/nomic-ai/nomic-embed-text-v1.5
    NOMIC_EMBED_TEXT_V1_5("nomic-embed-text:latest", 512),
    // https://ollama.com/library/paraphrase-multilingual
    PARAPHRASE_MUTILANGUAL_278M("paraphrase-multilingual:278m", 768),
    // https://huggingface.co/sentence-transformers
    ALL_MINILM_L12("all-minilm:l12", 384);

    private final String modelName ;

    private final int vectorDimension ;

    EmbeddingModels(String modelName, int vectorDimension)
    {
        this.modelName = modelName ;
        this.vectorDimension = vectorDimension ;
    }

    public int vectorDimension() {
        return vectorDimension  ;
    }

    public String modelName() {
        return modelName ;
    }

    @Override
    public String toString() {
        return this.modelName ;
    }
}
