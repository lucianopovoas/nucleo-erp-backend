package br.com.nucleodasreformas.nucleoerp.materialfornecedor.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class MaterialFornecedorResponse {

    private Long id;

    private MaterialResumoResponse material;

    private FornecedorResumoResponse fornecedor;

    private BigDecimal precoCompra;

    private Boolean ativo;

    private LocalDateTime criadoEm;
}
