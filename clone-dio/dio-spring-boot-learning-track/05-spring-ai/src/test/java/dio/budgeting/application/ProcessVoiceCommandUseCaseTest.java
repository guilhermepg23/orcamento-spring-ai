package dio.budgeting.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessVoiceCommandUseCaseTest {
    private final Resource audio = new ByteArrayResource(new byte[]{1, 2, 3});

    private TranscriptionModel transcriptionModel;
    private TextToSpeechModel textToSpeechModel;
    private ChatClient chatClient;
    private ProcessVoiceCommandUseCase useCase;

    @BeforeEach
    void setUp() throws IOException {
        transcriptionModel = mock(TranscriptionModel.class);
        textToSpeechModel = mock(TextToSpeechModel.class);
        chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);

        var builder = mock(ChatClient.Builder.class, Answers.RETURNS_SELF);
        when(builder.build()).thenReturn(chatClient);

        useCase = new ProcessVoiceCommandUseCase(
                transcriptionModel,
                textToSpeechModel,
                builder,
                new ByteArrayResource("Você é um assistente".getBytes()),
                mock(PersistTransactionUseCase.class),
                mock(ListTransactionsByCategoryUseCase.class),
                mock(SumTransactionsByCategoryUseCase.class),
                mock(ListTransactionsByPeriodUseCase.class),
                mock(CurrentDateTool.class));
    }

    @Test
    void should_transcribeAskTheAssistantAndSpeakTheAnswer() {
        var mp3 = new byte[]{9, 9, 9};
        when(transcriptionModel.transcribe(any(Resource.class))).thenReturn("gastei 80 reais no mercado");
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("Registrei oitenta reais em mercado.");
        when(textToSpeechModel.call("Registrei oitenta reais em mercado.")).thenReturn(mp3);

        var result = useCase.execute(audio);

        assertThat(result).isEqualTo(mp3);
        verify(chatClient.prompt()).user("gastei 80 reais no mercado");
    }

    @Test
    void should_failWhenTranscriptionIsBlank() {
        when(transcriptionModel.transcribe(any(Resource.class))).thenReturn("   ");

        assertThatThrownBy(() -> useCase.execute(audio)).isInstanceOf(VoiceCommandException.class);
        verify(textToSpeechModel, never()).call(anyString());
    }

    @Test
    void should_failWhenAssistantDoesNotAnswer() {
        when(transcriptionModel.transcribe(any(Resource.class))).thenReturn("quanto gastei hoje?");
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn(null);

        assertThatThrownBy(() -> useCase.execute(audio)).isInstanceOf(VoiceCommandException.class);
        verify(textToSpeechModel, never()).call(anyString());
    }
}
