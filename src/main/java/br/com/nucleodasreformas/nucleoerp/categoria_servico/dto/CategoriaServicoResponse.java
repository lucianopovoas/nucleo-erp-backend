package br.com.nucleodasreformas.nucleoerp.categoria_servico.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CategoriaServicoResponse {

    private Long id;
    private String nome;
    private Boolean ativo;
    private LocalDateTime criadoEm;
}
