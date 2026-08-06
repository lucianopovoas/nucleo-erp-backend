package br.com.nucleodasreformas.nucleoerp.fornecedor.mapper;

import br.com.nucleodasreformas.nucleoerp.fornecedor.dto.FornecedorRequest;
import br.com.nucleodasreformas.nucleoerp.fornecedor.dto.FornecedorResponse;
import br.com.nucleodasreformas.nucleoerp.fornecedor.entity.Fornecedor;

public class FornecedorMapper {

    private FornecedorMapper() {
    }

    public static Fornecedor toEntity(FornecedorRequest request){
        return Fornecedor.builder()
                .nome(request.getNome())
                .endereco(request.getEndereco())
                .celular(request.getCelular())
                .contato(request.getContato())
                .ativo(request.isAtivo())
                .build();
    }

    public static FornecedorResponse toResponse(Fornecedor fornecedor){
        return FornecedorResponse.builder()
                .nome(fornecedor.getNome())
                .endereco(fornecedor.getEndereco())
                .celular(fornecedor.getCelular())
                .contato(fornecedor.getContato())
                .ativo(fornecedor.getAtivo())
                .build();
    }

    public static void updateEntity(Fornecedor fornecedor, FornecedorRequest request){
        fornecedor.setNome(request.getNome());
        fornecedor.setEndereco(request.getEndereco());
        fornecedor.setCelular(request.getCelular());
        fornecedor.setContato(request.getContato());
        fornecedor.setAtivo(request.isAtivo());
    }
}
