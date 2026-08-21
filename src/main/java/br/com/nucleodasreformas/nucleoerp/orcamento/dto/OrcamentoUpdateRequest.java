package br.com.nucleodasreformas.nucleoerp.orcamento.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Dados para correção do cliente do orçamento inicial")
public class OrcamentoUpdateRequest {

    @Schema(example = "10")
    @NotNull(message = "O cliente é obrigatório.")
    private Long clienteId;
}
