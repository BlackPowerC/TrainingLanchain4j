package org.blackpowerc.training.chatbot;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.UrlDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Utilitaire pour créer les différents éléments pour le client ollama et le RAG.
 */
public class LLMConfiguration
{
    private static LLMConfiguration instance ;

    private LLMConfiguration() {}

    public static synchronized LLMConfiguration getInstance()
    {
        if(instance == null) {
            instance = new LLMConfiguration() ;
        }

        return instance ;
    }

    /**
     * Créer un embedding store PGVectore
     *
     * @param database Le nom de la base de données.
     * @param table La table à utiliser pour les vecteurs.
     * @param password
     * @param username
     * @param host
     * @param port
     * @param vectorDimension La dimension des vecteurs.
     * @param dropAndCreate Mettre à true pour supprimer et recréer table.
     * @return
     */
    public EmbeddingStore<TextSegment> getPGVectorStore(final String database, final String table, final String password, final String username, final String host, final int port, final int vectorDimension, final boolean dropAndCreate)
    {
        return PgVectorEmbeddingStore.builder()
                .database(database)
                .password(password)
                .user(username)
                .table(table)
                .host(host)
                .port(port)
                .createTable(dropAndCreate)
                .dimension(vectorDimension)
                .build() ;
    }

    /**
     * Créer un conteneur de mémoire pour le LLM à utiliser.
     *
     * @param memoryMaxMessage Le nombre maximal de messages à garder en mémoire. Au minimum 1.
     * @return
     */
    public ChatMemory getMessageWindowChatMemory(final int memoryMaxMessage)
    {
        if(memoryMaxMessage <= 0) {
            throw new IllegalArgumentException("memoryMaxMessage must be greater than 0") ;
        }
        return MessageWindowChatMemory.withMaxMessages(memoryMaxMessage) ;
    }

    /**
     * Obtenir un nouveau modèLe d'embedding depuis ollama.
     *
     * @param hostAndPort L'ip ou le nom d'hôte de ollama et son port. Ex: http://127.0.0.1:11434
     * @param model Le nom du modèle d'embedding à utiliser.
     * @return
     */
    public EmbeddingModel getOllamaEmbeddingModel(final String hostAndPort, final String model)
    {
        Objects.requireNonNull(model) ;
        Objects.requireNonNull(hostAndPort) ;

        return OllamaEmbeddingModel.builder()
                .maxRetries(5)
                .baseUrl(hostAndPort)
                .logRequests(true)
                .logResponses(true)
                .timeout(Duration.ofMinutes(1))
                .modelName(model)
                .build() ;
    }

    /**
     *
     * @param languageModel Le (client) LLM à utiliser.
     * @param maxMemoryMessages Le nombre maximal de messages à garder en mémoire. Au minimum 1.
     * @param ra Le RAG
     * @return
     */
    public LLMAssistant getLLMAssistant(final ChatModel languageModel, final int maxMemoryMessages, final RetrievalAugmentor ra)
    {
        Objects.requireNonNull(ra) ;
        AiServices<LLMAssistant> builder = AiServices.builder(LLMAssistant.class) ;
        builder.chatModel(languageModel) ;
        builder.chatMemory(getMessageWindowChatMemory(30)) ;
        builder.retrievalAugmentor(ra) ;

        return builder.build() ;
    }

    /**
     * Obtenir un RAG pour l'utiliser avec un LLM.
     *
     * @param documentsSourceUrl Une liste d'URL de document texte.
     * @param embeddingModel L'embedding model à utiliser pour le RAG.
     * @param embeddingStore L'embedding store à utiliser pour le RAG.
     * @param languageModel Le modèle de llm
     * @param promptTemplate Un prompt personnalisé pour instruire le RAG et le LLM.
     * @return
     */
    public RetrievalAugmentor getRetrievalAugmentor(final List<String> documentsSourceUrl, final EmbeddingModel embeddingModel, final EmbeddingStore<TextSegment> embeddingStore, final ChatModel languageModel, final String promptTemplate)
    {
        Objects.requireNonNull(documentsSourceUrl, "No documents source URLs provided") ;
        Objects.requireNonNull(embeddingModel) ;
        Objects.requireNonNull(languageModel) ;
        Objects.requireNonNull(embeddingStore) ;

        if(documentsSourceUrl.isEmpty()) {
            throw new IllegalArgumentException("Documents source URLs is empty") ;
        }

        final DocumentParser parser = new TextDocumentParser() ;
        List<Document> documents = documentsSourceUrl
                .stream()
                .map(document -> UrlDocumentLoader.load(document, parser))
                .collect(Collectors.toList()) ;

        DocumentSplitter docSplitter = DocumentSplitters.recursive(
                512,
                0
        ) ;

        // Ingesteur de données
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor
                .builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .documentSplitter(docSplitter)
                .build() ;

        ingestor.ingest(documents) ;

        // Création du content retriever
        ContentRetriever retriever = EmbeddingStoreContentRetriever
                .builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .maxResults(5)
                .minScore(0.5)
                .build() ;

        var builder = DefaultRetrievalAugmentor.builder() ;
        // Création de l'augmenteur
        builder.contentRetriever(retriever) ;
        builder.executor(new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors(), 60L, TimeUnit.SECONDS, new SynchronousQueue<>())) ;
        builder.queryTransformer(new CompressingQueryTransformer(languageModel)) ;

        if(promptTemplate != null && !promptTemplate.isBlank())
        {
            builder.contentInjector(
                    new DefaultContentInjector(
                            new PromptTemplate(promptTemplate)
                    )
            ) ;
        }

        return builder.build();
    }
}
