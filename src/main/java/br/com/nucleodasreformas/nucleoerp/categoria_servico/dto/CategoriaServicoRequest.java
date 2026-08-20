package br.com.nucleodasreformas.nucleoerp.categoria_servico.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Dados para cadastro de categoria de serviço")
public class CategoriaServicoRequest {

    @Schema(example = "Pintura")
    @NotBlank(message = "O nome é obrigatório.")
    @Size(max = 200, message = "O nome deve possuir no máximo 200 caracteres.")
    private String nome;

    @Schema(defaultValue = "true")
    private Boolean ativo;
}
