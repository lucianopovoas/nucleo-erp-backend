package br.com.nucleodasreformas.nucleoerp.orcamento_versao.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Dados para atualização da versão em rascunho")
public class OrcamentoVersaoUpdateRequest {

    @Setter(AccessLevel.NONE)
    @Schema(example = "Proposta revisada")
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
