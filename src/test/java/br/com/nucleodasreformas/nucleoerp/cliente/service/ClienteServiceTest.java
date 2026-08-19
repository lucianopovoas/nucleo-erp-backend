package br.com.nucleodasreformas.nucleoerp.cliente.service;

import br.com.nucleodasreformas.nucleoerp.cliente.dto.ClienteRequest;
import br.com.nucleodasreformas.nucleoerp.cliente.dto.ClienteResponse;
import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.cliente.repository.ClienteRepository;
import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository repository;

    @InjectMocks
    private ClienteService service;

    @Test
    void deveSalvarClienteValidoAtivoPorPadrao() {
        ClienteRequest request = requestSemCamposUnicos("Cliente A");
        when(repository.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente cliente = invocation.getArgument(0);
            cliente.setId(1L);
            return cliente;
        });

        ClienteResponse response = service.salvar(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNome()).isEqualTo("Cliente A");
        assertThat(response.getAtivo()).isTrue();
        verify(repository).save(any(Cliente.class));
    }

    @Test
    void deveRejeitarCpfDuplicado() {
        ClienteRequest request = requestSemCamposUnicos("Cliente A");
        request.setCpf("12345678901");
        when(repository.existsByCpf("12345678901")).thenReturn(true);

        assertThatThrownBy(() -> service.salvar(request)).isInstanceOf(BusinessException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void deveRejeitarCnpjDuplicado() {
        ClienteRequest request = requestSemCamposUnicos("Cliente A");
        request.setCnpj("12345678000199");
        when(repository.existsByCnpj("12345678000199")).thenReturn(true);

        assertThatThrownBy(() -> service.salvar(request)).isInstanceOf(BusinessException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void deveRejeitarTelefoneDuplicado() {
        ClienteRequest request = requestSemCamposUnicos("Cliente A");
        request.setTelefone("7133334444");
        when(repository.existsByTelefone("7133334444")).thenReturn(true);

        assertThatThrownBy(() -> service.salvar(request)).isInstanceOf(BusinessException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void deveRejeitarCelularDuplicado() {
        ClienteRequest request = requestSemCamposUnicos("Cliente A");
        request.setCelular("71999998888");
        when(repository.existsByCelular("71999998888")).thenReturn(true);

        assertThatThrownBy(() -> service.salvar(request)).isInstanceOf(BusinessException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void deveRejeitarEmailDuplicado() {
        ClienteRequest request = requestSemCamposUnicos("Cliente A");
        request.setEmail("cliente@teste.com");
        when(repository.existsByEmail("cliente@teste.com")).thenReturn(true);

        assertThatThrownBy(() -> service.salvar(request)).isInstanceOf(BusinessException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void deveBuscarClienteExistente() {
        when(repository.findById(1L)).thenReturn(Optional.of(cliente(1L, "Cliente A", true)));

        ClienteResponse response = service.buscarPorId(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNome()).isEqualTo("Cliente A");
    }

    @Test
    void deveFalharAoBuscarClienteInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deveListarClientesConformeComportamentoAtualInclusiveInativos() {
        when(repository.findAll()).thenReturn(List.of(
                cliente(1L, "Ativo", true), cliente(2L, "Inativo", false)));

        List<ClienteResponse> responses = service.listar();

        assertThat(responses).extracting(ClienteResponse::getAtivo).containsExactly(true, false);
    }

    @Test
    void deveAtualizarClienteExistenteSemCamposUnicos() {
        Cliente cliente = cliente(1L, "Antigo", true);
        ClienteRequest request = requestSemCamposUnicos("Atualizado");
        when(repository.findById(1L)).thenReturn(Optional.of(cliente));
        when(repository.save(cliente)).thenReturn(cliente);

        ClienteResponse response = service.atualizar(1L, request);

        assertThat(response.getNome()).isEqualTo("Atualizado");
        verify(repository).save(cliente);
    }

    @Test
    void deveRejeitarAtualizacaoQuandoCpfForInformadoComoExistente() {
        Cliente cliente = cliente(1L, "Cliente A", true);
        cliente.setCpf("12345678901");
        ClienteRequest request = requestSemCamposUnicos("Cliente A");
        request.setCpf("12345678901");
        when(repository.findById(1L)).thenReturn(Optional.of(cliente));
        when(repository.existsByCpf("12345678901")).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(1L, request)).isInstanceOf(BusinessException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void deveFalharAoAtualizarClienteInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(99L, requestSemCamposUnicos("Cliente")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deveDeletarClienteLogicamente() {
        Cliente cliente = cliente(1L, "Cliente A", true);
        when(repository.findById(1L)).thenReturn(Optional.of(cliente));

        service.deletar(1L);

        assertThat(cliente.getAtivo()).isFalse();
        verify(repository).save(cliente);
    }

    @Test
    void deveFalharAoDeletarClienteInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletar(99L)).isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).save(any());
    }

    private ClienteRequest requestSemCamposUnicos(String nome) {
        ClienteRequest request = new ClienteRequest();
        request.setNome(nome);
        return request;
    }

    private Cliente cliente(Long id, String nome, boolean ativo) {
        return Cliente.builder().id(id).nome(nome).ativo(ativo).build();
    }
}
