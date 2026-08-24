package br.com.nucleodasreformas.nucleoerp.orcamento_versao.repository;

import br.com.nucleodasreformas.nucleoerp.categoria_servico.entity.CategoriaServico;
import br.com.nucleodasreformas.nucleoerp.categoria_servico.repository.CategoriaServicoRepository;
import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.cliente.repository.ClienteRepository;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.service.DespesaOrcamentoService;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.service.ItemOrcamentoService;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto.MaoDeObraOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.service.MaoDeObraOrcamentoService;
import br.com.nucleodasreformas.nucleoerp.material.entity.Material;
import br.com.nucleodasreformas.nucleoerp.material.repository.MaterialRepository;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.dto.MaterialOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.service.MaterialOrcamentoService;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento.service.OrcamentoService;
import br.com.nucleodasreformas.nucleoerp.servico.entity.Servico;
import br.com.nucleodasreformas.nucleoerp.servico.repository.ServicoRepository;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.entity.UnidadeMaoDeObra;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.repository.UnidadeMaoDeObraRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class OrcamentoMonetaryPrecisionIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private CategoriaServicoRepository categoriaRepository;
    @Autowired private ServicoRepository servicoRepository;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private UnidadeMaoDeObraRepository unidadeRepository;
    @Autowired private OrcamentoService orcamentoService;
    @Autowired private ItemOrcamentoService itemService;
    @Autowired private MaterialOrcamentoService materialService;
    @Autowired private MaoDeObraOrcamentoService maoDeObraService;
    @Autowired private DespesaOrcamentoService despesaService;

    @Test
    void deveManterPrecisaoEEscalaDefinidasPelasMigrations() {
        assertColuna("item_orcamento", "quantidade", 15, 4);
        assertColuna("item_orcamento", "valor_unitario", 15, 2);
        assertColuna("item_orcamento", "desconto", 15, 2);
        assertColuna("item_orcamento", "valor_total", 15, 2);
        assertColuna("material_orcamento", "quantidade", 15, 4);
        assertColuna("material_orcamento", "custo_unitario", 15, 2);
        assertColuna("material_orcamento", "custo_total", 15, 2);
        assertColuna("mao_de_obra_orcamento", "quantidade", 15, 4);
        assertColuna("mao_de_obra_orcamento", "custo_unitario", 15, 2);
        assertColuna("mao_de_obra_orcamento", "custo_total", 15, 2);
        assertColuna("despesa_orcamento", "valor", 15, 2);
    }

    @Test
    void devePersistirEscalaContratadaEValorMonetarioMaximo() {
        String sufixo = UUID.randomUUID().toString();
        Cliente cliente = clienteRepository.saveAndFlush(Cliente.builder()
                .nome("Cliente precisão " + sufixo).build());
        CategoriaServico categoria = categoriaRepository.saveAndFlush(
                CategoriaServico.builder().nome("Categoria " + sufixo).build());
        Servico servico = servicoRepository.saveAndFlush(Servico.builder()
                .nome("Serviço " + sufixo).categoriaServico(categoria).build());
        Material material = materialRepository.saveAndFlush(Material.builder()
                .nome("Material " + sufixo).unidade("UN").build());
        UnidadeMaoDeObra unidade = unidadeRepository.saveAndFlush(
                UnidadeMaoDeObra.builder().nome("Unidade " + sufixo).build());

        OrcamentoRequest orcamentoRequest = new OrcamentoRequest();
        orcamentoRequest.setClienteId(cliente.getId());
        var orcamento = orcamentoService.salvar(orcamentoRequest);
        Long versaoId = orcamento.getVersaoAtual().getId();
        BigDecimal maximo = new BigDecimal("9999999999999.99");

        ItemOrcamentoRequest item = new ItemOrcamentoRequest();
        item.setServicoId(servico.getId());
        item.setQuantidade(new BigDecimal("1.0000"));
        item.setValorUnitario(maximo);
        item.setDesconto(new BigDecimal("0.00"));
        Long itemId = itemService.salvar(orcamento.getId(), versaoId, item).getId();

        MaterialOrcamentoRequest materialRequest = new MaterialOrcamentoRequest();
        materialRequest.setMaterialId(material.getId());
        materialRequest.setQuantidade(new BigDecimal("1.0000"));
        materialRequest.setCustoUnitario(maximo);
        Long materialId = materialService.salvar(
                orcamento.getId(), versaoId, materialRequest).getId();

        MaoDeObraOrcamentoRequest mao = new MaoDeObraOrcamentoRequest();
        mao.setUnidadeMaoDeObraId(unidade.getId());
        mao.setDescricao("Precisão");
        mao.setQuantidade(new BigDecimal("1.0000"));
        mao.setCustoUnitario(maximo);
        Long maoId = maoDeObraService.salvar(orcamento.getId(), versaoId, mao).getId();

        DespesaOrcamentoRequest despesa = new DespesaOrcamentoRequest();
        despesa.setDescricao("Precisão");
        despesa.setValor(maximo);
        Long despesaId = despesaService.salvar(orcamento.getId(), versaoId, despesa).getId();

        assertValor("item_orcamento", "quantidade", itemId, "1.0000", 4);
        assertValor("item_orcamento", "valor_unitario", itemId, "9999999999999.99", 2);
        assertValor("item_orcamento", "valor_total", itemId, "9999999999999.99", 2);
        assertValor("material_orcamento", "custo_total", materialId,
                "9999999999999.99", 2);
        assertValor("mao_de_obra_orcamento", "custo_total", maoId,
                "9999999999999.99", 2);
        assertValor("despesa_orcamento", "valor", despesaId,
                "9999999999999.99", 2);
    }

    private void assertColuna(String tabela, String coluna, int precisao, int escala) {
        Map<String, Object> metadados = jdbcTemplate.queryForMap("""
                SELECT numeric_precision, numeric_scale
                FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
                """, tabela, coluna);
        assertThat(((Number) metadados.get("numeric_precision")).intValue()).isEqualTo(precisao);
        assertThat(((Number) metadados.get("numeric_scale")).intValue()).isEqualTo(escala);
    }

    private void assertValor(
            String tabela, String coluna, Long id, String esperado, int escala) {
        BigDecimal valor = jdbcTemplate.queryForObject(
                "SELECT " + coluna + " FROM " + tabela + " WHERE id = ?",
                BigDecimal.class, id);
        assertThat(valor).isEqualByComparingTo(esperado);
        assertThat(valor.scale()).isEqualTo(escala);
    }
}
