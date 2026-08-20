package br.com.nucleodasreformas.nucleoerp.status_orcamento.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Dados para cadastro de status de orçamento")
public class StatusOrcamentoRequest {

    @Schema(example = "Rascunho")
    @NotBlank(message = "O nome é obrigatório.")
    @Size(max = 100, message = "O nome deve possuir no máximo 100 caracteres.")
    private String nome;

    @Schema(defaultValue = "true")
    private Boolean ativo;
}
