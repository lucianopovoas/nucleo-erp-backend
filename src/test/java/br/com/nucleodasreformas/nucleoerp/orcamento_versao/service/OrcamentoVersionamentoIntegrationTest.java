package br.com.nucleodasreformas.nucleoerp.orcamento_versao.service;

import br.com.nucleodasreformas.nucleoerp.categoria_servico.entity.CategoriaServico;
import br.com.nucleodasreformas.nucleoerp.categoria_servico.repository.CategoriaServicoRepository;
import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.cliente.repository.ClienteRepository;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.service.DespesaOrcamentoService;
import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.dto.ItemOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.service.ItemOrcamentoService;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto.MaoDeObraOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto.MaoDeObraOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.service.MaoDeObraOrcamentoService;
import br.com.nucleodasreformas.nucleoerp.material.entity.Material;
import br.com.nucleodasreformas.nucleoerp.material.repository.MaterialRepository;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.dto.MaterialOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.dto.MaterialOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.service.MaterialOrcamentoService;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento.service.OrcamentoService;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.dto.OrcamentoVersaoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.dto.OrcamentoVersaoStatusRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.dto.OrcamentoVersaoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.servico.entity.Servico;
import br.com.nucleodasreformas.nucleoerp.servico.repository.ServicoRepository;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.entity.StatusOrcamento;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.repository.StatusOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.entity.UnidadeMaoDeObra;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.repository.UnidadeMaoDeObraRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class OrcamentoVersionamentoIntegrationTest {

    @Autowired private OrcamentoService orcamentoService;
    @Autowired private OrcamentoVersaoService versaoService;
    @Autowired private ItemOrcamentoService itemService;
    @Autowired private MaterialOrcamentoService materialService;
    @Autowired private MaoDeObraOrcamentoService maoDeObraService;
    @Autowired private DespesaOrcamentoService despesaService;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private CategoriaServicoRepository categoriaRepository;
    @Autowired private ServicoRepository servicoRepository;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private UnidadeMaoDeObraRepository unidadeRepository;
    @Autowired private StatusOrcamentoRepository statusRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;

    @Test
    void deveCriarV1ClonarSnapshotsIsoladamenteECongelarHistorico() {
        Cliente cliente = salvarCliente("Cliente A");
        OrcamentoResponse orcamento = criarOrcamento(cliente, "Observação V1");
        Long v1 = orcamento.getVersaoAtual().getId();

        Servico servico = salvarServico();
        Material material = salvarMaterial();
        UnidadeMaoDeObra unidade = salvarUnidade();

        Long itemV1 = itemService.salvar(orcamento.getId(), v1,
                itemRequest(servico.getId(), "2", "500.00", "50.00")).getId();
        Long materialV1 = materialService.salvar(orcamento.getId(), v1,
                materialRequest(material.getId(), "3", "100.00")).getId();
        Long maoDeObraV1 = maoDeObraService.salvar(orcamento.getId(), v1,
                maoRequest(unidade.getId(), "Instalação", "2", "80.00")).getId();
        Long despesaV1 = despesaService.salvar(orcamento.getId(), v1,
                despesaRequest("Frete", "40.00")).getId();

        OrcamentoVersaoResponse antes = versaoService.buscarPorId(orcamento.getId(), v1);
        assertThat(antes.getTotalComercial()).isEqualByComparingTo("950.00");
        assertThat(antes.getCustoTotalMateriais()).isEqualByComparingTo("300.00");
        assertThat(antes.getCustoTotalMaoDeObra()).isEqualByComparingTo("160.00");
        assertThat(antes.getCustoTotalDespesas()).isEqualByComparingTo("40.00");
        assertThat(antes.getMargemPrevista()).isEqualByComparingTo("450.00");

        alterarStatus(orcamento.getId(), v1, "ENVIADO");
        assertThatThrownBy(() -> despesaService.salvar(
                orcamento.getId(), v1, despesaRequest("Bloqueada", "1.00")))
                .isInstanceOf(BusinessException.class);

        servico.setAtivo(false);
        material.setAtivo(false);
        unidade.setAtivo(false);
        servicoRepository.saveAndFlush(servico);
        materialRepository.saveAndFlush(material);
        unidadeRepository.saveAndFlush(unidade);

        OrcamentoVersaoResponse v2Response = versaoService.criarNovaVersao(orcamento.getId(), v1);
        Long v2 = v2Response.getId();
        assertThat(v2Response.getNumeroVersao()).isEqualTo(2);
        assertThat(v2Response.getStatus().getCodigo()).isEqualTo("RASCUNHO");
        assertThat(v2Response.getObservacao()).isEqualTo("Observação V1");
        assertThat(v2Response.getTotalComercial()).isEqualByComparingTo("950.00");
        assertThat(materialService.listar(orcamento.getId(), v2)).singleElement()
                .satisfies(linha -> {
                    assertThat(linha.getId()).isNotEqualTo(materialV1);
                    assertThat(linha.getMaterial().getId()).isEqualTo(material.getId());
                    assertThat(linha.getDescricao()).isEqualTo(material.getNome());
                    assertThat(linha.getUnidade()).isEqualTo("M2");
                    assertThat(linha.getCustoTotal()).isEqualByComparingTo("300.00");
                });
        assertThat(itemService.listar(orcamento.getId(), v2)).singleElement()
                .satisfies(linha -> {
                    assertThat(linha.getId()).isNotEqualTo(itemV1);
                    assertThat(linha.getServico().getId()).isEqualTo(servico.getId());
                    assertThat(linha.getDescricao()).isEqualTo(servico.getNome());
                    assertThat(linha.getQuantidade()).isEqualByComparingTo("2.0000");
                    assertThat(linha.getValorUnitario()).isEqualByComparingTo("500.00");
                    assertThat(linha.getDesconto()).isEqualByComparingTo("50.00");
                    assertThat(linha.getValorTotal()).isEqualByComparingTo("950.00");
                });
        assertThat(maoDeObraService.listar(orcamento.getId(), v2)).singleElement()
                .satisfies(linha -> {
                    assertThat(linha.getId()).isNotEqualTo(maoDeObraV1);
                    assertThat(linha.getUnidadeMaoDeObra().getId()).isEqualTo(unidade.getId());
                    assertThat(linha.getDescricao()).isEqualTo("Instalação");
                    assertThat(linha.getUnidade()).isEqualTo(unidade.getNome());
                    assertThat(linha.getCustoTotal()).isEqualByComparingTo("160.00");
                });
        assertThat(despesaService.listar(orcamento.getId(), v2)).singleElement()
                .satisfies(linha -> {
                    assertThat(linha.getId()).isNotEqualTo(despesaV1);
                    assertThat(linha.getDescricao()).isEqualTo("Frete");
                    assertThat(linha.getValor()).isEqualByComparingTo("40.00");
                });

        ItemOrcamentoUpdateRequest update = new ItemOrcamentoUpdateRequest();
        update.setDescricao("Condição V2");
        update.setValorUnitario(new BigDecimal("600.00"));
        Long itemV2 = itemService.listar(orcamento.getId(), v2).getFirst().getId();
        itemService.atualizar(orcamento.getId(), v2, itemV2, update);

        assertThat(versaoService.buscarPorId(orcamento.getId(), v1).getTotalComercial())
                .isEqualByComparingTo("950.00");
        assertThat(versaoService.buscarPorId(orcamento.getId(), v2).getTotalComercial())
                .isEqualByComparingTo("1150.00");
        assertThatThrownBy(() -> versaoService.alterarStatus(
                orcamento.getId(), v1, statusRequest("APROVADO")))
                .isInstanceOf(BusinessException.class);

        alterarStatus(orcamento.getId(), v2, "ENVIADO");
        alterarStatus(orcamento.getId(), v2, "APROVADO");
        assertThatThrownBy(() -> versaoService.criarNovaVersao(orcamento.getId(), v2))
                .isInstanceOf(BusinessException.class);

        Cliente outro = salvarCliente("Cliente B");
        OrcamentoUpdateRequest troca = new OrcamentoUpdateRequest();
        troca.setClienteId(outro.getId());
        assertThatThrownBy(() -> orcamentoService.atualizar(orcamento.getId(), troca))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void devePermitirCorrigirClienteSomenteNaV1RascunhoECriarNovaAposRecusa() {
        Cliente primeiro = salvarCliente("Primeiro");
        Cliente segundo = salvarCliente("Segundo");
        OrcamentoResponse orcamento = criarOrcamento(primeiro, null);
        Long v1 = orcamento.getVersaoAtual().getId();

        OrcamentoUpdateRequest update = new OrcamentoUpdateRequest();
        update.setClienteId(segundo.getId());
        assertThat(orcamentoService.atualizar(orcamento.getId(), update).getCliente().getId())
                .isEqualTo(segundo.getId());

        alterarStatus(orcamento.getId(), v1, "ENVIADO");
        alterarStatus(orcamento.getId(), v1, "RECUSADO");
        OrcamentoVersaoResponse v2 = versaoService.criarNovaVersao(orcamento.getId(), v1);
        assertThat(v2.getNumeroVersao()).isEqualTo(2);
        assertThat(v2.getStatus().getCodigo()).isEqualTo("RASCUNHO");
    }

    @Test
    void deveTratarVersaoDeOutroOrcamentoComoInexistente() {
        OrcamentoResponse primeiro = criarOrcamento(salvarCliente("A"), null);
        OrcamentoResponse segundo = criarOrcamento(salvarCliente("B"), null);

        assertThatThrownBy(() -> versaoService.buscarPorId(
                primeiro.getId(), segundo.getVersaoAtual().getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ENVIADO", "APROVADO", "RECUSADO", "CANCELADO"})
    void deveCongelarTodoConteudoQuandoVersaoNaoForRascunho(String codigoFinal) {
        OrcamentoResponse orcamento = criarOrcamento(salvarCliente("Congelamento"), null);
        Long versaoId = orcamento.getVersaoAtual().getId();
        Servico servico = salvarServico();
        Material material = salvarMaterial();
        UnidadeMaoDeObra unidade = salvarUnidade();

        if (!"CANCELADO".equals(codigoFinal)) {
            alterarStatus(orcamento.getId(), versaoId, "ENVIADO");
        }
        if (!"ENVIADO".equals(codigoFinal)) {
            alterarStatus(orcamento.getId(), versaoId, codigoFinal);
        }

        OrcamentoVersaoUpdateRequest observacao = new OrcamentoVersaoUpdateRequest();
        observacao.setObservacao("Não pode alterar");
        assertThatThrownBy(() -> versaoService.atualizar(
                orcamento.getId(), versaoId, observacao)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> itemService.salvar(
                orcamento.getId(), versaoId,
                itemRequest(servico.getId(), "1", "1.00", "0.00")))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> materialService.salvar(
                orcamento.getId(), versaoId,
                materialRequest(material.getId(), "1", "1.00")))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> maoDeObraService.salvar(
                orcamento.getId(), versaoId,
                maoRequest(unidade.getId(), "Bloqueada", "1", "1.00")))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> despesaService.salvar(
                orcamento.getId(), versaoId, despesaRequest("Bloqueada", "1.00")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deveBloquearAlteracaoERemocaoDeTodasAsLinhasDepoisDoEnvio() {
        OrcamentoResponse orcamento = criarOrcamento(salvarCliente("Linhas congeladas"), null);
        Long versaoId = orcamento.getVersaoAtual().getId();
        Long itemId = itemService.salvar(orcamento.getId(), versaoId,
                itemRequest(salvarServico().getId(), "1", "10.00", "0.00")).getId();
        Long materialId = materialService.salvar(orcamento.getId(), versaoId,
                materialRequest(salvarMaterial().getId(), "1", "10.00")).getId();
        Long maoDeObraId = maoDeObraService.salvar(orcamento.getId(), versaoId,
                maoRequest(salvarUnidade().getId(), "Equipe", "1", "10.00")).getId();
        Long despesaId = despesaService.salvar(orcamento.getId(), versaoId,
                despesaRequest("Frete", "10.00")).getId();
        alterarStatus(orcamento.getId(), versaoId, "ENVIADO");

        ItemOrcamentoUpdateRequest item = new ItemOrcamentoUpdateRequest();
        item.setQuantidade(BigDecimal.TWO);
        MaterialOrcamentoUpdateRequest material = new MaterialOrcamentoUpdateRequest();
        material.setQuantidade(BigDecimal.TWO);
        MaoDeObraOrcamentoUpdateRequest maoDeObra = new MaoDeObraOrcamentoUpdateRequest();
        maoDeObra.setQuantidade(BigDecimal.TWO);
        DespesaOrcamentoUpdateRequest despesa = new DespesaOrcamentoUpdateRequest();
        despesa.setValor(new BigDecimal("20.00"));

        assertThatThrownBy(() -> itemService.atualizar(
                orcamento.getId(), versaoId, itemId, item)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> itemService.deletar(
                orcamento.getId(), versaoId, itemId)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> materialService.atualizar(
                orcamento.getId(), versaoId, materialId, material)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> materialService.deletar(
                orcamento.getId(), versaoId, materialId)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> maoDeObraService.atualizar(
                orcamento.getId(), versaoId, maoDeObraId, maoDeObra)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> maoDeObraService.deletar(
                orcamento.getId(), versaoId, maoDeObraId)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> despesaService.atualizar(
                orcamento.getId(), versaoId, despesaId, despesa)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> despesaService.deletar(
                orcamento.getId(), versaoId, despesaId)).isInstanceOf(BusinessException.class);
    }

    @Test
    void deveDelimitarCadaLinhaPelaVersaoNoContextoDoOrcamento() {
        OrcamentoResponse orcamento = criarOrcamento(salvarCliente("Ownership"), null);
        Long v1 = orcamento.getVersaoAtual().getId();
        Long itemId = itemService.salvar(orcamento.getId(), v1,
                itemRequest(salvarServico().getId(), "1", "10.00", "0.00")).getId();
        Long materialId = materialService.salvar(orcamento.getId(), v1,
                materialRequest(salvarMaterial().getId(), "1", "10.00")).getId();
        Long maoDeObraId = maoDeObraService.salvar(orcamento.getId(), v1,
                maoRequest(salvarUnidade().getId(), "Equipe", "1", "10.00")).getId();
        Long despesaId = despesaService.salvar(orcamento.getId(), v1,
                despesaRequest("Frete", "10.00")).getId();
        alterarStatus(orcamento.getId(), v1, "ENVIADO");
        Long v2 = versaoService.criarNovaVersao(orcamento.getId(), v1).getId();

        assertThatThrownBy(() -> itemService.buscarPorId(orcamento.getId(), v2, itemId))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> materialService.buscarPorId(orcamento.getId(), v2, materialId))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> maoDeObraService.buscarPorId(orcamento.getId(), v2, maoDeObraId))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> despesaService.buscarPorId(orcamento.getId(), v2, despesaId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deveManterPercentualZeroComReceitaZeroEMargemNegativa() {
        OrcamentoResponse orcamento = criarOrcamento(salvarCliente("Margem negativa"), null);
        Long versaoId = orcamento.getVersaoAtual().getId();
        despesaService.salvar(orcamento.getId(), versaoId,
                despesaRequest("Custo sem receita", "25.00"));

        OrcamentoVersaoResponse versao = versaoService.buscarPorId(orcamento.getId(), versaoId);
        assertThat(versao.getTotalComercial()).isEqualByComparingTo("0.00");
        assertThat(versao.getMargemPrevista()).isEqualByComparingTo("-25.00");
        assertThat(versao.getPercentualMargem()).isEqualByComparingTo("0.00");
    }

    @Test
    void deveListarVersoesComQuantidadeConstanteDeConsultas() {
        OrcamentoResponse orcamento = criarOrcamento(salvarCliente("Performance"), null);
        Long v1 = orcamento.getVersaoAtual().getId();
        alterarStatus(orcamento.getId(), v1, "ENVIADO");
        Long v2 = versaoService.criarNovaVersao(orcamento.getId(), v1).getId();
        alterarStatus(orcamento.getId(), v2, "ENVIADO");
        versaoService.criarNovaVersao(orcamento.getId(), v2);
        entityManager.flush();
        entityManager.clear();

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        assertThat(versaoService.listar(orcamento.getId())).hasSize(3);
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(6);
    }

    private OrcamentoResponse criarOrcamento(Cliente cliente, String observacao) {
        OrcamentoRequest request = new OrcamentoRequest();
        request.setClienteId(cliente.getId());
        request.setObservacao(observacao);
        return orcamentoService.salvar(request);
    }

    private void alterarStatus(Long orcamentoId, Long versaoId, String codigo) {
        versaoService.alterarStatus(orcamentoId, versaoId, statusRequest(codigo));
    }

    private OrcamentoVersaoStatusRequest statusRequest(String codigo) {
        StatusOrcamento status = statusRepository.findByCodigo(codigo).orElseThrow();
        OrcamentoVersaoStatusRequest request = new OrcamentoVersaoStatusRequest();
        request.setStatusOrcamentoId(status.getId());
        return request;
    }

    private Cliente salvarCliente(String nome) {
        return clienteRepository.saveAndFlush(Cliente.builder()
                .nome(nome + " " + UUID.randomUUID()).build());
    }

    private Servico salvarServico() {
        CategoriaServico categoria = categoriaRepository.saveAndFlush(CategoriaServico.builder()
                .nome("Categoria " + UUID.randomUUID()).build());
        return servicoRepository.saveAndFlush(Servico.builder()
                .nome("Serviço snapshot " + UUID.randomUUID())
                .categoriaServico(categoria).build());
    }

    private Material salvarMaterial() {
        return materialRepository.saveAndFlush(Material.builder()
                .nome("Material snapshot " + UUID.randomUUID()).unidade("M2").build());
    }

    private UnidadeMaoDeObra salvarUnidade() {
        return unidadeRepository.saveAndFlush(UnidadeMaoDeObra.builder()
                .nome("Diária " + UUID.randomUUID()).build());
    }

    private ItemOrcamentoRequest itemRequest(Long id, String q, String valor, String desconto) {
        ItemOrcamentoRequest request = new ItemOrcamentoRequest();
        request.setServicoId(id);
        request.setQuantidade(new BigDecimal(q));
        request.setValorUnitario(new BigDecimal(valor));
        request.setDesconto(new BigDecimal(desconto));
        return request;
    }

    private MaterialOrcamentoRequest materialRequest(Long id, String q, String custo) {
        MaterialOrcamentoRequest request = new MaterialOrcamentoRequest();
        request.setMaterialId(id);
        request.setQuantidade(new BigDecimal(q));
        request.setCustoUnitario(new BigDecimal(custo));
        return request;
    }

    private MaoDeObraOrcamentoRequest maoRequest(Long id, String descricao, String q, String custo) {
        MaoDeObraOrcamentoRequest request = new MaoDeObraOrcamentoRequest();
        request.setUnidadeMaoDeObraId(id);
        request.setDescricao(descricao);
        request.setQuantidade(new BigDecimal(q));
        request.setCustoUnitario(new BigDecimal(custo));
        return request;
    }

    private DespesaOrcamentoRequest despesaRequest(String descricao, String valor) {
        DespesaOrcamentoRequest request = new DespesaOrcamentoRequest();
        request.setDescricao(descricao);
        request.setValor(new BigDecimal(valor));
        return request;
    }
}
