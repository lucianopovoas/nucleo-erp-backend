package br.com.nucleodasreformas.nucleoerp.material.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Dados para cadastro de material")
public class MaterialRequest {

    @Schema(example = "Lona")
    private String nome;

    @Schema(example = "Ja galvanizada")
    @Size(max = 100)
    private String descricao;

    @Schema(example = "Polegadas")
    @Size(max = 10)
    private String unidade;

    @Schema(example = "1.50")
    private BigDecimal largura;

    @Schema(defaultValue = "true")
    private Boolean ativo;
}
