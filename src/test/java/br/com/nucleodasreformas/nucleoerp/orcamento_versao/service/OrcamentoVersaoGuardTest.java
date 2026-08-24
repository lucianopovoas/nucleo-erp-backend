package br.com.nucleodasreformas.nucleoerp.orcamento_versao.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.repository.OrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.repository.OrcamentoVersaoRepository;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.entity.StatusOrcamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrcamentoVersaoGuardTest {

    @Mock private OrcamentoRepository orcamentoRepository;
    @Mock private OrcamentoVersaoRepository versaoRepository;
    private OrcamentoVersaoGuard guard;

    @BeforeEach
    void setUp() {
        guard = new OrcamentoVersaoGuard(
                orcamentoRepository, versaoRepository, new OrcamentoVersaoPolicy());
    }

    @Test
    void deveBloquearPrimeiroRaizEDepoisVersaoDoMesmoOrcamento() {
        Orcamento orcamento = Orcamento.builder().id(1L).build();
        OrcamentoVersao versao = versao(2L, orcamento, "RASCUNHO");
        when(orcamentoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(orcamento));
        when(versaoRepository.findByIdAndOrcamentoIdForUpdate(2L, 1L)).thenReturn(Optional.of(versao));

        ContextoOrcamentoVersao contexto = guard.bloquear(1L, 2L);

        assertThat(contexto.orcamento()).isSameAs(orcamento);
        assertThat(contexto.versao()).isSameAs(versao);
        InOrder ordem = inOrder(orcamentoRepository, versaoRepository);
        ordem.verify(orcamentoRepository).findByIdForUpdate(1L);
        ordem.verify(versaoRepository).findByIdAndOrcamentoIdForUpdate(2L, 1L);
    }

    @Test
    void deveRejeitarVersaoHistoricaMesmoEmRascunhoParaEdicao() {
        Orcamento orcamento = Orcamento.builder().id(1L).build();
        OrcamentoVersao historica = versao(2L, orcamento, "RASCUNHO");
        orcamento.setVersaoAtual(versao(3L, orcamento, "RASCUNHO"));
        when(orcamentoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(orcamento));
        when(versaoRepository.findByIdAndOrcamentoIdForUpdate(2L, 1L))
                .thenReturn(Optional.of(historica));

        assertThatThrownBy(() -> guard.bloquearEditavel(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("histórica");
    }

    @Test
    void deveRejeitarVersaoAtualForaDeRascunhoParaEdicao() {
        Orcamento orcamento = Orcamento.builder().id(1L).build();
        OrcamentoVersao enviada = versao(2L, orcamento, "ENVIADO");
        orcamento.setVersaoAtual(enviada);
        when(orcamentoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(orcamento));
        when(versaoRepository.findByIdAndOrcamentoIdForUpdate(2L, 1L)).thenReturn(Optional.of(enviada));

        assertThatThrownBy(() -> guard.bloquearEditavel(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("RASCUNHO");
    }

    @Test
    void deveTratarVersaoDeOutroOrcamentoComoInexistente() {
        when(orcamentoRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(Orcamento.builder().id(1L).build()));
        when(versaoRepository.findByIdAndOrcamentoIdForUpdate(2L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.bloquear(1L, 2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("orçamento: 1");
    }

    private OrcamentoVersao versao(Long id, Orcamento orcamento, String codigo) {
        return OrcamentoVersao.builder()
                .id(id).orcamento(orcamento).numeroVersao(1)
                .statusOrcamento(StatusOrcamento.builder().codigo(codigo).build())
                .build();
    }
}
