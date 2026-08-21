package br.com.nucleodasreformas.nucleoerp.material_orcamento.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MaterialResumoResponse {

    private Long id;
    private String nome;
}
