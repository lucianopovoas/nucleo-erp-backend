package br.com.nucleodasreformas.nucleoerp.ordem_servico.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatusOrdemServicoResumoResponse {

    private Long id;
    private String codigo;
    private String nome;
}
