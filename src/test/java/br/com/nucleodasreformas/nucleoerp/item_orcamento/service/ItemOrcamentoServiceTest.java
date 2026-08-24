package br.com.nucleodasreformas.nucleoerp.item_orcamento.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.entity.ItemOrcamento;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.repository.ItemOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.service.OrcamentoVersaoGuard;
import br.com.nucleodasreformas.nucleoerp.servico.entity.Servico;
import br.com.nucleodasreformas.nucleoerp.servico.repository.ServicoRepository;
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
class ItemOrcamentoServiceTest {

    @Mock private ItemOrcamentoRepository repository;
    @Mock private ServicoRepository servicoRepository;
    @Mock private OrcamentoVersaoGuard versaoGuard;
    @InjectMocks private ItemOrcamentoService service;

    @Test
    void deveCalcularSubtotalDescontoETotalComercial() {
        prepararCriacao(servico(5L, "Instalação", true));

        ItemOrcamentoResponse response = service.salvar(
                1L, 2L, request(5L, "2.5000", "150.00", "20.00"));

        assertThat(response.getDescricao()).isEqualTo("Instalação");
        assertThat(response.getQuantidade()).isEqualByComparingTo("2.5000");
        assertThat(response.getDesconto()).isEqualByComparingTo("20.00");
        assertThat(response.getValorTotal()).isEqualByComparingTo("355.00");
        assertThat(response.getValorTotal().scale()).isEqualTo(2);
    }

    @Test
    void deveUsarDescontoZeroQuandoOmitidoEArredondarHalfUp() {
        prepararCriacao(servico(5L, "Serviço fracionado", true));

        ItemOrcamentoResponse response = service.salvar(
                1L, 2L, request(5L, "0.3333", "0.03", null));

        assertThat(response.getDesconto()).isEqualByComparingTo("0.00");
        assertThat(response.getValorTotal()).isEqualByComparingTo("0.01");
        assertThat(response.getValorTotal().scale()).isEqualTo(2);
    }

    @Test
    void devePermitirDescontoIgualAoSubtotal() {
        prepararCriacao(servico(5L, "Serviço", true));

        ItemOrcamentoResponse response = service.salvar(
                1L, 2L, request(5L, "2.0000", "10.00", "20.00"));

        assertThat(response.getValorTotal()).isEqualByComparingTo("0.00");
    }

    @Test
    void deveRejeitarDescontoMaiorQueSubtotal() {
        when(versaoGuard.bloquearEditavel(1L, 2L)).thenReturn(versao());
        when(servicoRepository.findById(5L)).thenReturn(Optional.of(servico(5L, "Serviço", true)));

        assertThatThrownBy(() -> service.salvar(
                1L, 2L, request(5L, "1.0000", "10.00", "10.01")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O desconto não pode ser maior que o subtotal do item.");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveRejeitarServicoInativoNaInclusao() {
        when(versaoGuard.bloquearEditavel(1L, 2L)).thenReturn(versao());
        when(servicoRepository.findById(5L)).thenReturn(Optional.of(servico(5L, "Inativo", false)));

        assertThatThrownBy(() -> service.salvar(
                1L, 2L, request(5L, "1.0000", "10.00", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("serviço inativo");
    }

    @Test
    void deveAtualizarSnapshotAoTrocarParaServicoAtivo() {
        ItemOrcamento item = item(servico(5L, "Nome antigo", false), "Snapshot antigo");
        Servico novo = servico(6L, "Nome novo", true);
        ItemOrcamentoUpdateRequest request = new ItemOrcamentoUpdateRequest();
        request.setServicoId(6L);

        when(repository.findByIdAndOrcamentoVersaoIdForUpdate(10L, 2L)).thenReturn(Optional.of(item));
        when(servicoRepository.findById(6L)).thenReturn(Optional.of(novo));
        when(repository.saveAndFlush(item)).thenReturn(item);

        ItemOrcamentoResponse response = service.atualizar(1L, 2L, 10L, request);

        assertThat(response.getServico().getId()).isEqualTo(6L);
        assertThat(response.getDescricao()).isEqualTo("Nome novo");
        verify(versaoGuard).bloquearEditavel(1L, 2L);
    }

    @Test
    void devePreservarReferenciaInativaESnapshotSemTrocaEfetiva() {
        ItemOrcamento item = item(servico(5L, "Catálogo renomeado", false), "Snapshot negociado");
        ItemOrcamentoUpdateRequest request = new ItemOrcamentoUpdateRequest();
        request.setQuantidade(new BigDecimal("3.0000"));

        when(repository.findByIdAndOrcamentoVersaoIdForUpdate(10L, 2L)).thenReturn(Optional.of(item));
        when(repository.saveAndFlush(item)).thenReturn(item);

        ItemOrcamentoResponse response = service.atualizar(1L, 2L, 10L, request);

        assertThat(response.getServico().getId()).isEqualTo(5L);
        assertThat(response.getDescricao()).isEqualTo("Snapshot negociado");
        assertThat(response.getValorTotal()).isEqualByComparingTo("30.00");
        verifyNoInteractions(servicoRepository);
    }

    private void prepararCriacao(Servico servico) {
        when(versaoGuard.bloquearEditavel(1L, 2L)).thenReturn(versao());
        when(servicoRepository.findById(servico.getId())).thenReturn(Optional.of(servico));
        when(repository.saveAndFlush(any(ItemOrcamento.class))).thenAnswer(invocation -> {
            ItemOrcamento item = invocation.getArgument(0);
            item.setId(10L);
            return item;
        });
    }

    private ItemOrcamentoRequest request(Long servicoId, String quantidade, String unitario, String desconto) {
        ItemOrcamentoRequest request = new ItemOrcamentoRequest();
        request.setServicoId(servicoId);
        request.setQuantidade(new BigDecimal(quantidade));
        request.setValorUnitario(new BigDecimal(unitario));
        request.setDesconto(desconto == null ? null : new BigDecimal(desconto));
        return request;
    }

    private ItemOrcamento item(Servico servico, String descricao) {
        return ItemOrcamento.builder()
                .id(10L)
                .orcamentoVersao(versao())
                .servico(servico)
                .descricao(descricao)
                .quantidade(new BigDecimal("1.0000"))
                .valorUnitario(new BigDecimal("10.00"))
                .desconto(new BigDecimal("0.00"))
                .valorTotal(new BigDecimal("10.00"))
                .build();
    }

    private Servico servico(Long id, String nome, boolean ativo) {
        return Servico.builder().id(id).nome(nome).ativo(ativo).build();
    }

    private OrcamentoVersao versao() {
        return OrcamentoVersao.builder().id(2L).build();
    }
}
