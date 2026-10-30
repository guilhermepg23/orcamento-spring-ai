package dio.budgeting.application;

import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Orquestra o fluxo completo de voz: áudio -> texto -> tool calling -> áudio.
 * Fica na camada de aplicação para que o controller só cuide de HTTP.
 */
@Service
public class ProcessVoiceCommandUseCase {
    private final TranscriptionModel transcriptionModel;
    private final ChatClient chatClient;
    private final TextToSpeechModel textToSpeechModel;

    public ProcessVoiceCommandUseCase(TranscriptionModel transcriptionModel,
                                      TextToSpeechModel textToSpeechModel,
                                      ChatClient.Builder chatClientBuilder,
                                      @Value("classpath:prompts/system-message.st") Resource systemPrompt,
                                      PersistTransactionUseCase persistTransactionUseCase,
                                      ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase,
                                      SumTransactionsByCategoryUseCase sumTransactionsByCategoryUseCase,
                                      ListTransactionsByPeriodUseCase listTransactionsByPeriodUseCase,
                                      CurrentDateTool currentDateTool) throws IOException {
        this.transcriptionModel = transcriptionModel;
        this.textToSpeechModel = textToSpeechModel;
        this.chatClient = chatClientBuilder
                .defaultSystem(systemPrompt.getContentAsString(StandardCharsets.UTF_8))
                .defaultTools(persistTransactionUseCase,
                        listTransactionsByCategoryUseCase,
                        sumTransactionsByCategoryUseCase,
                        listTransactionsByPeriodUseCase,
                        currentDateTool)
                .build();
    }

    /**
     * @param audio áudio com o comando de voz
     * @return resposta falada, em MP3
     * @throws VoiceCommandException se não for possível entender o áudio ou gerar uma resposta
     */
    public byte[] execute(Resource audio) {
        String command = transcriptionModel.transcribe(audio);
        if (command == null || command.isBlank()) {
            throw new VoiceCommandException("Não foi possível transcrever o áudio");
        }

        String answer = chatClient.prompt().user(command).call().content();
        if (answer == null || answer.isBlank()) {
            throw new VoiceCommandException("O assistente não gerou uma resposta");
        }

        return textToSpeechModel.call(answer);
    }
}
