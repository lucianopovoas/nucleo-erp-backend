package br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UnidadeMaoDeObraResponse {

    private Long id;
    private String nome;
    private Boolean ativo;
    private LocalDateTime criadoEm;
}
