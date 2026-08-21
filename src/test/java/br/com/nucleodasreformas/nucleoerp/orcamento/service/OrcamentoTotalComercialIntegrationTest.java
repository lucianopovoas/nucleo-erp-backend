package br.com.nucleodasreformas.nucleoerp.orcamento.service;

import br.com.nucleodasreformas.nucleoerp.categoria_servico.entity.CategoriaServico;
import br.com.nucleodasreformas.nucleoerp.categoria_servico.repository.CategoriaServicoRepository;
import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.cliente.repository.ClienteRepository;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.service.ItemOrcamentoService;
import br.com.nucleodasreformas.nucleoerp.material.entity.Material;
import br.com.nucleodasreformas.nucleoerp.material.repository.MaterialRepository;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.entity.MaterialOrcamento;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.repository.MaterialOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.repository.OrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.servico.entity.Servico;
import br.com.nucleodasreformas.nucleoerp.servico.repository.ServicoRepository;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.entity.StatusOrcamento;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.repository.StatusOrcamentoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class OrcamentoTotalComercialIntegrationTest {

    @Autowired
    private OrcamentoService orcamentoService;

    @Autowired
    private ItemOrcamentoService itemOrcamentoService;

    @Autowired
    private OrcamentoRepository orcamentoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private StatusOrcamentoRepository statusOrcamentoRepository;

    @Autowired
    private CategoriaServicoRepository categoriaServicoRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private MaterialOrcamentoRepository materialOrcamentoRepository;

    @Test
    void deveRefletirInclusaoAtualizacaoEExclusaoDosItens() {
        Orcamento orcamento = salvarOrcamento();
        Servico servico = salvarServico();

        assertTotal(orcamento, "0.00");

        ItemOrcamentoResponse primeiro = itemOrcamentoService.salvar(
                orcamento.getId(), itemRequest(servico.getId(), "1", "100.00", "0.00"));
        assertTotal(orcamento, "100.00");

        ItemOrcamentoResponse segundo = itemOrcamentoService.salvar(
                orcamento.getId(), itemRequest(servico.getId(), "2", "130.00", "10.00"));
        assertThat(segundo.getValorTotal()).isEqualByComparingTo("250.00");
        assertTotal(orcamento, "350.00");

        ItemOrcamentoUpdateRequest update = new ItemOrcamentoUpdateRequest();
        update.setValorUnitario(new BigDecimal("155.00"));
        itemOrcamentoService.atualizar(orcamento.getId(), segundo.getId(), update);
        assertTotal(orcamento, "400.00");

        itemOrcamentoService.deletar(orcamento.getId(), primeiro.getId());
        assertTotal(orcamento, "300.00");
    }

    @Test
    void materialOrcamentoNaoDeveParticiparDoTotalComercial() {
        Orcamento orcamento = salvarOrcamento();
        Material material = materialRepository.saveAndFlush(Material.builder()
                .nome("Material total " + UUID.randomUUID())
                .unidade("m2")
                .build());
        materialOrcamentoRepository.saveAndFlush(MaterialOrcamento.builder()
                .orcamento(orcamento)
                .material(material)
                .descricao(material.getNome())
                .unidade(material.getUnidade())
                .quantidade(new BigDecimal("2.0000"))
                .custoUnitario(new BigDecimal("500.00"))
                .custoTotal(new BigDecimal("1000.00"))
                .build());

        assertTotal(orcamento, "0.00");

        Servico servico = salvarServico();
        itemOrcamentoService.salvar(
                orcamento.getId(), itemRequest(servico.getId(), "1", "75.00", "0.00"));

        assertTotal(orcamento, "75.00");
    }

    private void assertTotal(Orcamento orcamento, String esperado) {
        BigDecimal total = orcamentoService.buscarPorId(orcamento.getId()).getTotalComercial();
        assertThat(total).isEqualTo(new BigDecimal(esperado));
        assertThat(total.scale()).isEqualTo(2);
    }

    private ItemOrcamentoRequest itemRequest(
            Long servicoId,
            String quantidade,
            String valorUnitario,
            String desconto) {
        ItemOrcamentoRequest request = new ItemOrcamentoRequest();
        request.setServicoId(servicoId);
        request.setQuantidade(new BigDecimal(quantidade));
        request.setValorUnitario(new BigDecimal(valorUnitario));
        request.setDesconto(new BigDecimal(desconto));
        return request;
    }

    private Orcamento salvarOrcamento() {
        Cliente cliente = clienteRepository.saveAndFlush(Cliente.builder()
                .nome("Cliente total " + UUID.randomUUID())
                .build());
        StatusOrcamento rascunho = statusOrcamentoRepository
                .findByNomeNormalizado("Rascunho")
                .orElseThrow();
        return orcamentoRepository.saveAndFlush(Orcamento.builder()
                .cliente(cliente)
                .statusOrcamento(rascunho)
                .build());
    }

    private Servico salvarServico() {
        CategoriaServico categoria = categoriaServicoRepository.saveAndFlush(
                CategoriaServico.builder()
                        .nome("Categoria total " + UUID.randomUUID())
                        .build());
        return servicoRepository.saveAndFlush(Servico.builder()
                .nome("Serviço total " + UUID.randomUUID())
                .categoriaServico(categoria)
                .build());
    }
}
