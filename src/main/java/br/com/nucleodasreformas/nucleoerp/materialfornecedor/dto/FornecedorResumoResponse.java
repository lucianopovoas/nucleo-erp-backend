package br.com.nucleodasreformas.nucleoerp.materialfornecedor.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FornecedorResumoResponse {

    private Long id;

    private String nome;
}
