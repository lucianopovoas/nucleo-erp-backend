package br.com.nucleodasreformas.nucleoerp.cliente.mapper;

import br.com.nucleodasreformas.nucleoerp.cliente.dto.ClienteRequest;
import br.com.nucleodasreformas.nucleoerp.cliente.dto.ClienteResponse;
import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;

import java.util.Objects;

public class ClienteMapper {

    private ClienteMapper() {
    }

    public static Cliente toEntity(ClienteRequest request) {

        return Cliente.builder()
                .nome(request.getNome())
                .cpf(request.getCpf())
                .cnpj(request.getCnpj())
                .telefone(request.getTelefone())
                .celular(request.getCelular())
                .email(request.getEmail())
                .contato(request.getContato())
                .endereco(request.getEndereco())
                .ativo(request.isAtivo())
                .build();
    }

    public static ClienteResponse toResponse(Cliente cliente) {

        return ClienteResponse.builder()
                .id(cliente.getId())
                .nome(cliente.getNome())
                .cpf(cliente.getCpf())
                .cnpj(cliente.getCnpj())
                .telefone(cliente.getTelefone())
                .celular(cliente.getCelular())
                .email(cliente.getEmail())
                .contato(cliente.getContato())
                .endereco(cliente.getEndereco())
                .ativo(cliente.getAtivo())
                .criadoEm(cliente.getCriadoEm())
                .build();
    }

    public static void updateEntity(Cliente cliente, ClienteRequest request){

        cliente.setNome(request.getNome());
        cliente.setCpf(request.getCpf());
        cliente.setCnpj(request.getCnpj());
        cliente.setTelefone(request.getTelefone());
        cliente.setCelular(request.getCelular());
        cliente.setEmail(request.getEmail());
        cliente.setContato(request.getContato());
        cliente.setEndereco(request.getEndereco());
        cliente.setAtivo(request.isAtivo());
    }
}