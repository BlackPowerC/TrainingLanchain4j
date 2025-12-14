# Setup
- Java 21
- Docker 18+ et l'image pgvector/pgvector:pg17
- maven 3.8+
- ollama 0.4.6+
- Modèle LLM: mistral:7b
- Modèle embedding: paraphrase-multilingual:278m

# Utilisation
- Compilation: mvn package
- Run: java -jar ./target/TrainingLangchain4j-1.0-SNAPSHOT.jar
- Instruction: CTRL + C ou saisissez 'stop' pour arrêter le programme