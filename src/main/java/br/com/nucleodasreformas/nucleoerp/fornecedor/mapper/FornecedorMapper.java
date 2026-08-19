package br.com.nucleodasreformas.nucleoerp.fornecedor.mapper;

import br.com.nucleodasreformas.nucleoerp.fornecedor.dto.FornecedorRequest;
import br.com.nucleodasreformas.nucleoerp.fornecedor.dto.FornecedorResponse;
import br.com.nucleodasreformas.nucleoerp.fornecedor.entity.Fornecedor;

public class FornecedorMapper {

    private FornecedorMapper() {
    }

    public static Fornecedor toEntity(FornecedorRequest request){
        Fornecedor.FornecedorBuilder builder = Fornecedor.builder()
                .nome(request.getNome())
                .endereco(request.getEndereco())
                .celular(request.getCelular())
                .email(request.getEmail())
                .contato(request.getContato());

        if (request.getAtivo() != null) {
            builder.ativo(request.getAtivo());
        }

        return builder.build();
    }

    public static FornecedorResponse toResponse(Fornecedor fornecedor){
        return FornecedorResponse.builder()
                .id(fornecedor.getId())
                .nome(fornecedor.getNome())
                .endereco(fornecedor.getEndereco())
                .celular(fornecedor.getCelular())
                .email(fornecedor.getEmail())
                .contato(fornecedor.getContato())
                .ativo(fornecedor.getAtivo())
                .criadoEm(fornecedor.getCriadoEm())
                .build();
    }

    public static void updateEntity(Fornecedor fornecedor, FornecedorRequest request){
        fornecedor.setNome(request.getNome());
        fornecedor.setEndereco(request.getEndereco());
        fornecedor.setCelular(request.getCelular());
        fornecedor.setEmail(request.getEmail());
        fornecedor.setContato(request.getContato());
        if (request.getAtivo() != null) {
            fornecedor.setAtivo(request.getAtivo());
        }
    }
}
