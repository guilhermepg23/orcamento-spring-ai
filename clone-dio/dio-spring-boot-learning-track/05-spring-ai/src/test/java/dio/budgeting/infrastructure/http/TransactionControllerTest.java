package dio.budgeting.infrastructure.http;

import dio.budgeting.application.ListTransactionsByCategoryUseCase;
import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.application.ProcessVoiceCommandUseCase;
import dio.budgeting.application.VoiceCommandException;
import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionControllerTest {
    private PersistTransactionUseCase persist;
    private ListTransactionsByCategoryUseCase list;
    private ProcessVoiceCommandUseCase voice;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        persist = mock(PersistTransactionUseCase.class);
        list = mock(ListTransactionsByCategoryUseCase.class);
        voice = mock(ProcessVoiceCommandUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(new TransactionController(persist, list, voice)).build();
    }

    @Test
    void should_createTransaction() throws Exception {
        when(persist.execute(any())).thenReturn(
                new TransactionOutput("id-1", "Mercado", "GROCERIES", 80.0, "2026-10-30T10:00:00Z"));

        mvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Mercado","category":"GROCERIES","amount":8000}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("id-1"))
                .andExpect(jsonPath("$.amount").value(80.0));
    }

    @Test
    void should_listTransactionsByCategory() throws Exception {
        when(list.execute(Category.PHARMA)).thenReturn(
                List.of(new TransactionOutput("id-2", "Remédio", "PHARMA", 30.0, null)));

        mvc.perform(get("/transactions/PHARMA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].description").value("Remédio"));
    }

    @Test
    void should_returnAudioForVoiceCommand() throws Exception {
        when(voice.execute(any(Resource.class))).thenReturn(new byte[]{1, 2, 3, 4});

        mvc.perform(multipart("/transactions/ai")
                        .file(new MockMultipartFile("file", "cmd.m4a", "audio/mp4", new byte[]{1, 2})))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/mpeg"))
                .andExpect(content().bytes(new byte[]{1, 2, 3, 4}));
    }

    @Test
    void should_rejectEmptyAudio() throws Exception {
        mvc.perform(multipart("/transactions/ai")
                        .file(new MockMultipartFile("file", "cmd.m4a", "audio/mp4", new byte[0])))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return422WhenVoiceCommandCannotBeProcessed() throws Exception {
        when(voice.execute(any(Resource.class))).thenThrow(new VoiceCommandException("Não foi possível transcrever o áudio"));

        mvc.perform(multipart("/transactions/ai")
                        .file(new MockMultipartFile("file", "cmd.m4a", "audio/mp4", new byte[]{1, 2})))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Não foi possível transcrever o áudio"));
    }
}
