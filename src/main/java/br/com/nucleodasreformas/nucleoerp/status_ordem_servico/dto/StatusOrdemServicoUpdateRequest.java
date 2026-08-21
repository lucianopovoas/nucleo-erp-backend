package br.com.nucleodasreformas.nucleoerp.status_ordem_servico.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Dados para atualização de status de ordem de serviço")
public class StatusOrdemServicoUpdateRequest {

    @Schema(example = "Comprar materiais")
    @NotBlank(message = "O nome é obrigatório.")
    @Size(max = 100, message = "O nome deve possuir no máximo 100 caracteres.")
    private String nome;

    @Schema(defaultValue = "true")
    private Boolean ativo;
}
