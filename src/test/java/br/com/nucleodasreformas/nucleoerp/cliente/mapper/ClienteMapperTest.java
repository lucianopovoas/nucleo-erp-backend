package br.com.nucleodasreformas.nucleoerp.cliente.mapper;

import br.com.nucleodasreformas.nucleoerp.cliente.dto.ClienteRequest;
import br.com.nucleodasreformas.nucleoerp.cliente.dto.ClienteResponse;
import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ClienteMapperTest {

    @Test
    void deveConverterRequestParaEntidadeComTodosOsCampos() {
        ClienteRequest request = requestCompleto();
        request.setAtivo(false);

        Cliente cliente = ClienteMapper.toEntity(request);

        assertThat(cliente.getNome()).isEqualTo("Cliente A");
        assertThat(cliente.getCpf()).isEqualTo("12345678901");
        assertThat(cliente.getCnpj()).isEqualTo("12345678000199");
        assertThat(cliente.getTelefone()).isEqualTo("7133334444");
        assertThat(cliente.getCelular()).isEqualTo("71999998888");
        assertThat(cliente.getEmail()).isEqualTo("cliente@teste.com");
        assertThat(cliente.getContato()).isEqualTo("Contato A");
        assertThat(cliente.getEndereco()).isEqualTo("Rua A");
        assertThat(cliente.getAtivo()).isFalse();
    }

    @Test
    void deveAplicarAtivoComoVerdadeiroQuandoRequestNaoInformarValor() {
        ClienteRequest request = requestCompleto();

        Cliente cliente = ClienteMapper.toEntity(request);

        assertThat(cliente.getAtivo()).isTrue();
    }

    @Test
    void deveConverterEntidadeParaResponseCompleto() {
        LocalDateTime criadoEm = LocalDateTime.of(2026, 8, 19, 10, 30);
        Cliente cliente = Cliente.builder()
                .id(1L).nome("Cliente A").cpf("12345678901").cnpj("12345678000199")
                .telefone("7133334444").celular("71999998888").email("cliente@teste.com")
                .contato("Contato A").endereco("Rua A").ativo(false).criadoEm(criadoEm).build();

        ClienteResponse response = ClienteMapper.toResponse(cliente);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNome()).isEqualTo("Cliente A");
        assertThat(response.getCpf()).isEqualTo("12345678901");
        assertThat(response.getCnpj()).isEqualTo("12345678000199");
        assertThat(response.getTelefone()).isEqualTo("7133334444");
        assertThat(response.getCelular()).isEqualTo("71999998888");
        assertThat(response.getEmail()).isEqualTo("cliente@teste.com");
        assertThat(response.getContato()).isEqualTo("Contato A");
        assertThat(response.getEndereco()).isEqualTo("Rua A");
        assertThat(response.getAtivo()).isFalse();
        assertThat(response.getCriadoEm()).isEqualTo(criadoEm);
    }

    @Test
    void deveAtualizarEntidadeEPreservarAtivoQuandoNaoInformado() {
        Cliente cliente = Cliente.builder().nome("Antigo").ativo(false).build();
        ClienteRequest request = requestCompleto();

        ClienteMapper.updateEntity(cliente, request);

        assertThat(cliente.getNome()).isEqualTo("Cliente A");
        assertThat(cliente.getCpf()).isEqualTo("12345678901");
        assertThat(cliente.getCnpj()).isEqualTo("12345678000199");
        assertThat(cliente.getTelefone()).isEqualTo("7133334444");
        assertThat(cliente.getCelular()).isEqualTo("71999998888");
        assertThat(cliente.getEmail()).isEqualTo("cliente@teste.com");
        assertThat(cliente.getContato()).isEqualTo("Contato A");
        assertThat(cliente.getEndereco()).isEqualTo("Rua A");
        assertThat(cliente.getAtivo()).isFalse();
    }

    private ClienteRequest requestCompleto() {
        ClienteRequest request = new ClienteRequest();
        request.setNome("Cliente A");
        request.setCpf("12345678901");
        request.setCnpj("12345678000199");
        request.setTelefone("7133334444");
        request.setCelular("71999998888");
        request.setEmail("cliente@teste.com");
        request.setContato("Contato A");
        request.setEndereco("Rua A");
        return request;
    }
}
