package br.com.nucleodasreformas.nucleoerp.orcamento.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatusOrcamentoResumoResponse {

    private Long id;
    private String nome;
}
