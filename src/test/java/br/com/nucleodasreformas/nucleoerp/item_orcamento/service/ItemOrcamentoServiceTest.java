package br.com.nucleodasreformas.nucleoerp.item_orcamento.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.entity.ItemOrcamento;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.repository.ItemOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.repository.OrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.servico.entity.Servico;
import br.com.nucleodasreformas.nucleoerp.servico.repository.ServicoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemOrcamentoServiceTest {

    @Mock
    private ItemOrcamentoRepository repository;

    @Mock
    private OrcamentoRepository orcamentoRepository;

    @Mock
    private ServicoRepository servicoRepository;

    @InjectMocks
    private ItemOrcamentoService service;

    @Test
    void deveCriarItemComSnapshotCalculoEDescontoZero() {
        Orcamento orcamento = orcamento(10L);
        Servico servico = servico(5L, "Instalação", true);
        when(orcamentoRepository.findById(10L)).thenReturn(Optional.of(orcamento));
        when(servicoRepository.findById(5L)).thenReturn(Optional.of(servico));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> {
            ItemOrcamento item = invocation.getArgument(0);
            item.setId(20L);
            item.setCriadoEm(LocalDateTime.of(2026, 8, 20, 12, 0));
            return item;
        });

        ItemOrcamentoResponse response = service.salvar(
                10L, request(5L, "2.5000", "150.00", null));

        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getDescricao()).isEqualTo("Instalação");
        assertThat(response.getQuantidade()).isEqualByComparingTo("2.5000");
        assertThat(response.getDesconto()).isEqualByComparingTo("0.00");
        assertThat(response.getValorTotal()).isEqualByComparingTo("375.00");
    }

    @Test
    void deveArredondarTotalParaDuasCasasComHalfUp() {
        prepararCriacao(servico(5L, "Serviço decimal", true));

        ItemOrcamentoResponse response = service.salvar(
                10L, request(5L, "1.0050", "1.00", "0.00"));

        assertThat(response.getValorTotal()).isEqualByComparingTo("1.01");
        assertThat(response.getValorTotal().scale()).isEqualTo(2);
    }

    @Test
    void deveFalharComOrcamentoInexistente() {
        when(orcamentoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.salvar(99L, request(5L, "1", "10", null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Orçamento não encontrado. Id: 99");

        verifyNoInteractions(servicoRepository, repository);
    }

    @Test
    void deveFalharComServicoInexistente() {
        when(orcamentoRepository.findById(10L)).thenReturn(Optional.of(orcamento(10L)));
        when(servicoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.salvar(10L, request(99L, "1", "10", null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Serviço não encontrado. Id: 99");

        verifyNoInteractions(repository);
    }

    @Test
    void deveFalharComServicoInativo() {
        when(orcamentoRepository.findById(10L)).thenReturn(Optional.of(orcamento(10L)));
        when(servicoRepository.findById(5L))
                .thenReturn(Optional.of(servico(5L, "Inativo", false)));

        assertThatThrownBy(() -> service.salvar(10L, request(5L, "1", "10", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Não é possível vincular um item de orçamento a um serviço inativo.");

        verifyNoInteractions(repository);
    }

    @Test
    void deveRejeitarQuantidadeZeroOuNegativa() {
        prepararReferenciasParaCriacao();

        assertThatThrownBy(() -> service.salvar(10L, request(5L, "0", "10", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A quantidade deve ser maior que zero.");
        assertThatThrownBy(() -> service.salvar(10L, request(5L, "-1", "10", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A quantidade deve ser maior que zero.");
    }

    @Test
    void deveRejeitarValorUnitarioNegativo() {
        prepararReferenciasParaCriacao();

        assertThatThrownBy(() -> service.salvar(10L, request(5L, "1", "-0.01", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O valor unitário não pode ser negativo.");
    }

    @Test
    void deveRejeitarDescontoNegativo() {
        prepararReferenciasParaCriacao();

        assertThatThrownBy(() -> service.salvar(10L, request(5L, "1", "10", "-0.01")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O desconto não pode ser negativo.");
    }

    @Test
    void deveRejeitarDescontoMaiorQueSubtotal() {
        prepararReferenciasParaCriacao();

        assertThatThrownBy(() -> service.salvar(10L, request(5L, "2", "10", "20.01")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O desconto não pode ser maior que o subtotal do item.");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void devePermitirMesmoServicoRepetidoNoOrcamento() {
        prepararReferenciasParaCriacao();
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.salvar(10L, request(5L, "1", "10", null));
        service.salvar(10L, request(5L, "2", "10", null));

        verify(repository, org.mockito.Mockito.times(2)).saveAndFlush(any());
    }

    @Test
    void deveBuscarItemSomenteDentroDoOrcamentoInformado() {
        when(repository.findByIdAndOrcamento_Id(20L, 10L))
                .thenReturn(Optional.of(item(20L, 10L, servico(5L, "Instalação", false))));

        ItemOrcamentoResponse response = service.buscarPorId(10L, 20L);

        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getServico().getId()).isEqualTo(5L);
        verify(repository).findByIdAndOrcamento_Id(20L, 10L);
    }

    @Test
    void deveFalharQuandoItemNaoPertencerAoOrcamento() {
        when(repository.findByIdAndOrcamento_Id(20L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L, 20L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Item de orçamento não encontrado. Id: 20, orçamento: 99");
    }

    @Test
    void deveListarItensApenasDoOrcamentoExistente() {
        when(orcamentoRepository.existsById(10L)).thenReturn(true);
        when(repository.findByOrcamento_IdOrderByIdAsc(10L)).thenReturn(List.of(
                item(20L, 10L, servico(5L, "A", true)),
                item(21L, 10L, servico(6L, "B", false))));

        List<ItemOrcamentoResponse> responses = service.listar(10L);

        assertThat(responses).extracting(ItemOrcamentoResponse::getId)
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
    void deveAtualizarValoresERecalcularTotal() {
        ItemOrcamento item = item(20L, 10L, servico(5L, "Instalação", true));
        ItemOrcamentoUpdateRequest request = new ItemOrcamentoUpdateRequest();
        request.setQuantidade(new BigDecimal("3.0000"));
        request.setValorUnitario(new BigDecimal("25.00"));
        request.setDesconto(new BigDecimal("5.00"));
        prepararAtualizacao(item);

        ItemOrcamentoResponse response = service.atualizar(10L, 20L, request);

        assertThat(response.getQuantidade()).isEqualByComparingTo("3.0000");
        assertThat(response.getValorUnitario()).isEqualByComparingTo("25.00");
        assertThat(response.getDesconto()).isEqualByComparingTo("5.00");
        assertThat(response.getValorTotal()).isEqualByComparingTo("70.00");
    }

    @Test
    void deveTrocarServicoEUsarNovoNomeQuandoDescricaoForOmitida() {
        ItemOrcamento item = item(20L, 10L, servico(5L, "Anterior", false));
        Servico novoServico = servico(6L, "Novo serviço", true);
        ItemOrcamentoUpdateRequest request = new ItemOrcamentoUpdateRequest();
        request.setServicoId(6L);
        prepararAtualizacao(item);
        when(servicoRepository.findById(6L)).thenReturn(Optional.of(novoServico));

        ItemOrcamentoResponse response = service.atualizar(10L, 20L, request);

        assertThat(response.getServico().getId()).isEqualTo(6L);
        assertThat(response.getDescricao()).isEqualTo("Novo serviço");
    }

    @Test
    void deveTrocarServicoEUsarDescricaoExplicitaAparada() {
        ItemOrcamento item = item(20L, 10L, servico(5L, "Anterior", true));
        ItemOrcamentoUpdateRequest request = new ItemOrcamentoUpdateRequest();
        request.setServicoId(6L);
        request.setDescricao("  Aplicação na área frontal  ");
        prepararAtualizacao(item);
        when(servicoRepository.findById(6L))
                .thenReturn(Optional.of(servico(6L, "Novo serviço", true)));

        ItemOrcamentoResponse response = service.atualizar(10L, 20L, request);

        assertThat(response.getServico().getId()).isEqualTo(6L);
        assertThat(response.getDescricao()).isEqualTo("Aplicação na área frontal");
    }

    @Test
    void devePreservarDescricaoAoReinformarMesmoServico() {
        Servico servicoInativo = servico(5L, "Nome atual", false);
        ItemOrcamento item = item(20L, 10L, servicoInativo);
        item.setDescricao("Snapshot preservado");
        ItemOrcamentoUpdateRequest request = new ItemOrcamentoUpdateRequest();
        request.setServicoId(5L);
        prepararAtualizacao(item);

        ItemOrcamentoResponse response = service.atualizar(10L, 20L, request);

        assertThat(response.getDescricao()).isEqualTo("Snapshot preservado");
        assertThat(response.getServico().getId()).isEqualTo(5L);
        verifyNoInteractions(servicoRepository);
    }

    @Test
    void devePreservarCamposQuandoNullForInformadoNoPut() {
        ItemOrcamento item = item(20L, 10L, servico(5L, "Instalação", true));
        ItemOrcamentoUpdateRequest request = new ItemOrcamentoUpdateRequest();
        request.setServicoId(null);
        request.setQuantidade(null);
        request.setValorUnitario(null);
        request.setDesconto(null);
        prepararAtualizacao(item);

        ItemOrcamentoResponse response = service.atualizar(10L, 20L, request);

        assertThat(response.getDescricao()).isEqualTo("Snapshot");
        assertThat(response.getQuantidade()).isEqualByComparingTo("2.0000");
        assertThat(response.getValorUnitario()).isEqualByComparingTo("50.00");
        assertThat(response.getDesconto()).isEqualByComparingTo("10.00");
        assertThat(response.getValorTotal()).isEqualByComparingTo("90.00");
        verifyNoInteractions(servicoRepository);
    }

    @Test
    void deveRejeitarDescricaoExplicitaNulaOuVazia() {
        ItemOrcamento item = item(20L, 10L, servico(5L, "Instalação", true));
        when(repository.findByIdAndOrcamento_Id(20L, 10L)).thenReturn(Optional.of(item));
        ItemOrcamentoUpdateRequest nula = new ItemOrcamentoUpdateRequest();
        nula.setDescricao(null);

        assertThatThrownBy(() -> service.atualizar(10L, 20L, nula))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A descrição informada não pode ser nula ou vazia.");

        ItemOrcamentoUpdateRequest vazia = new ItemOrcamentoUpdateRequest();
        vazia.setDescricao("   ");
        assertThatThrownBy(() -> service.atualizar(10L, 20L, vazia))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A descrição informada não pode ser nula ou vazia.");
    }

    @Test
    void deveRejeitarTrocaParaServicoInativo() {
        ItemOrcamento item = item(20L, 10L, servico(5L, "Atual", true));
        ItemOrcamentoUpdateRequest request = new ItemOrcamentoUpdateRequest();
        request.setServicoId(6L);
        when(repository.findByIdAndOrcamento_Id(20L, 10L)).thenReturn(Optional.of(item));
        when(servicoRepository.findById(6L))
                .thenReturn(Optional.of(servico(6L, "Inativo", false)));

        assertThatThrownBy(() -> service.atualizar(10L, 20L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Não é possível vincular um item de orçamento a um serviço inativo.");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveRejeitarTrocaParaServicoInexistente() {
        ItemOrcamento item = item(20L, 10L, servico(5L, "Atual", true));
        ItemOrcamentoUpdateRequest request = new ItemOrcamentoUpdateRequest();
        request.setServicoId(99L);
        when(repository.findByIdAndOrcamento_Id(20L, 10L)).thenReturn(Optional.of(item));
        when(servicoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(10L, 20L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Serviço não encontrado. Id: 99");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveExcluirFisicamenteSomenteItemPertencenteAoOrcamento() {
        ItemOrcamento item = item(20L, 10L, servico(5L, "Instalação", true));
        when(repository.findByIdAndOrcamento_Id(20L, 10L)).thenReturn(Optional.of(item));

        service.deletar(10L, 20L);

        verify(repository).delete(item);
        verify(orcamentoRepository, never()).delete(any());
        verify(servicoRepository, never()).delete(any());
    }

    private void prepararCriacao(Servico servico) {
        when(orcamentoRepository.findById(10L)).thenReturn(Optional.of(orcamento(10L)));
        when(servicoRepository.findById(servico.getId())).thenReturn(Optional.of(servico));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void prepararReferenciasParaCriacao() {
        when(orcamentoRepository.findById(10L)).thenReturn(Optional.of(orcamento(10L)));
        when(servicoRepository.findById(5L))
                .thenReturn(Optional.of(servico(5L, "Instalação", true)));
    }

    private void prepararAtualizacao(ItemOrcamento item) {
        when(repository.findByIdAndOrcamento_Id(20L, 10L)).thenReturn(Optional.of(item));
        when(repository.saveAndFlush(item)).thenReturn(item);
    }

    private ItemOrcamentoRequest request(
            Long servicoId,
            String quantidade,
            String valorUnitario,
            String desconto) {

        ItemOrcamentoRequest request = new ItemOrcamentoRequest();
        request.setServicoId(servicoId);
        request.setQuantidade(new BigDecimal(quantidade));
        request.setValorUnitario(new BigDecimal(valorUnitario));
        request.setDesconto(desconto != null ? new BigDecimal(desconto) : null);
        return request;
    }

    private ItemOrcamento item(Long id, Long orcamentoId, Servico servico) {
        return ItemOrcamento.builder()
                .id(id)
                .orcamento(orcamento(orcamentoId))
                .servico(servico)
                .descricao("Snapshot")
                .quantidade(new BigDecimal("2.0000"))
                .valorUnitario(new BigDecimal("50.00"))
                .desconto(new BigDecimal("10.00"))
                .valorTotal(new BigDecimal("90.00"))
                .criadoEm(LocalDateTime.of(2026, 8, 20, 12, 0))
                .build();
    }

    private Orcamento orcamento(Long id) {
        return Orcamento.builder().id(id).numero(1000L + id).build();
    }

    private Servico servico(Long id, String nome, boolean ativo) {
        return Servico.builder().id(id).nome(nome).ativo(ativo).build();
    }
}
