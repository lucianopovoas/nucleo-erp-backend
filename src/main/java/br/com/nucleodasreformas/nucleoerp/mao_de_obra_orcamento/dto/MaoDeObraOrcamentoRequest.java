package br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Dados para inclusão de mão de obra prevista no orçamento")
public class MaoDeObraOrcamentoRequest {

    @NotNull(message = "A unidade de mão de obra é obrigatória.")
    @Schema(example = "1")
    private Long unidadeMaoDeObraId;

    @NotBlank(message = "A descrição é obrigatória.")
    @Size(max = 200, message = "A descrição deve ter no máximo 200 caracteres.")
    @Schema(example = "Instalação de toldo")
    private String descricao;

    @NotNull(message = "A quantidade é obrigatória.")
    @Positive(message = "A quantidade deve ser maior que zero.")
    @Digits(integer = 11, fraction = 4,
            message = "A quantidade deve ter até 11 dígitos inteiros e 4 casas decimais.")
    @Schema(example = "2.0000")
    private BigDecimal quantidade;

    @NotNull(message = "O custo unitário é obrigatório.")
    @PositiveOrZero(message = "O custo unitário não pode ser negativo.")
    @Digits(integer = 13, fraction = 2,
            message = "O custo unitário deve ter até 13 dígitos inteiros e 2 casas decimais.")
    @Schema(example = "250.00")
    private BigDecimal custoUnitario;
}
