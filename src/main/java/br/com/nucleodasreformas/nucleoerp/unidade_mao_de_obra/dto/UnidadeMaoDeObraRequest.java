package br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Dados para cadastro de unidade de mão de obra")
public class UnidadeMaoDeObraRequest {

    @Schema(example = "Hora")
    @NotBlank(message = "O nome é obrigatório.")
    @Size(max = 100, message = "O nome deve possuir no máximo 100 caracteres.")
    private String nome;

    @Schema(defaultValue = "true")
    private Boolean ativo;
}
