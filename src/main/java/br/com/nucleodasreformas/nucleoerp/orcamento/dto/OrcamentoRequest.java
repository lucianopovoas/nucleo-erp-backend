package br.com.nucleodasreformas.nucleoerp.orcamento.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Dados para criação de orçamento")
public class OrcamentoRequest {

    @Schema(example = "10")
    @NotNull(message = "O cliente é obrigatório.")
    private Long clienteId;

    @Schema(example = "Orçamento para reforma da área externa")
    private String observacao;
}
