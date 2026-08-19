package br.com.nucleodasreformas.nucleoerp.fornecedor.mapper;

import br.com.nucleodasreformas.nucleoerp.fornecedor.dto.FornecedorRequest;
import br.com.nucleodasreformas.nucleoerp.fornecedor.dto.FornecedorResponse;
import br.com.nucleodasreformas.nucleoerp.fornecedor.entity.Fornecedor;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class FornecedorMapperTest {

    @Test
    void deveConverterRequestParaEntidadeComTodosOsCampos() {
        FornecedorRequest request = requestCompleto();
        request.setAtivo(false);

        Fornecedor fornecedor = FornecedorMapper.toEntity(request);

        assertThat(fornecedor.getNome()).isEqualTo("Fornecedor A");
        assertThat(fornecedor.getEndereco()).isEqualTo("Rua A");
        assertThat(fornecedor.getCelular()).isEqualTo("71999998888");
        assertThat(fornecedor.getEmail()).isEqualTo("fornecedor@teste.com");
        assertThat(fornecedor.getContato()).isEqualTo("Contato A");
        assertThat(fornecedor.getAtivo()).isFalse();
    }

    @Test
    void deveAplicarAtivoComoVerdadeiroQuandoRequestNaoInformarValor() {
        Fornecedor fornecedor = FornecedorMapper.toEntity(requestCompleto());

        assertThat(fornecedor.getAtivo()).isTrue();
    }

    @Test
    void deveConverterEntidadeParaResponseCompleto() {
        LocalDateTime criadoEm = LocalDateTime.of(2026, 8, 19, 11, 0);
        Fornecedor fornecedor = Fornecedor.builder().id(2L).nome("Fornecedor A")
                .endereco("Rua A").celular("71999998888").email("fornecedor@teste.com")
                .contato("Contato A").ativo(false).criadoEm(criadoEm).build();

        FornecedorResponse response = FornecedorMapper.toResponse(fornecedor);

        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getNome()).isEqualTo("Fornecedor A");
        assertThat(response.getEndereco()).isEqualTo("Rua A");
        assertThat(response.getCelular()).isEqualTo("71999998888");
        assertThat(response.getEmail()).isEqualTo("fornecedor@teste.com");
        assertThat(response.getContato()).isEqualTo("Contato A");
        assertThat(response.getAtivo()).isFalse();
        assertThat(response.getCriadoEm()).isEqualTo(criadoEm);
    }

    @Test
    void deveAtualizarEntidadeInclusiveComEmailNuloEPreservarAtivo() {
        Fornecedor fornecedor = Fornecedor.builder().nome("Antigo").email("antigo@teste.com").ativo(false).build();
        FornecedorRequest request = requestCompleto();
        request.setEmail(null);

        FornecedorMapper.updateEntity(fornecedor, request);

        assertThat(fornecedor.getNome()).isEqualTo("Fornecedor A");
        assertThat(fornecedor.getEndereco()).isEqualTo("Rua A");
        assertThat(fornecedor.getCelular()).isEqualTo("71999998888");
        assertThat(fornecedor.getEmail()).isNull();
        assertThat(fornecedor.getContato()).isEqualTo("Contato A");
        assertThat(fornecedor.getAtivo()).isFalse();
    }

    private FornecedorRequest requestCompleto() {
        FornecedorRequest request = new FornecedorRequest();
        request.setNome("Fornecedor A");
        request.setEndereco("Rua A");
        request.setCelular("71999998888");
        request.setEmail("fornecedor@teste.com");
        request.setContato("Contato A");
        return request;
    }
}
