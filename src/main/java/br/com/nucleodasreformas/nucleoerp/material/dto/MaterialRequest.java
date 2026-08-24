package br.com.nucleodasreformas.nucleoerp.material.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Digits;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Dados para cadastro de material")
public class MaterialRequest {

    @Schema(example = "Lona")
    @NotBlank(message = "O nome é obrigatório.")
    @Size(max = 200, message = "O nome deve possuir no máximo 200 caracteres.")
    private String nome;

    @Schema(example = "Ja galvanizada")
    private String descricao;

    @Schema(example = "Polegadas")
    @NotBlank(message = "A unidade é obrigatória.")
    @Size(max = 10, message = "A unidade deve possuir no máximo 10 caracteres.")
    private String unidade;

    @Schema(example = "1.50")
    @Digits(integer = 8, fraction = 2,
            message = "A largura deve ter até 8 dígitos inteiros e 2 casas decimais.")
    private BigDecimal largura;

    @Schema(defaultValue = "true")
    private Boolean ativo;
}
