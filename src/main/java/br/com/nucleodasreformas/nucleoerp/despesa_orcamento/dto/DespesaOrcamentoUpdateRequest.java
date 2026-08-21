package br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "Dados para atualização parcial de despesa prevista no orçamento")
public class DespesaOrcamentoUpdateRequest {

    @Setter(AccessLevel.NONE)
    @Size(max = 200, message = "A descrição deve ter no máximo 200 caracteres.")
    @Schema(example = "Frete adicional", nullable = true)
    private String descricao;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean descricaoInformada;

    @PositiveOrZero(message = "O valor não pode ser negativo.")
    @Digits(integer = 13, fraction = 2,
            message = "O valor deve ter até 13 dígitos inteiros e 2 casas decimais.")
    @Schema(example = "50.00", nullable = true)
    private BigDecimal valor;

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
