package br.com.nucleodasreformas.nucleoerp.orcamento.service;

import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.cliente.repository.ClienteRepository;
import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.repository.OrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.repository.OrcamentoVersaoRepository;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.entity.StatusOrcamento;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.repository.StatusOrcamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrcamentoServiceTest {

    @Mock private OrcamentoRepository repository;
    @Mock private OrcamentoVersaoRepository versaoRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private StatusOrcamentoRepository statusRepository;
    @InjectMocks private OrcamentoService service;

    @Test
    void deveCriarRaizV1RascunhoEVersaoAtualNaOrdemEsperada() {
        Cliente cliente = cliente(10L, true);
        StatusOrcamento rascunho = status(1L, "RASCUNHO", true);
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente));
        when(statusRepository.findByCodigo("RASCUNHO")).thenReturn(Optional.of(rascunho));
        when(repository.saveAndFlush(any(Orcamento.class))).thenAnswer(invocation -> {
            Orcamento orcamento = invocation.getArgument(0);
            orcamento.setId(20L);
            orcamento.setNumero(100L);
            return orcamento;
        });
        when(versaoRepository.saveAndFlush(any(OrcamentoVersao.class))).thenAnswer(invocation -> {
            OrcamentoVersao versao = invocation.getArgument(0);
            versao.setId(30L);
            return versao;
        });

        OrcamentoResponse response = service.salvar(request(10L, "Proposta inicial"));

        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getNumero()).isEqualTo(100L);
        assertThat(response.getCliente().getId()).isEqualTo(10L);
        assertThat(response.getVersaoAtual().getId()).isEqualTo(30L);
        assertThat(response.getVersaoAtual().getNumeroVersao()).isEqualTo(1);
        assertThat(response.getVersaoAtual().getStatus().getCodigo()).isEqualTo("RASCUNHO");

        InOrder ordem = inOrder(repository, versaoRepository);
        ordem.verify(repository).saveAndFlush(any(Orcamento.class));
        ordem.verify(versaoRepository).saveAndFlush(any(OrcamentoVersao.class));
        ordem.verify(repository).saveAndFlush(any(Orcamento.class));
    }

    @Test
    void deveRejeitarClienteInativoAntesDeCriarRaiz() {
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente(10L, false)));

        assertThatThrownBy(() -> service.salvar(request(10L, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cliente inativo");

        verifyNoInteractions(statusRepository, versaoRepository);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveRejeitarStatusInicialInativo() {
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente(10L, true)));
        when(statusRepository.findByCodigo("RASCUNHO"))
                .thenReturn(Optional.of(status(1L, "RASCUNHO", false)));

        assertThatThrownBy(() -> service.salvar(request(10L, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O status inicial 'RASCUNHO' está inativo.");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveTrocarClienteAtivoSomenteNaUnicaV1AtualEmRascunho() {
        Orcamento orcamento = orcamento(20L, cliente(10L, true), 1, "RASCUNHO");
        Cliente novo = cliente(11L, true);
        when(repository.findByIdForUpdate(20L)).thenReturn(Optional.of(orcamento));
        when(versaoRepository.findByIdAndOrcamentoIdForUpdate(30L, 20L))
                .thenReturn(Optional.of(orcamento.getVersaoAtual()));
        when(versaoRepository.countByOrcamento_Id(20L)).thenReturn(1L);
        when(clienteRepository.findById(11L)).thenReturn(Optional.of(novo));
        when(repository.saveAndFlush(orcamento)).thenReturn(orcamento);

        OrcamentoResponse response = service.atualizar(20L, update(11L));

        assertThat(response.getCliente().getId()).isEqualTo(11L);
        verify(repository).findByIdForUpdate(20L);
    }

    @Test
    void deveRejeitarTrocaDeClienteDepoisQueV1SairDeRascunho() {
        Orcamento orcamento = orcamento(20L, cliente(10L, true), 1, "ENVIADO");
        when(repository.findByIdForUpdate(20L)).thenReturn(Optional.of(orcamento));
        when(versaoRepository.findByIdAndOrcamentoIdForUpdate(30L, 20L))
                .thenReturn(Optional.of(orcamento.getVersaoAtual()));

        assertThatThrownBy(() -> service.atualizar(20L, update(11L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("V1 deixa o estado RASCUNHO");

        verifyNoInteractions(clienteRepository);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveTratarReenvioDoMesmoClienteComoIdempotente() {
        Orcamento orcamento = orcamento(20L, cliente(10L, false), 2, "ENVIADO");
        when(repository.findByIdForUpdate(20L)).thenReturn(Optional.of(orcamento));

        OrcamentoResponse response = service.atualizar(20L, update(10L));

        assertThat(response.getCliente().getId()).isEqualTo(10L);
        verifyNoInteractions(clienteRepository, versaoRepository);
        verify(repository, never()).saveAndFlush(any());
    }

    private OrcamentoRequest request(Long clienteId, String observacao) {
        OrcamentoRequest request = new OrcamentoRequest();
        request.setClienteId(clienteId);
        request.setObservacao(observacao);
        return request;
    }

    private OrcamentoUpdateRequest update(Long clienteId) {
        OrcamentoUpdateRequest request = new OrcamentoUpdateRequest();
        request.setClienteId(clienteId);
        return request;
    }

    private Orcamento orcamento(Long id, Cliente cliente, int numeroVersao, String codigo) {
        Orcamento orcamento = Orcamento.builder().id(id).numero(100L).cliente(cliente).build();
        OrcamentoVersao versao = OrcamentoVersao.builder()
                .id(30L).orcamento(orcamento).numeroVersao(numeroVersao)
                .statusOrcamento(status(1L, codigo, true)).build();
        orcamento.setVersaoAtual(versao);
        return orcamento;
    }

    private Cliente cliente(Long id, boolean ativo) {
        return Cliente.builder().id(id).nome("Cliente " + id).ativo(ativo).build();
    }

    private StatusOrcamento status(Long id, String codigo, boolean ativo) {
        return StatusOrcamento.builder().id(id).codigo(codigo).nome(codigo).ativo(ativo).build();
    }
}
