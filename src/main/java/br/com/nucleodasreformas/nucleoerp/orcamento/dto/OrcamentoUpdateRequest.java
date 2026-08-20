package br.com.nucleodasreformas.nucleoerp.orcamento.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Dados para atualização parcial do cabeçalho do orçamento")
public class OrcamentoUpdateRequest {

    @Schema(example = "10")
    private Long clienteId;

    @Schema(example = "2")
    private Long statusOrcamentoId;

    @Setter(AccessLevel.NONE)
    @Schema(example = "Orçamento revisado")
    private String observacao;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean observacaoInformada;

    @JsonSetter("observacao")
    public void setObservacao(String observacao) {
        this.observacao = observacao;
        this.observacaoInformada = true;
    }

    @JsonIgnore
    public boolean isObservacaoInformada() {
        return observacaoInformada;
    }
}
