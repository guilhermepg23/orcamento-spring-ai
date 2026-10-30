package dio.budgeting.infrastructure.http;

import dio.budgeting.application.ListTransactionsByCategoryUseCase;
import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.application.ProcessVoiceCommandUseCase;
import dio.budgeting.application.VoiceCommandException;
import dio.budgeting.domain.Category;
import dio.budgeting.infrastructure.http.request.TransactionRequest;
import dio.budgeting.infrastructure.http.response.TransactionResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {
    private static final String MP3 = "audio/mpeg";

    private final PersistTransactionUseCase persistTransactionUseCase;
    private final ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase;
    private final ProcessVoiceCommandUseCase processVoiceCommandUseCase;

    public TransactionController(PersistTransactionUseCase persistTransactionUseCase,
                                 ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase,
                                 ProcessVoiceCommandUseCase processVoiceCommandUseCase) {
        this.persistTransactionUseCase = persistTransactionUseCase;
        this.listTransactionsByCategoryUseCase = listTransactionsByCategoryUseCase;
        this.processVoiceCommandUseCase = processVoiceCommandUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(@RequestBody TransactionRequest request) {
        var transaction = persistTransactionUseCase.execute(request.toInput());
        return TransactionResponse.from(transaction);
    }

    @GetMapping("/{category}")
    public List<TransactionResponse> readTransactions(@PathVariable Category category) {
        return listTransactionsByCategoryUseCase.execute(category).stream().map(TransactionResponse::from).toList();
    }

    @PostMapping(value = "/ai", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MP3)
    public ResponseEntity<Resource> voiceCommand(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O arquivo de áudio está vazio");
        }

        byte[] audio = processVoiceCommandUseCase.execute(file.getResource());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(MP3))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("audio.mp3").build().toString())
                .body(new ByteArrayResource(audio));
    }

    @ExceptionHandler(VoiceCommandException.class)
    public ResponseEntity<ProblemDetail> handleVoiceCommandException(VoiceCommandException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage()));
    }
}
