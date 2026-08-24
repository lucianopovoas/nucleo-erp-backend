package br.com.nucleodasreformas.nucleoerp.ordem_servico.service;

import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.service.ContextoOrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.service.OrcamentoVersaoGuard;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrdemServicoResponse;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrdemServicoStatusRequest;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrdemServicoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.entity.OrdemServico;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.repository.OrdemServicoRepository;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.entity.StatusOrcamento;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.entity.StatusOrdemServico;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.repository.StatusOrdemServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdemServicoServiceTest {

    @Mock private OrdemServicoRepository repository;
    @Mock private StatusOrdemServicoRepository statusRepository;
    @Mock private OrcamentoVersaoGuard versaoGuard;
    private OrdemServicoService service;

    @BeforeEach
    void setUp() {
        service = new OrdemServicoService(
                repository, statusRepository, versaoGuard, new OrdemServicoPolicy());
    }

    @Test
    void deveCriarSomenteDaVersaoAtualAprovadaComStatusInicialAtivo() {
        ContextoOrcamentoVersao contexto = contexto("APROVADO", true);
        StatusOrdemServico inicial = status(1L, "COMPRAR_MATERIAL", true);
        when(versaoGuard.bloquear(1L, 2L)).thenReturn(contexto);
        when(repository.existsByOrcamentoVersao_Id(2L)).thenReturn(false);
        when(statusRepository.findByCodigo("COMPRAR_MATERIAL")).thenReturn(Optional.of(inicial));
        when(repository.saveAndFlush(any(OrdemServico.class))).thenAnswer(invocation -> {
            OrdemServico ordem = invocation.getArgument(0);
            ordem.setId(10L);
            ordem.setNumero(50L);
            return ordem;
        });

        OrdemServicoResponse response = service.salvar(1L, 2L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getNumero()).isEqualTo(50L);
        assertThat(response.getStatus().getCodigo()).isEqualTo("COMPRAR_MATERIAL");
        assertThat(response.getOrigem().getVersao().getId()).isEqualTo(2L);
    }

    @Test
    void deveRejeitarOrigemQueNaoSejaVersaoAtual() {
        ContextoOrcamentoVersao contexto = contexto("APROVADO", false);
        when(versaoGuard.bloquear(1L, 2L)).thenReturn(contexto);

        assertThatThrownBy(() -> service.salvar(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("versão atual");

        verifyNoInteractions(statusRepository);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveRejeitarSegundaOrdemParaMesmaVersaoAntesDePersistir() {
        when(versaoGuard.bloquear(1L, 2L)).thenReturn(contexto("APROVADO", true));
        when(repository.existsByOrcamentoVersao_Id(2L)).thenReturn(true);

        assertThatThrownBy(() -> service.salvar(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe uma ordem de serviço");

        verifyNoInteractions(statusRepository);
    }

    @Test
    void deveRejeitarStatusInicialInativo() {
        when(versaoGuard.bloquear(1L, 2L)).thenReturn(contexto("APROVADO", true));
        when(repository.existsByOrcamentoVersao_Id(2L)).thenReturn(false);
        when(statusRepository.findByCodigo("COMPRAR_MATERIAL"))
                .thenReturn(Optional.of(status(1L, "COMPRAR_MATERIAL", false)));

        assertThatThrownBy(() -> service.salvar(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O status inicial 'COMPRAR_MATERIAL' está inativo.");
    }

    @Test
    void deveExecutarTransicaoLinearParaStatusAtivo() {
        OrdemServico ordem = ordem("COMPRAR_MATERIAL");
        OrdemServicoStatusRequest request = new OrdemServicoStatusRequest();
        request.setStatusOrdemServicoId(2L);
        when(repository.findByIdForUpdate(10L)).thenReturn(Optional.of(ordem));
        when(statusRepository.findById(2L)).thenReturn(Optional.of(status(2L, "EM_EXECUCAO", true)));
        when(repository.saveAndFlush(ordem)).thenReturn(ordem);

        OrdemServicoResponse response = service.alterarStatus(10L, request);

        assertThat(response.getStatus().getCodigo()).isEqualTo("EM_EXECUCAO");
    }

    @Test
    void deveTratarMesmoStatusComoIdempotenteSemPersistir() {
        OrdemServico ordem = ordem("EM_EXECUCAO");
        OrdemServicoStatusRequest request = new OrdemServicoStatusRequest();
        request.setStatusOrdemServicoId(ordem.getStatusOrdemServico().getId());
        when(repository.findByIdForUpdate(10L)).thenReturn(Optional.of(ordem));

        OrdemServicoResponse response = service.alterarStatus(10L, request);

        assertThat(response.getStatus().getCodigo()).isEqualTo("EM_EXECUCAO");
        verify(statusRepository, never()).findById(any());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveCongelarObservacaoEmConcluido() {
        OrdemServico ordem = ordem("CONCLUIDO");
        OrdemServicoUpdateRequest request = new OrdemServicoUpdateRequest();
        request.setObservacao("Não deve alterar");
        when(repository.findByIdForUpdate(10L)).thenReturn(Optional.of(ordem));

        assertThatThrownBy(() -> service.atualizar(10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CONCLUIDO");

        verify(repository, never()).saveAndFlush(any());
    }

    private ContextoOrcamentoVersao contexto(String codigo, boolean atual) {
        Orcamento orcamento = Orcamento.builder()
                .id(1L).numero(100L)
                .cliente(Cliente.builder().id(3L).nome("Cliente").build())
                .build();
        OrcamentoVersao versao = OrcamentoVersao.builder()
                .id(2L).numeroVersao(1).orcamento(orcamento)
                .statusOrcamento(StatusOrcamento.builder().codigo(codigo).build())
                .build();
        orcamento.setVersaoAtual(atual ? versao : OrcamentoVersao.builder().id(99L).build());
        return new ContextoOrcamentoVersao(orcamento, versao);
    }

    private OrdemServico ordem(String codigo) {
        ContextoOrcamentoVersao contexto = contexto("APROVADO", true);
        return OrdemServico.builder()
                .id(10L).numero(50L).orcamentoVersao(contexto.versao())
                .statusOrdemServico(status(1L, codigo, true))
                .observacao("Observação atual")
                .build();
    }

    private StatusOrdemServico status(Long id, String codigo, boolean ativo) {
        return StatusOrdemServico.builder()
                .id(id).codigo(codigo).nome(codigo).ativo(ativo).build();
    }
}
