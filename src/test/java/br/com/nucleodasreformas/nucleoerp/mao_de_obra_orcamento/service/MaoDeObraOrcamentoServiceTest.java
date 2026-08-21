package br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto.MaoDeObraOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto.MaoDeObraOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto.MaoDeObraOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.entity.MaoDeObraOrcamento;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.repository.MaoDeObraOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.repository.OrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.entity.UnidadeMaoDeObra;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.repository.UnidadeMaoDeObraRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaoDeObraOrcamentoServiceTest {

    @Mock
    private MaoDeObraOrcamentoRepository repository;

    @Mock
    private OrcamentoRepository orcamentoRepository;

    @Mock
    private UnidadeMaoDeObraRepository unidadeMaoDeObraRepository;

    @InjectMocks
    private MaoDeObraOrcamentoService service;

    @Test
    void deveCriarComDescricaoAparadaSnapshotDaUnidadeECustoCalculado() {
        prepararCriacao(unidade(5L, "Diária", true));

        MaoDeObraOrcamentoResponse response = service.salvar(
                10L, request(5L, "  Instalação de toldo  ", "2.0000", "250.00"));

        assertThat(response.getUnidadeMaoDeObra().getId()).isEqualTo(5L);
        assertThat(response.getDescricao()).isEqualTo("Instalação de toldo");
        assertThat(response.getUnidade()).isEqualTo("Diária");
        assertThat(response.getQuantidade()).isEqualByComparingTo("2.0000");
        assertThat(response.getCustoUnitario()).isEqualByComparingTo("250.00");
        assertThat(response.getCustoTotal()).isEqualByComparingTo("500.00");
    }

    @Test
    void deveArredondarCustoTotalComHalfUp() {
        prepararCriacao(unidade(5L, "Hora", true));

        MaoDeObraOrcamentoResponse response = service.salvar(
                10L, request(5L, "Acabamento", "1.0050", "1.00"));

        assertThat(response.getCustoTotal()).isEqualByComparingTo("1.01");
        assertThat(response.getCustoTotal().scale()).isEqualTo(2);
    }

    @Test
    void devePermitirCustoZero() {
        prepararCriacao(unidade(5L, "Serviço", true));

        MaoDeObraOrcamentoResponse response = service.salvar(
                10L, request(5L, "Cortesia", "3.5000", "0.00"));

        assertThat(response.getCustoTotal()).isEqualByComparingTo("0.00");
    }

    @Test
    void deveFalharComOrcamentoInexistente() {
        when(orcamentoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.salvar(
                99L, request(5L, "Instalação", "1", "10")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Orçamento não encontrado. Id: 99");

        verifyNoInteractions(unidadeMaoDeObraRepository, repository);
    }

    @Test
    void deveFalharComUnidadeInexistente() {
        when(orcamentoRepository.findById(10L)).thenReturn(Optional.of(orcamento(10L)));
        when(unidadeMaoDeObraRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.salvar(
                10L, request(99L, "Instalação", "1", "10")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Unidade de mão de obra não encontrada. Id: 99");

        verifyNoInteractions(repository);
    }

    @Test
    void deveFalharComUnidadeInativaNaInclusao() {
        when(orcamentoRepository.findById(10L)).thenReturn(Optional.of(orcamento(10L)));
        when(unidadeMaoDeObraRepository.findById(5L))
                .thenReturn(Optional.of(unidade(5L, "Diária", false)));

        assertThatThrownBy(() -> service.salvar(
                10L, request(5L, "Instalação", "1", "10")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Não é possível vincular uma unidade de mão de obra inativa ao orçamento.");

        verifyNoInteractions(repository);
    }

    @Test
    void deveRejeitarDescricaoNulaOuVaziaNaCriacao() {
        prepararReferenciasParaCriacao();

        assertThatThrownBy(() -> service.salvar(
                10L, request(5L, null, "1", "10")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A descrição informada não pode ser nula ou vazia.");
        assertThatThrownBy(() -> service.salvar(
                10L, request(5L, "   ", "1", "10")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A descrição informada não pode ser nula ou vazia.");
    }

    @Test
    void deveRejeitarQuantidadeZeroOuNegativa() {
        prepararReferenciasParaCriacao();

        assertThatThrownBy(() -> service.salvar(
                10L, request(5L, "Instalação", "0", "10")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A quantidade deve ser maior que zero.");
        assertThatThrownBy(() -> service.salvar(
                10L, request(5L, "Instalação", "-1", "10")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A quantidade deve ser maior que zero.");
    }

    @Test
    void deveRejeitarCustoUnitarioNegativo() {
        prepararReferenciasParaCriacao();

        assertThatThrownBy(() -> service.salvar(
                10L, request(5L, "Instalação", "1", "-0.01")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O custo unitário não pode ser negativo.");
    }

    @Test
    void devePermitirMesmaUnidadeRepetidaNoOrcamento() {
        prepararReferenciasParaCriacao();
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.salvar(10L, request(5L, "Instalação principal", "1", "10"));
        service.salvar(10L, request(5L, "Instalação complementar", "2", "10"));

        verify(repository, times(2)).saveAndFlush(any());
    }

    @Test
    void deveBuscarSomenteLinhaPertencenteAoOrcamentoEPreservarSnapshot() {
        when(repository.findByIdAndOrcamento_Id(20L, 10L))
                .thenReturn(Optional.of(registro(
                        20L, 10L, unidade(5L, "Nome atual", false))));

        MaoDeObraOrcamentoResponse response = service.buscarPorId(10L, 20L);

        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getUnidadeMaoDeObra().getNome()).isEqualTo("Nome atual");
        assertThat(response.getUnidade()).isEqualTo("Diária");
        verify(repository).findByIdAndOrcamento_Id(20L, 10L);
    }

    @Test
    void deveFalharQuandoLinhaNaoPertencerAoOrcamento() {
        when(repository.findByIdAndOrcamento_Id(20L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L, 20L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Mão de obra do orçamento não encontrada. Id: 20, orçamento: 99");
    }

    @Test
    void deveListarLinhasDoOrcamentoExistente() {
        when(orcamentoRepository.existsById(10L)).thenReturn(true);
        when(repository.findByOrcamento_IdOrderByIdAsc(10L)).thenReturn(List.of(
                registro(20L, 10L, unidade(5L, "Diária", true)),
                registro(21L, 10L, unidade(6L, "Hora", false))));

        List<MaoDeObraOrcamentoResponse> responses = service.listar(10L);

        assertThat(responses).extracting(MaoDeObraOrcamentoResponse::getId)
                .containsExactly(20L, 21L);
    }

    @Test
    void deveFalharAoListarOrcamentoInexistente() {
        when(orcamentoRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.listar(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Orçamento não encontrado. Id: 99");

        verifyNoInteractions(repository);
    }

    @Test
    void deveAtualizarCamposERecalcularTotal() {
        MaoDeObraOrcamento registro = registro(
                20L, 10L, unidade(5L, "Diária", true));
        MaoDeObraOrcamentoUpdateRequest request = new MaoDeObraOrcamentoUpdateRequest();
        request.setDescricao("  Instalação principal  ");
        request.setQuantidade(new BigDecimal("3.0000"));
        request.setCustoUnitario(new BigDecimal("125.00"));
        prepararAtualizacao(registro);

        MaoDeObraOrcamentoResponse response = service.atualizar(10L, 20L, request);

        assertThat(response.getDescricao()).isEqualTo("Instalação principal");
        assertThat(response.getQuantidade()).isEqualByComparingTo("3.0000");
        assertThat(response.getCustoUnitario()).isEqualByComparingTo("125.00");
        assertThat(response.getCustoTotal()).isEqualByComparingTo("375.00");
    }

    @Test
    void devePreservarCamposQuandoNullForInformadoNoPut() {
        MaoDeObraOrcamento registro = registro(
                20L, 10L, unidade(5L, "Nome atual", false));
        MaoDeObraOrcamentoUpdateRequest request = new MaoDeObraOrcamentoUpdateRequest();
        request.setUnidadeMaoDeObraId(null);
        request.setQuantidade(null);
        request.setCustoUnitario(null);
        prepararAtualizacao(registro);

        MaoDeObraOrcamentoResponse response = service.atualizar(10L, 20L, request);

        assertThat(response.getDescricao()).isEqualTo("Instalação");
        assertThat(response.getUnidade()).isEqualTo("Diária");
        assertThat(response.getQuantidade()).isEqualByComparingTo("2.0000");
        assertThat(response.getCustoUnitario()).isEqualByComparingTo("50.00");
        assertThat(response.getCustoTotal()).isEqualByComparingTo("100.00");
        verifyNoInteractions(unidadeMaoDeObraRepository);
    }

    @Test
    void devePreservarSnapshotAoReinformarMesmaUnidadeInativa() {
        MaoDeObraOrcamento registro = registro(
                20L, 10L, unidade(5L, "Nome atual", false));
        MaoDeObraOrcamentoUpdateRequest request = new MaoDeObraOrcamentoUpdateRequest();
        request.setUnidadeMaoDeObraId(5L);
        prepararAtualizacao(registro);

        MaoDeObraOrcamentoResponse response = service.atualizar(10L, 20L, request);

        assertThat(response.getUnidade()).isEqualTo("Diária");
        assertThat(response.getUnidadeMaoDeObra().getId()).isEqualTo(5L);
        verifyNoInteractions(unidadeMaoDeObraRepository);
    }

    @Test
    void deveTrocarParaUnidadeAtivaEAtualizarSomenteSnapshotDaUnidade() {
        MaoDeObraOrcamento registro = registro(
                20L, 10L, unidade(5L, "Diária", false));
        MaoDeObraOrcamentoUpdateRequest request = new MaoDeObraOrcamentoUpdateRequest();
        request.setUnidadeMaoDeObraId(6L);
        prepararAtualizacao(registro);
        when(unidadeMaoDeObraRepository.findById(6L))
                .thenReturn(Optional.of(unidade(6L, "Hora", true)));

        MaoDeObraOrcamentoResponse response = service.atualizar(10L, 20L, request);

        assertThat(response.getUnidadeMaoDeObra().getId()).isEqualTo(6L);
        assertThat(response.getUnidade()).isEqualTo("Hora");
        assertThat(response.getDescricao()).isEqualTo("Instalação");
    }

    @Test
    void deveRejeitarTrocaParaUnidadeInativa() {
        MaoDeObraOrcamento registro = registro(
                20L, 10L, unidade(5L, "Diária", true));
        MaoDeObraOrcamentoUpdateRequest request = new MaoDeObraOrcamentoUpdateRequest();
        request.setUnidadeMaoDeObraId(6L);
        when(repository.findByIdAndOrcamento_Id(20L, 10L)).thenReturn(Optional.of(registro));
        when(unidadeMaoDeObraRepository.findById(6L))
                .thenReturn(Optional.of(unidade(6L, "Hora", false)));

        assertThatThrownBy(() -> service.atualizar(10L, 20L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Não é possível vincular uma unidade de mão de obra inativa ao orçamento.");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveRejeitarTrocaParaUnidadeInexistente() {
        MaoDeObraOrcamento registro = registro(
                20L, 10L, unidade(5L, "Diária", true));
        MaoDeObraOrcamentoUpdateRequest request = new MaoDeObraOrcamentoUpdateRequest();
        request.setUnidadeMaoDeObraId(99L);
        when(repository.findByIdAndOrcamento_Id(20L, 10L)).thenReturn(Optional.of(registro));
        when(unidadeMaoDeObraRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(10L, 20L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Unidade de mão de obra não encontrada. Id: 99");
    }

    @Test
    void deveRejeitarDescricaoExplicitaNulaOuVaziaNoPut() {
        MaoDeObraOrcamento registro = registro(
                20L, 10L, unidade(5L, "Diária", true));
        when(repository.findByIdAndOrcamento_Id(20L, 10L)).thenReturn(Optional.of(registro));
        MaoDeObraOrcamentoUpdateRequest nula = new MaoDeObraOrcamentoUpdateRequest();
        nula.setDescricao(null);

        assertThatThrownBy(() -> service.atualizar(10L, 20L, nula))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A descrição informada não pode ser nula ou vazia.");

        MaoDeObraOrcamentoUpdateRequest vazia = new MaoDeObraOrcamentoUpdateRequest();
        vazia.setDescricao("   ");
        assertThatThrownBy(() -> service.atualizar(10L, 20L, vazia))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A descrição informada não pode ser nula ou vazia.");
    }

    @Test
    void deveExcluirFisicamenteSomenteLinhaPertencenteAoOrcamento() {
        MaoDeObraOrcamento registro = registro(
                20L, 10L, unidade(5L, "Diária", true));
        when(repository.findByIdAndOrcamento_Id(20L, 10L)).thenReturn(Optional.of(registro));

        service.deletar(10L, 20L);

        verify(repository).delete(registro);
        verify(orcamentoRepository, never()).delete(any());
        verify(unidadeMaoDeObraRepository, never()).delete(any());
    }

    private void prepararCriacao(UnidadeMaoDeObra unidadeMaoDeObra) {
        when(orcamentoRepository.findById(10L)).thenReturn(Optional.of(orcamento(10L)));
        when(unidadeMaoDeObraRepository.findById(unidadeMaoDeObra.getId()))
                .thenReturn(Optional.of(unidadeMaoDeObra));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> {
            MaoDeObraOrcamento registro = invocation.getArgument(0);
            registro.setId(20L);
            registro.setCriadoEm(LocalDateTime.of(2026, 8, 21, 12, 0));
            return registro;
        });
    }

    private void prepararReferenciasParaCriacao() {
        when(orcamentoRepository.findById(10L)).thenReturn(Optional.of(orcamento(10L)));
        when(unidadeMaoDeObraRepository.findById(5L))
                .thenReturn(Optional.of(unidade(5L, "Diária", true)));
    }

    private void prepararAtualizacao(MaoDeObraOrcamento registro) {
        when(repository.findByIdAndOrcamento_Id(20L, 10L)).thenReturn(Optional.of(registro));
        when(repository.saveAndFlush(registro)).thenReturn(registro);
    }

    private MaoDeObraOrcamentoRequest request(
            Long unidadeId,
            String descricao,
            String quantidade,
            String custoUnitario) {
        MaoDeObraOrcamentoRequest request = new MaoDeObraOrcamentoRequest();
        request.setUnidadeMaoDeObraId(unidadeId);
        request.setDescricao(descricao);
        request.setQuantidade(new BigDecimal(quantidade));
        request.setCustoUnitario(new BigDecimal(custoUnitario));
        return request;
    }

    private MaoDeObraOrcamento registro(
            Long id,
            Long orcamentoId,
            UnidadeMaoDeObra unidadeMaoDeObra) {
        return MaoDeObraOrcamento.builder()
                .id(id)
                .orcamento(orcamento(orcamentoId))
                .unidadeMaoDeObra(unidadeMaoDeObra)
                .descricao("Instalação")
                .unidade("Diária")
                .quantidade(new BigDecimal("2.0000"))
                .custoUnitario(new BigDecimal("50.00"))
                .custoTotal(new BigDecimal("100.00"))
                .criadoEm(LocalDateTime.of(2026, 8, 21, 12, 0))
                .build();
    }

    private Orcamento orcamento(Long id) {
        return Orcamento.builder().id(id).numero(1000L + id).build();
    }

    private UnidadeMaoDeObra unidade(Long id, String nome, boolean ativo) {
        return UnidadeMaoDeObra.builder().id(id).nome(nome).ativo(ativo).build();
    }
}
