package br.com.nucleodasreformas.nucleoerp.orcamento.service;

import br.com.nucleodasreformas.nucleoerp.categoria_servico.entity.CategoriaServico;
import br.com.nucleodasreformas.nucleoerp.categoria_servico.repository.CategoriaServicoRepository;
import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.cliente.repository.ClienteRepository;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.service.ItemOrcamentoService;
import br.com.nucleodasreformas.nucleoerp.material.entity.Material;
import br.com.nucleodasreformas.nucleoerp.material.repository.MaterialRepository;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.dto.MaterialOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.dto.MaterialOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.dto.MaterialOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.service.MaterialOrcamentoService;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoResponse;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class OrcamentoCustoTotalMateriaisIntegrationTest {

    @Autowired
    private OrcamentoService orcamentoService;

    @Autowired
    private MaterialOrcamentoService materialOrcamentoService;

    @Autowired
    private ItemOrcamentoService itemOrcamentoService;

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

    @Test
    void deveRefletirInclusaoAtualizacaoEExclusaoDosMateriais() {
        Orcamento orcamento = salvarOrcamento();
        Material material = salvarMaterial();

        assertTotais(orcamento, "0.00", "0.00");

        MaterialOrcamentoResponse primeiro = materialOrcamentoService.salvar(
                orcamento.getId(), materialRequest(material.getId(), "2.0000", "50.00"));
        assertTotais(orcamento, "0.00", "100.00");

        MaterialOrcamentoResponse segundo = materialOrcamentoService.salvar(
                orcamento.getId(), materialRequest(material.getId(), "1.5000", "40.00"));
        assertThat(segundo.getCustoTotal()).isEqualByComparingTo("60.00");
        assertTotais(orcamento, "0.00", "160.00");

        MaterialOrcamentoUpdateRequest update = new MaterialOrcamentoUpdateRequest();
        update.setQuantidade(new BigDecimal("2.0000"));
        materialOrcamentoService.atualizar(orcamento.getId(), segundo.getId(), update);
        assertTotais(orcamento, "0.00", "180.00");

        materialOrcamentoService.deletar(orcamento.getId(), primeiro.getId());
        assertTotais(orcamento, "0.00", "80.00");
    }

    @Test
    void deveManterValorComercialECustoDeMateriaisSeparados() {
        Orcamento orcamento = salvarOrcamento();
        Material material = salvarMaterial();
        materialOrcamentoService.salvar(
                orcamento.getId(), materialRequest(material.getId(), "2.0000", "500.00"));

        assertTotais(orcamento, "0.00", "1000.00");

        Servico servico = salvarServico();
        itemOrcamentoService.salvar(
                orcamento.getId(), itemRequest(servico.getId(), "1", "75.00", "0.00"));

        assertTotais(orcamento, "75.00", "1000.00");
    }

    @Test
    void deveListarCadaOrcamentoComSeusDoisAgregados() {
        Orcamento primeiro = salvarOrcamento();
        Orcamento segundo = salvarOrcamento();
        Orcamento semLinhas = salvarOrcamento();
        Material material = salvarMaterial();
        Servico servico = salvarServico();
        materialOrcamentoService.salvar(
                primeiro.getId(), materialRequest(material.getId(), "2.0000", "50.00"));
        itemOrcamentoService.salvar(
                primeiro.getId(), itemRequest(servico.getId(), "1", "250.00", "0.00"));
        materialOrcamentoService.salvar(
                segundo.getId(), materialRequest(material.getId(), "1.0000", "40.00"));
        itemOrcamentoService.salvar(
                segundo.getId(), itemRequest(servico.getId(), "1", "80.00", "0.00"));

        List<OrcamentoResponse> respostas = orcamentoService.listar();

        assertResponse(respostas, primeiro.getId(), "250.00", "100.00");
        assertResponse(respostas, segundo.getId(), "80.00", "40.00");
        assertResponse(respostas, semLinhas.getId(), "0.00", "0.00");
    }

    private void assertTotais(Orcamento orcamento, String comercial, String materiais) {
        OrcamentoResponse response = orcamentoService.buscarPorId(orcamento.getId());
        assertThat(response.getTotalComercial()).isEqualTo(new BigDecimal(comercial));
        assertThat(response.getCustoTotalMateriais()).isEqualTo(new BigDecimal(materiais));
        assertThat(response.getTotalComercial().scale()).isEqualTo(2);
        assertThat(response.getCustoTotalMateriais().scale()).isEqualTo(2);
    }

    private void assertResponse(
            List<OrcamentoResponse> respostas,
            Long orcamentoId,
            String comercial,
            String materiais) {
        assertThat(respostas).filteredOn(response -> response.getId().equals(orcamentoId))
                .singleElement()
                .satisfies(response -> {
                    assertThat(response.getTotalComercial()).isEqualTo(new BigDecimal(comercial));
                    assertThat(response.getCustoTotalMateriais())
                            .isEqualTo(new BigDecimal(materiais));
                });
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
                .nome("Cliente custo " + UUID.randomUUID())
                .build());
        StatusOrcamento rascunho = statusOrcamentoRepository
                .findByNomeNormalizado("Rascunho")
                .orElseThrow();
        return orcamentoRepository.saveAndFlush(Orcamento.builder()
                .cliente(cliente)
                .statusOrcamento(rascunho)
                .build());
    }

    private Material salvarMaterial() {
        return materialRepository.saveAndFlush(Material.builder()
                .nome("Material custo " + UUID.randomUUID())
                .unidade("M2")
                .build());
    }

    private Servico salvarServico() {
        CategoriaServico categoria = categoriaServicoRepository.saveAndFlush(
                CategoriaServico.builder()
                        .nome("Categoria custo " + UUID.randomUUID())
                        .build());
        return servicoRepository.saveAndFlush(Servico.builder()
                .nome("Serviço custo " + UUID.randomUUID())
                .categoriaServico(categoria)
                .build());
    }
}
