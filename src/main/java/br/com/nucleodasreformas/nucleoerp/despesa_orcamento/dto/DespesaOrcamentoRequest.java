package br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Dados para inclusão de despesa prevista no orçamento")
public class DespesaOrcamentoRequest {

    @NotBlank(message = "A descrição é obrigatória.")
    @Size(max = 200, message = "A descrição deve ter no máximo 200 caracteres.")
    @Schema(example = "Frete")
    private String descricao;

    @NotNull(message = "O valor é obrigatório.")
    @PositiveOrZero(message = "O valor não pode ser negativo.")
    @Digits(integer = 13, fraction = 2,
            message = "O valor deve ter até 13 dígitos inteiros e 2 casas decimais.")
    @Schema(example = "180.00")
    private BigDecimal valor;
}
