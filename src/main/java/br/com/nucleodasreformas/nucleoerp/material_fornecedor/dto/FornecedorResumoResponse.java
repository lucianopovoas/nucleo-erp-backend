package br.com.nucleodasreformas.nucleoerp.material_fornecedor.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FornecedorResumoResponse {

    private Long id;

    private String nome;
}
