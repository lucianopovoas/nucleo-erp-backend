package br.com.nucleodasreformas.nucleoerp.item_orcamento.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "Dados para atualização parcial de um item do orçamento")
public class ItemOrcamentoUpdateRequest {

    @Schema(example = "5", nullable = true)
    private Long servicoId;

    @Setter(AccessLevel.NONE)
    @Size(max = 200, message = "A descrição deve ter no máximo 200 caracteres.")
    @Schema(example = "Instalação na área frontal", nullable = true)
    private String descricao;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean descricaoInformada;

    @Positive(message = "A quantidade deve ser maior que zero.")
    @Digits(integer = 11, fraction = 4,
            message = "A quantidade deve ter até 11 dígitos inteiros e 4 casas decimais.")
    @Schema(example = "2.5000", nullable = true)
    private BigDecimal quantidade;

    @PositiveOrZero(message = "O valor unitário não pode ser negativo.")
    @Digits(integer = 13, fraction = 2,
            message = "O valor unitário deve ter até 13 dígitos inteiros e 2 casas decimais.")
    @Schema(example = "150.00", nullable = true)
    private BigDecimal valorUnitario;

    @PositiveOrZero(message = "O desconto não pode ser negativo.")
    @Digits(integer = 13, fraction = 2,
            message = "O desconto deve ter até 13 dígitos inteiros e 2 casas decimais.")
    @Schema(example = "20.00", nullable = true)
    private BigDecimal desconto;

    @JsonSetter("descricao")
    public void setDescricao(String descricao) {
        this.descricao = descricao;
        this.descricaoInformada = true;
    }

    @JsonIgnore
    public boolean isDescricaoInformada() {
        return descricaoInformada;
    }

    @JsonIgnore
    @AssertTrue(message = "A descrição informada não pode ser nula ou vazia.")
    public boolean isDescricaoValida() {
        return !descricaoInformada || descricao != null && !descricao.trim().isEmpty();
    }
}
