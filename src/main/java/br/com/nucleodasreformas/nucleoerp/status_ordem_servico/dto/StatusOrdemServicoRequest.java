package br.com.nucleodasreformas.nucleoerp.status_ordem_servico.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Dados para cadastro de status de ordem de serviço")
public class StatusOrdemServicoRequest {

    @Schema(example = "AGUARDANDO_LIBERACAO")
    @NotBlank(message = "O código é obrigatório.")
    @Size(max = 50, message = "O código deve possuir no máximo 50 caracteres.")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$",
            message = "O código deve começar com uma letra e conter apenas letras, números e underscore.")
    private String codigo;

    @Schema(example = "Aguardando liberação")
    @NotBlank(message = "O nome é obrigatório.")
    @Size(max = 100, message = "O nome deve possuir no máximo 100 caracteres.")
    private String nome;
}
