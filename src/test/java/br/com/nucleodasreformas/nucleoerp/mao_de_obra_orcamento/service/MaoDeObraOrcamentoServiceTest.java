package br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto.MaoDeObraOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto.MaoDeObraOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto.MaoDeObraOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.entity.MaoDeObraOrcamento;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.repository.MaoDeObraOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.service.OrcamentoVersaoGuard;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.entity.UnidadeMaoDeObra;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.repository.UnidadeMaoDeObraRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaoDeObraOrcamentoServiceTest {

    @Mock private MaoDeObraOrcamentoRepository repository;
    @Mock private UnidadeMaoDeObraRepository unidadeRepository;
    @Mock private OrcamentoVersaoGuard versaoGuard;
    @InjectMocks private MaoDeObraOrcamentoService service;

    @Test
    void deveCalcularCustoTotalENormalizarDescricaoComSnapshotDaUnidade() {
        prepararCriacao(unidade(5L, "Hora", true));

        MaoDeObraOrcamentoResponse response = service.salvar(
                1L, 2L, request(5L, "  Instalação externa  ", "2.5000", "80.00"));

        assertThat(response.getDescricao()).isEqualTo("Instalação externa");
        assertThat(response.getUnidade()).isEqualTo("Hora");
        assertThat(response.getCustoTotal()).isEqualByComparingTo("200.00");
        assertThat(response.getCustoTotal().scale()).isEqualTo(2);
    }

    @Test
    void deveArredondarCustoTotalComHalfUp() {
        prepararCriacao(unidade(5L, "Hora", true));

        MaoDeObraOrcamentoResponse response = service.salvar(
                1L, 2L, request(5L, "Serviço", "0.3333", "0.05"));

        assertThat(response.getCustoTotal()).isEqualByComparingTo("0.02");
    }

    @Test
    void deveRejeitarUnidadeInativaNaInclusao() {
        when(versaoGuard.bloquearEditavel(1L, 2L)).thenReturn(versao());
        when(unidadeRepository.findById(5L)).thenReturn(Optional.of(unidade(5L, "Hora", false)));

        assertThatThrownBy(() -> service.salvar(
                1L, 2L, request(5L, "Serviço", "1.0000", "10.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("unidade de mão de obra inativa");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveAtualizarSnapshotAoTrocarParaUnidadeAtiva() {
        MaoDeObraOrcamento linha = linha(unidade(5L, "Hora nova", false), "Hora negociada");
        UnidadeMaoDeObra nova = unidade(6L, "Diária", true);
        MaoDeObraOrcamentoUpdateRequest request = new MaoDeObraOrcamentoUpdateRequest();
        request.setUnidadeMaoDeObraId(6L);

        when(repository.findByIdAndOrcamentoVersaoIdForUpdate(10L, 2L)).thenReturn(Optional.of(linha));
        when(unidadeRepository.findById(6L)).thenReturn(Optional.of(nova));
        when(repository.saveAndFlush(linha)).thenReturn(linha);

        MaoDeObraOrcamentoResponse response = service.atualizar(1L, 2L, 10L, request);

        assertThat(response.getUnidadeMaoDeObra().getId()).isEqualTo(6L);
        assertThat(response.getUnidade()).isEqualTo("Diária");
        assertThat(response.getDescricao()).isEqualTo("Descrição negociada");
    }

    @Test
    void devePreservarUnidadeInativaESnapshotSemTrocaEfetiva() {
        MaoDeObraOrcamento linha = linha(unidade(5L, "Catálogo renomeado", false), "Hora negociada");
        MaoDeObraOrcamentoUpdateRequest request = new MaoDeObraOrcamentoUpdateRequest();
        request.setQuantidade(new BigDecimal("3.0000"));

        when(repository.findByIdAndOrcamentoVersaoIdForUpdate(10L, 2L)).thenReturn(Optional.of(linha));
        when(repository.saveAndFlush(linha)).thenReturn(linha);

        MaoDeObraOrcamentoResponse response = service.atualizar(1L, 2L, 10L, request);

        assertThat(response.getUnidadeMaoDeObra().getId()).isEqualTo(5L);
        assertThat(response.getUnidade()).isEqualTo("Hora negociada");
        assertThat(response.getCustoTotal()).isEqualByComparingTo("30.00");
        verifyNoInteractions(unidadeRepository);
    }

    @Test
    void deveRejeitarDescricaoVaziaNaAtualizacaoExplicita() {
        MaoDeObraOrcamento linha = linha(unidade(5L, "Hora", true), "Hora");
        MaoDeObraOrcamentoUpdateRequest request = new MaoDeObraOrcamentoUpdateRequest();
        request.setDescricao("   ");
        when(repository.findByIdAndOrcamentoVersaoIdForUpdate(10L, 2L)).thenReturn(Optional.of(linha));

        assertThatThrownBy(() -> service.atualizar(1L, 2L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não pode ser nula ou vazia");
    }

    private void prepararCriacao(UnidadeMaoDeObra unidade) {
        when(versaoGuard.bloquearEditavel(1L, 2L)).thenReturn(versao());
        when(unidadeRepository.findById(unidade.getId())).thenReturn(Optional.of(unidade));
        when(repository.saveAndFlush(any(MaoDeObraOrcamento.class))).thenAnswer(invocation -> {
            MaoDeObraOrcamento linha = invocation.getArgument(0);
            linha.setId(10L);
            return linha;
        });
    }

    private MaoDeObraOrcamentoRequest request(
            Long unidadeId, String descricao, String quantidade, String custo) {
        MaoDeObraOrcamentoRequest request = new MaoDeObraOrcamentoRequest();
        request.setUnidadeMaoDeObraId(unidadeId);
        request.setDescricao(descricao);
        request.setQuantidade(new BigDecimal(quantidade));
        request.setCustoUnitario(new BigDecimal(custo));
        return request;
    }

    private MaoDeObraOrcamento linha(UnidadeMaoDeObra unidade, String snapshot) {
        return MaoDeObraOrcamento.builder()
                .id(10L).orcamentoVersao(versao()).unidadeMaoDeObra(unidade)
                .descricao("Descrição negociada").unidade(snapshot)
                .quantidade(new BigDecimal("2.0000"))
                .custoUnitario(new BigDecimal("10.00"))
                .custoTotal(new BigDecimal("20.00"))
                .build();
    }

    private UnidadeMaoDeObra unidade(Long id, String nome, boolean ativo) {
        return UnidadeMaoDeObra.builder().id(id).nome(nome).ativo(ativo).build();
    }

    private OrcamentoVersao versao() {
        return OrcamentoVersao.builder().id(2L).build();
    }
}
