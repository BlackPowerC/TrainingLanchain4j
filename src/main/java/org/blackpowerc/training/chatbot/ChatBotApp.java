package org.blackpowerc.training.chatbot;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static org.blackpowerc.training.chatbot.EmbeddingModels.PARAPHRASE_MUTILANGUAL_278M;

public class ChatBotApp
{
    public static void main(String[] args)
    {
        final String pgVectorPass = "langchain" ;
        final String ollamaBaseUrl = "http://127.0.0.1:11434" ;
        final int pgVectorMappedPort = 5432 ;
        final boolean enableLogs = false ;

        LLMConfiguration llmConf = LLMConfiguration.getInstance() ;
        EmbeddingModels embeddingModels = PARAPHRASE_MUTILANGUAL_278M ;

        // https://github.com/pgvector/pgvector/
        try (GenericContainer<?> pg = new GenericContainer<>(DockerImageName.parse("pgvector/pgvector").withTag("pg17")))
        {
            pg.setWaitStrategy(Wait.forListeningPorts(5432)) ;
            pg.withEnv(
                        Map.of(
                        "POSTGRES_USER", pgVectorPass,
                        "POSTGRES_PASSWORD", pgVectorPass,
                        "POSTGRES_DATABASE", pgVectorPass)
                    )
                    .withExposedPorts(pgVectorMappedPort)
                    .withPrivilegedMode(true) ;
            pg.start() ;

            EmbeddingStore<TextSegment> embeddingStore = llmConf.getPGVectorStore(
                    pgVectorPass,
                    pgVectorPass,
                    pgVectorPass,
                    pgVectorPass,
                    pg.getHost(),
                    pg.getMappedPort(pgVectorMappedPort),
                    embeddingModels.vectorDimension(),
                    true
            ) ;

            EmbeddingModel embeddingModel = llmConf.getOllamaEmbeddingModel(
                    ollamaBaseUrl,
                    embeddingModels.modelName(),
                    enableLogs
            ) ;

            ChatModel ollamaChatModel = OllamaChatModel
                    .builder()
                    .baseUrl(ollamaBaseUrl)
                    .modelName(LLMModels.MISTRAL_7B.modelName())
                    .numCtx(4096)
                    .temperature(0.1)
                    .logRequests(enableLogs)
                    .logResponses(enableLogs)
                    .timeout(Duration.ofMinutes(1))
                    .maxRetries(5)
                    .build() ;

            RetrievalAugmentor ra = llmConf.getRetrievalAugmentor(
                    List.of("https://www.goafricaonline.com/tg/58173-diwa-international-concessionnaires-automobiles-moto-lome-togo"),
                    embeddingModel,
                    embeddingStore,
                    ollamaChatModel,
                    """
                            Répondez à la question en vous basant sur le contexte ci-dessous.
                                   
                            Si vous n'êtes pas sûr de la réponse, indiquez «Je ne sais pas quoi répondre» dans la même langue que la question,
                            ou si vous ne disposez pas de suffisamment d'informations contextuelles.
                                   
                            Contexte: {{contents}}
                                   
                            Question: {{userMessage}}
                                   
                            Réponse:
                            """) ;

            LLMAssistant assistant = llmConf.getLLMAssistant(
                    ollamaChatModel,
                    30,
                    ra
            ) ;

            Scanner sc = new Scanner(System.in) ;
            String userInput ;

            System.out.println("Press CRTL + C or type 'stop' to exit.\n");

            do
            {
                System.out.print("[user input]: ");
                userInput = sc.nextLine() ;
                if(userInput != null && userInput.compareToIgnoreCase("stop") != 0) {
                    System.out.println("[bot answer]: " + assistant.chat(userInput));
                }
            }
            while (userInput != null && userInput.compareToIgnoreCase("stop") != 0) ;

            sc.close() ;
        }
    }
}
