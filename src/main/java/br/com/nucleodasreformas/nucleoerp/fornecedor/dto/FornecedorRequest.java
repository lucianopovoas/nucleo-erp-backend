package br.com.nucleodasreformas.nucleoerp.fornecedor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Dados para cadastro de fornecedor")
public class FornecedorRequest {

    @Schema(example = "João da Silva")
    @NotBlank(message = "O nome é obrigatório.")
    private String nome;

    @Schema(example = "rua x, travessa y")
    @Size(max = 100)
    private String endereco;

    @Schema(example = "71999999999")
    @Size(max = 20)
    private String celular;

    @Schema(example = "joao")
    @Size(max = 50)
    private String contato;

    @NotNull(message = "O campo ativo é obrigatório.")
    private boolean ativo;
}
