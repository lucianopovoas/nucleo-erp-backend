package br.com.nucleodasreformas.nucleoerp.orcamento.service;

import br.com.nucleodasreformas.nucleoerp.categoria_servico.entity.CategoriaServico;
import br.com.nucleodasreformas.nucleoerp.categoria_servico.repository.CategoriaServicoRepository;
import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.cliente.repository.ClienteRepository;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.service.ItemOrcamentoService;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto.MaoDeObraOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto.MaoDeObraOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto.MaoDeObraOrcamentoUpdateRequest;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class OrcamentoCustoTotalMaoDeObraIntegrationTest {

    @Autowired
    private OrcamentoService orcamentoService;

    @Autowired
    private MaoDeObraOrcamentoService maoDeObraOrcamentoService;

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
    private UnidadeMaoDeObraRepository unidadeMaoDeObraRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private CategoriaServicoRepository categoriaServicoRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    @Test
    void deveRefletirInclusaoAtualizacaoEExclusaoDaMaoDeObra() {
        Orcamento orcamento = salvarOrcamento();
        UnidadeMaoDeObra unidade = salvarUnidade();

        assertTotais(orcamento, "0.00", "0.00", "0.00", "0.00", "0.00");

        MaoDeObraOrcamentoResponse primeira = maoDeObraOrcamentoService.salvar(
                orcamento.getId(), maoDeObraRequest(unidade.getId(), "Instalação", "2.0000", "50.00"));
        assertTotais(orcamento, "0.00", "0.00", "100.00", "-100.00", "0.00");

        MaoDeObraOrcamentoResponse segunda = maoDeObraOrcamentoService.salvar(
                orcamento.getId(), maoDeObraRequest(unidade.getId(), "Apoio", "1.5000", "40.00"));
        assertThat(segunda.getCustoTotal()).isEqualByComparingTo("60.00");
        assertTotais(orcamento, "0.00", "0.00", "160.00", "-160.00", "0.00");

        MaoDeObraOrcamentoUpdateRequest update = new MaoDeObraOrcamentoUpdateRequest();
        update.setQuantidade(new BigDecimal("2.0000"));
        maoDeObraOrcamentoService.atualizar(orcamento.getId(), segunda.getId(), update);
        assertTotais(orcamento, "0.00", "0.00", "180.00", "-180.00", "0.00");

        maoDeObraOrcamentoService.deletar(orcamento.getId(), primeira.getId());
        assertTotais(orcamento, "0.00", "0.00", "80.00", "-80.00", "0.00");
    }

    @Test
    void deveManterOsTresAgregadosIndependentes() {
        Orcamento orcamento = salvarOrcamento();
        UnidadeMaoDeObra unidade = salvarUnidade();
        Material material = salvarMaterial();
        Servico servico = salvarServico();

        itemOrcamentoService.salvar(
                orcamento.getId(), itemRequest(servico.getId(), "1", "500.00", "25.00"));
        materialOrcamentoService.salvar(
                orcamento.getId(), materialRequest(material.getId(), "2.0000", "60.00"));
        assertTotais(orcamento, "475.00", "120.00", "0.00", "355.00", "74.74");

        maoDeObraOrcamentoService.salvar(
                orcamento.getId(), maoDeObraRequest(unidade.getId(), "Custo zero", "1", "0.00"));
        maoDeObraOrcamentoService.salvar(
                orcamento.getId(), maoDeObraRequest(unidade.getId(), "Execução", "2", "75.00"));

        assertTotais(orcamento, "475.00", "120.00", "150.00", "205.00", "43.16");
    }

    @Test
    void deveListarCadaOrcamentoComSeusTresAgregados() {
        Orcamento primeiro = salvarOrcamento();
        Orcamento segundo = salvarOrcamento();
        Orcamento semLinhas = salvarOrcamento();
        UnidadeMaoDeObra unidade = salvarUnidade();
        Material material = salvarMaterial();
        Servico servico = salvarServico();

        itemOrcamentoService.salvar(
                primeiro.getId(), itemRequest(servico.getId(), "1", "250.00", "0.00"));
        materialOrcamentoService.salvar(
                primeiro.getId(), materialRequest(material.getId(), "2.0000", "50.00"));
        maoDeObraOrcamentoService.salvar(
                primeiro.getId(), maoDeObraRequest(unidade.getId(), "Primeira", "2", "35.00"));

        itemOrcamentoService.salvar(
                segundo.getId(), itemRequest(servico.getId(), "1", "80.00", "0.00"));
        materialOrcamentoService.salvar(
                segundo.getId(), materialRequest(material.getId(), "1.0000", "40.00"));
        maoDeObraOrcamentoService.salvar(
                segundo.getId(), maoDeObraRequest(unidade.getId(), "Segunda", "1", "25.00"));

        List<OrcamentoResponse> respostas = orcamentoService.listar();

        assertResponse(
                respostas, primeiro.getId(), "250.00", "100.00", "70.00", "80.00", "32.00");
        assertResponse(
                respostas, segundo.getId(), "80.00", "40.00", "25.00", "15.00", "18.75");
        assertResponse(
                respostas, semLinhas.getId(), "0.00", "0.00", "0.00", "0.00", "0.00");
    }

    private void assertTotais(
            Orcamento orcamento,
            String comercial,
            String materiais,
            String maoDeObra,
            String margem,
            String percentual) {
        OrcamentoResponse response = orcamentoService.buscarPorId(orcamento.getId());
        assertThat(response.getTotalComercial()).isEqualTo(new BigDecimal(comercial));
        assertThat(response.getCustoTotalMateriais()).isEqualTo(new BigDecimal(materiais));
        assertThat(response.getCustoTotalMaoDeObra()).isEqualTo(new BigDecimal(maoDeObra));
        assertThat(response.getMargemPrevista()).isEqualTo(new BigDecimal(margem));
        assertThat(response.getPercentualMargem()).isEqualTo(new BigDecimal(percentual));
        assertThat(response.getTotalComercial().scale()).isEqualTo(2);
        assertThat(response.getCustoTotalMateriais().scale()).isEqualTo(2);
        assertThat(response.getCustoTotalMaoDeObra().scale()).isEqualTo(2);
        assertThat(response.getMargemPrevista().scale()).isEqualTo(2);
        assertThat(response.getPercentualMargem().scale()).isEqualTo(2);
    }

    private void assertResponse(
            List<OrcamentoResponse> respostas,
            Long orcamentoId,
            String comercial,
            String materiais,
            String maoDeObra,
            String margem,
            String percentual) {
        assertThat(respostas).filteredOn(response -> response.getId().equals(orcamentoId))
                .singleElement()
                .satisfies(response -> {
                    assertThat(response.getTotalComercial()).isEqualTo(new BigDecimal(comercial));
                    assertThat(response.getCustoTotalMateriais()).isEqualTo(new BigDecimal(materiais));
                    assertThat(response.getCustoTotalMaoDeObra()).isEqualTo(new BigDecimal(maoDeObra));
                    assertThat(response.getMargemPrevista()).isEqualTo(new BigDecimal(margem));
                    assertThat(response.getPercentualMargem()).isEqualTo(new BigDecimal(percentual));
                });
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
                .nome("Cliente mão de obra " + UUID.randomUUID())
                .build());
        StatusOrcamento rascunho = statusOrcamentoRepository
                .findByNomeNormalizado("Rascunho")
                .orElseThrow();
        return orcamentoRepository.saveAndFlush(Orcamento.builder()
                .cliente(cliente)
                .statusOrcamento(rascunho)
                .build());
    }

    private UnidadeMaoDeObra salvarUnidade() {
        return unidadeMaoDeObraRepository.saveAndFlush(UnidadeMaoDeObra.builder()
                .nome("Unidade custo " + UUID.randomUUID())
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
