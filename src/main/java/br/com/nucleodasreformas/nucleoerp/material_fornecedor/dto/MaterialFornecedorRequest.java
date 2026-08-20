package br.com.nucleodasreformas.nucleoerp.material_fornecedor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Dados do vínculo entre material e fornecedor")
public class MaterialFornecedorRequest {

    @NotNull(message = "O material é obrigatório.")
    @Schema(example = "1")
    private Long materialId;

    @NotNull(message = "O fornecedor é obrigatório.")
    @Schema(example = "2")
    private Long fornecedorId;

    @PositiveOrZero(message = "O preço de compra não pode ser negativo.")
    @Digits(integer = 13, fraction = 2, message = "O preço de compra deve ter até 13 dígitos inteiros e 2 casas decimais.")
    @Schema(example = "125.50", nullable = true)
    private BigDecimal precoCompra;
}
