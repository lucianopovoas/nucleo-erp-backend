package br.com.nucleodasreformas.nucleoerp.item_orcamento.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Dados para inclusão de um serviço negociado no orçamento")
public class ItemOrcamentoRequest {

    @NotNull(message = "O serviço é obrigatório.")
    @Schema(example = "5")
    private Long servicoId;

    @NotNull(message = "A quantidade é obrigatória.")
    @Positive(message = "A quantidade deve ser maior que zero.")
    @Digits(integer = 11, fraction = 4,
            message = "A quantidade deve ter até 11 dígitos inteiros e 4 casas decimais.")
    @Schema(example = "2.5000")
    private BigDecimal quantidade;

    @NotNull(message = "O valor unitário é obrigatório.")
    @PositiveOrZero(message = "O valor unitário não pode ser negativo.")
    @Digits(integer = 13, fraction = 2,
            message = "O valor unitário deve ter até 13 dígitos inteiros e 2 casas decimais.")
    @Schema(example = "150.00")
    private BigDecimal valorUnitario;

    @PositiveOrZero(message = "O desconto não pode ser negativo.")
    @Digits(integer = 13, fraction = 2,
            message = "O desconto deve ter até 13 dígitos inteiros e 2 casas decimais.")
    @Schema(example = "20.00", nullable = true)
    private BigDecimal desconto;
}
