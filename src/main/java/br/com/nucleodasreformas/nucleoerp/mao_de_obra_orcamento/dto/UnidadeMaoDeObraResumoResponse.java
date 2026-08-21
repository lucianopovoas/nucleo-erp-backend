package br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UnidadeMaoDeObraResumoResponse {

    private Long id;
    private String nome;
}
