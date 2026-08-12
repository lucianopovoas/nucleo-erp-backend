package br.com.nucleodasreformas.nucleoerp.fornecedor.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FornecedorResponse {

    private Long id;

    private String nome;

    private String endereco;

    private String celular;

    private String contato;

    private Boolean ativo;

    private LocalDateTime criadoEm;
}
