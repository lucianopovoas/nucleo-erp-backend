package br.com.nucleodasreformas.nucleoerp.ordem_servico.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Dados editáveis da ordem de serviço")
public class OrdemServicoUpdateRequest {

    @Setter(AccessLevel.NONE)
    @Schema(example = "Separar materiais antes do início da execução")
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
