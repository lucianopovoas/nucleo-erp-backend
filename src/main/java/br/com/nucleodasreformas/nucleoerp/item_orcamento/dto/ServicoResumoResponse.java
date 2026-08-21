package br.com.nucleodasreformas.nucleoerp.item_orcamento.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServicoResumoResponse {

    private Long id;
    private String nome;
}
