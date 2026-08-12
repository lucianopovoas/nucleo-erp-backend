package br.com.nucleodasreformas.nucleoerp.cliente.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ClienteResponse {

    private Long id;

    private String nome;

    private String cpf;

    private String cnpj;

    private String telefone;

    private String celular;

    private String email;

    private String contato;

    private String endereco;

    private Boolean ativo;

    private LocalDateTime criadoEm;

}