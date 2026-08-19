package br.com.nucleodasreformas.nucleoerp.materialfornecedor.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MaterialResumoResponse {

    private Long id;

    private String nome;
}
