package br.com.nucleodasreformas.nucleoerp.material.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class MaterialResponse {

    private Long id;

    private String nome;

    private String descricao;

    private String unidade;

    private BigDecimal largura;

    private Boolean ativo = true;

    private LocalDateTime criadoEm;
}
