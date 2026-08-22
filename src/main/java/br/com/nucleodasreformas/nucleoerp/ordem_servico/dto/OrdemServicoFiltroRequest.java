package br.com.nucleodasreformas.nucleoerp.ordem_servico.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@Schema(description = "Filtros opcionais para a listagem de ordens de serviço")
public class OrdemServicoFiltroRequest {

    @Positive(message = "O número deve ser maior que zero.")
    @Schema(example = "45", nullable = true)
    private Long numero;

    @Size(max = 50, message = "O status deve possuir no máximo 50 caracteres.")
    @Pattern(regexp = "(?s).*\\S.*", message = "O status não pode ser vazio.")
    @Schema(example = "EM_EXECUCAO", nullable = true)
    private String status;

    @Positive(message = "O id do cliente deve ser maior que zero.")
    @Schema(example = "10", nullable = true)
    private Long clienteId;

    @Positive(message = "O id do orçamento deve ser maior que zero.")
    @Schema(example = "25", nullable = true)
    private Long orcamentoId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(example = "2026-08-01", type = "string", format = "date", nullable = true)
    private LocalDate criadoDe;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(example = "2026-08-31", type = "string", format = "date", nullable = true)
    private LocalDate criadoAte;
}
