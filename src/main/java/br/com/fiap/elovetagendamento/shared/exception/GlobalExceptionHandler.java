package br.com.fiap.elovetagendamento.shared.exception;

import br.com.fiap.elovetagendamento.agendamento.exception.AgendamentoNotFoundException;
import br.com.fiap.elovetagendamento.agendamento.exception.AgendamentoRegraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AgendamentoNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(AgendamentoNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(AgendamentoRegraException.class)
    public ResponseEntity<ErrorResponse> handleRegra(AgendamentoRegraException exception) {
        log.warn("Regra de negócio violada: {}", exception.getMessage());
        return build(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidacao(MethodArgumentNotValidException exception) {
        List<String> detalhes = exception.getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .toList();
        log.warn("Requisição com dados inválidos: {}", detalhes);
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Dados inválidos", detalhes));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleFormato(Exception exception) {
        log.warn("Requisição com formato inválido: {}", exception.getMessage());
        return build(HttpStatus.BAD_REQUEST,
                "Requisição com formato inválido (verifique datas, números e valores de status)");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegridade(DataIntegrityViolationException exception) {
        log.warn("Violação de integridade no Oracle: {}", exception.getMostSpecificCause().getMessage());
        return build(HttpStatus.UNPROCESSABLE_ENTITY,
                "Os dados violam uma regra do banco (pet, responsável, veterinário ou clínica inexistente, ou status inválido)");
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleBanco(DataAccessException exception) {
        log.error("Erro de acesso ao banco", exception);
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Erro ao acessar o banco de dados: "
                + exception.getMostSpecificCause().getMessage());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), mensagem));
    }
}
