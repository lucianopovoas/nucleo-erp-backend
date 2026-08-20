package br.com.nucleodasreformas.nucleoerp.servico.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ServicoResponse {

    private Long id;
    private String nome;
    private CategoriaServicoResumoResponse categoriaServico;
    private Boolean ativo;
    private LocalDateTime criadoEm;
}
