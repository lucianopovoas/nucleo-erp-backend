package br.com.nucleodasreformas.nucleoerp.orcamento.service;

import br.com.nucleodasreformas.nucleoerp.categoria_servico.entity.CategoriaServico;
import br.com.nucleodasreformas.nucleoerp.categoria_servico.repository.CategoriaServicoRepository;
import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.cliente.repository.ClienteRepository;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.service.DespesaOrcamentoService;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.service.ItemOrcamentoService;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto.MaoDeObraOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.service.MaoDeObraOrcamentoService;
import br.com.nucleodasreformas.nucleoerp.material.entity.Material;
import br.com.nucleodasreformas.nucleoerp.material.repository.MaterialRepository;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.dto.MaterialOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.service.MaterialOrcamentoService;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.repository.OrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.servico.entity.Servico;
import br.com.nucleodasreformas.nucleoerp.servico.repository.ServicoRepository;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.entity.StatusOrcamento;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.repository.StatusOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.entity.UnidadeMaoDeObra;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.repository.UnidadeMaoDeObraRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class OrcamentoCustoTotalDespesasIntegrationTest {

    @Autowired
    private OrcamentoService orcamentoService;

    @Autowired
    private DespesaOrcamentoService despesaOrcamentoService;

    @Autowired
    private ItemOrcamentoService itemOrcamentoService;

    @Autowired
    private MaterialOrcamentoService materialOrcamentoService;

    @Autowired
    private MaoDeObraOrcamentoService maoDeObraOrcamentoService;

    @Autowired
    private OrcamentoRepository orcamentoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private StatusOrcamentoRepository statusOrcamentoRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private CategoriaServicoRepository categoriaServicoRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private UnidadeMaoDeObraRepository unidadeMaoDeObraRepository;

    @Test
    void deveRefletirInclusaoAtualizacaoEExclusaoDeDespesasNaMargem() {
        Orcamento orcamento = salvarOrcamento();
        Servico servico = salvarServico();
        Material material = salvarMaterial();
        UnidadeMaoDeObra unidade = salvarUnidade();

        itemOrcamentoService.salvar(
                orcamento.getId(), itemRequest(servico.getId(), "1", "5000.00", "0.00"));
        materialOrcamentoService.salvar(
                orcamento.getId(), materialRequest(material.getId(), "2.0000", "600.00"));
        maoDeObraOrcamentoService.salvar(
                orcamento.getId(), maoDeObraRequest(unidade.getId(), "Instalação", "2", "400.00"));

        DespesaOrcamentoResponse frete = despesaOrcamentoService.salvar(
                orcamento.getId(), despesaRequest("Frete", "180.00"));
        DespesaOrcamentoResponse pedagio = despesaOrcamentoService.salvar(
                orcamento.getId(), despesaRequest("Pedágio", "60.00"));
        assertTotais(orcamento, "5000.00", "1200.00", "800.00", "240.00", "2760.00", "55.20");

        despesaOrcamentoService.salvar(
                orcamento.getId(), despesaRequest("Aluguel de andaime", "450.00"));
        assertTotais(orcamento, "5000.00", "1200.00", "800.00", "690.00", "2310.00", "46.20");

        DespesaOrcamentoUpdateRequest update = new DespesaOrcamentoUpdateRequest();
        update.setValor(new BigDecimal("100.00"));
        despesaOrcamentoService.atualizar(orcamento.getId(), pedagio.getId(), update);
        assertTotais(orcamento, "5000.00", "1200.00", "800.00", "730.00", "2270.00", "45.40");

        despesaOrcamentoService.deletar(orcamento.getId(), frete.getId());
        assertTotais(orcamento, "5000.00", "1200.00", "800.00", "550.00", "2450.00", "49.00");
    }

    private void assertTotais(
            Orcamento orcamento,
            String comercial,
            String materiais,
            String maoDeObra,
            String despesas,
            String margem,
            String percentual) {
        OrcamentoResponse response = orcamentoService.buscarPorId(orcamento.getId());

        assertThat(response.getTotalComercial()).isEqualTo(new BigDecimal(comercial));
        assertThat(response.getCustoTotalMateriais()).isEqualTo(new BigDecimal(materiais));
        assertThat(response.getCustoTotalMaoDeObra()).isEqualTo(new BigDecimal(maoDeObra));
        assertThat(response.getCustoTotalDespesas()).isEqualTo(new BigDecimal(despesas));
        assertThat(response.getMargemPrevista()).isEqualTo(new BigDecimal(margem));
        assertThat(response.getPercentualMargem()).isEqualTo(new BigDecimal(percentual));
        assertThat(response.getCustoTotalDespesas().scale()).isEqualTo(2);
        assertThat(response.getMargemPrevista().scale()).isEqualTo(2);
        assertThat(response.getPercentualMargem().scale()).isEqualTo(2);
    }

    private DespesaOrcamentoRequest despesaRequest(String descricao, String valor) {
        DespesaOrcamentoRequest request = new DespesaOrcamentoRequest();
        request.setDescricao(descricao);
        request.setValor(new BigDecimal(valor));
        return request;
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

    private MaterialOrcamentoRequest materialRequest(
            Long materialId,
            String quantidade,
            String custoUnitario) {
        MaterialOrcamentoRequest request = new MaterialOrcamentoRequest();
        request.setMaterialId(materialId);
        request.setQuantidade(new BigDecimal(quantidade));
        request.setCustoUnitario(new BigDecimal(custoUnitario));
        return request;
    }

    private MaoDeObraOrcamentoRequest maoDeObraRequest(
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

    private Orcamento salvarOrcamento() {
        Cliente cliente = clienteRepository.saveAndFlush(Cliente.builder()
                .nome("Cliente despesas " + UUID.randomUUID())
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
                        .nome("Categoria despesas " + UUID.randomUUID())
                        .build());
        return servicoRepository.saveAndFlush(Servico.builder()
                .nome("Serviço despesas " + UUID.randomUUID())
                .categoriaServico(categoria)
                .build());
    }

    private Material salvarMaterial() {
        return materialRepository.saveAndFlush(Material.builder()
                .nome("Material despesas " + UUID.randomUUID())
                .unidade("M2")
                .build());
    }

    private UnidadeMaoDeObra salvarUnidade() {
        return unidadeMaoDeObraRepository.saveAndFlush(UnidadeMaoDeObra.builder()
                .nome("Unidade despesas " + UUID.randomUUID())
                .build());
    }
}
