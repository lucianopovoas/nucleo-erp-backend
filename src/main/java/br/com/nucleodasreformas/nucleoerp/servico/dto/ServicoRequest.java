package br.com.nucleodasreformas.nucleoerp.servico.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Dados para cadastro de serviço")
public class ServicoRequest {

    @Schema(example = "Instalação de toldo")
    @NotBlank(message = "O nome é obrigatório.")
    @Size(max = 200, message = "O nome deve possuir no máximo 200 caracteres.")
    private String nome;

    @Schema(example = "3")
    @NotNull(message = "A categoria de serviço é obrigatória.")
    private Long categoriaServicoId;

    @Schema(defaultValue = "true")
    private Boolean ativo;
}
