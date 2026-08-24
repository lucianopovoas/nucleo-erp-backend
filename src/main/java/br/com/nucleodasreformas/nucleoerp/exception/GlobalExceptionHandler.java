package br.com.nucleodasreformas.nucleoerp.exception;

import org.apache.poi.EmptyFileException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Recurso não encontrado");
        problem.setDetail(ex.getMessage());

        return problem;
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex) {

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Erro de negócio");
        problem.setDetail(ex.getMessage());

        return problem;
    }

    @ExceptionHandler(IOException.class)
    public ProblemDetail handleIOException(IOException ex) {

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Erro ao importar arquivo");
        problem.setDetail(ex.getMessage());

        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                erros.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Dados inválidos");
        problem.setDetail("Um ou mais campos estão inválidos.");
        problem.setProperty("erros", erros);

        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleJsonInvalido(HttpMessageNotReadableException ex) {
        return problem(HttpStatus.BAD_REQUEST, "JSON inválido",
                "O corpo da requisição não contém um JSON válido.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleParametroInvalido(MethodArgumentTypeMismatchException ex) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Parâmetro inválido",
                "Um parâmetro da requisição possui valor incompatível com o tipo esperado.");
        problem.setProperty("erros", Map.of(
                ex.getName(), "O valor informado não é compatível com o tipo esperado."));
        return problem;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleParametroAusente(MissingServletRequestParameterException ex) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Requisição inválida",
                "Um parâmetro obrigatório não foi informado.");
        problem.setProperty("erros", Map.of(ex.getParameterName(), "O parâmetro é obrigatório."));
        return problem;
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ProblemDetail handleParteMultipartAusente(MissingServletRequestPartException ex) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Requisição inválida",
                "Uma parte obrigatória do multipart não foi informada.");
        problem.setProperty("erros", Map.of(ex.getRequestPartName(), "O arquivo é obrigatório."));
        return problem;
    }

    @ExceptionHandler(EmptyFileException.class)
    public ProblemDetail handleArquivoVazio(EmptyFileException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Erro ao importar arquivo",
                "O arquivo enviado está vazio.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleArquivoMuitoGrande(MaxUploadSizeExceededException ex) {
        return problem(HttpStatus.CONTENT_TOO_LARGE, "Arquivo muito grande",
                "O arquivo excede o limite aceito pela aplicação.");
    }

    @ExceptionHandler(MultipartException.class)
    public ProblemDetail handleMultipartInvalido(MultipartException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Multipart inválido",
                "A requisição multipart não pôde ser processada.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleMediaTypeNaoSuportado(HttpMediaTypeNotSupportedException ex) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de conteúdo não suportado",
                "O tipo de conteúdo enviado não é aceito por este endpoint.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMetodoNaoSuportado(HttpRequestMethodNotSupportedException ex) {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP não permitido",
                "O método HTTP enviado não é aceito por este endpoint.");
    }

    private ProblemDetail problem(HttpStatusCode status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(title);
        problem.setDetail(detail);
        return problem;
    }

}
