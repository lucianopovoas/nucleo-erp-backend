package br.com.nucleodasreformas.nucleoerp.cliente.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Dados para cadastro de cliente")
public class ClienteRequest {

    @Schema(example = "João da Silva")
    @NotBlank(message = "O nome é obrigatório.")
    @Size(max = 200, message = "O nome deve possuir no máximo 200 caracteres.")
    private String nome;

    @Schema(example = "40028922593")
    @Size(max = 14)
    private String cpf;

    @Schema(example = "52.360.761/0001-58")
    @Size(max = 18)
    private String cnpj;

    @Schema(example = "71999999999")
    @Size(max = 20)
    private String telefone;

    @Schema(example = "71999999999")
    @Size(max = 20)
    private String celular;

    @Schema(example = "joao@email.com")
    @Email
    @Size(max = 150, message = "O email deve possuir no máximo 150 caracteres.")
    private String email;

    @Schema(example = "joao")
    @Size(max = 150)
    private String contato;

    @Schema(example = "rua x, travessa y")
    private String endereco;

    @Schema(defaultValue = "true")
    private Boolean ativo;
}
