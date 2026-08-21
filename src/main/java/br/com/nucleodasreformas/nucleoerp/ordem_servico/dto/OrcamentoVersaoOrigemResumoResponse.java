package br.com.nucleodasreformas.nucleoerp.ordem_servico.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrcamentoVersaoOrigemResumoResponse {

    private Long id;
    private Integer numeroVersao;
}
