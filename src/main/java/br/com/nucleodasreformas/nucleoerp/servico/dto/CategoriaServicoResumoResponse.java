package br.com.nucleodasreformas.nucleoerp.servico.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoriaServicoResumoResponse {

    private Long id;
    private String nome;
}
