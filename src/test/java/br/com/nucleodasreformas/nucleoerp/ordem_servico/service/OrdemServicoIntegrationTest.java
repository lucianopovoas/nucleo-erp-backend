package br.com.nucleodasreformas.nucleoerp.ordem_servico.service;

import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.cliente.repository.ClienteRepository;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.service.DespesaOrcamentoService;
import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento.service.OrcamentoService;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.dto.OrcamentoVersaoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.dto.OrcamentoVersaoStatusRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.service.OrcamentoVersaoService;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrdemServicoStatusRequest;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrdemServicoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.repository.StatusOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.entity.StatusOrdemServico;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.repository.StatusOrdemServicoRepository;
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
class OrdemServicoIntegrationTest {

    @Autowired private ClienteRepository clienteRepository;
    @Autowired private OrcamentoService orcamentoService;
    @Autowired private OrcamentoVersaoService versaoService;
    @Autowired private StatusOrcamentoRepository statusOrcamentoRepository;
    @Autowired private StatusOrdemServicoRepository statusRepository;
    @Autowired private OrdemServicoService service;
    @Autowired private DespesaOrcamentoService despesaService;
    @Autowired private EntityManagerFactory entityManagerFactory;

    @Test
    void deveCriarDaVersaoAprovadaComNumeroStatusInicialEOrigem() {
        OrcamentoResponse orcamento = criarOrcamentoAprovado("Origem aprovada");
        Long versaoId = orcamento.getVersaoAtual().getId();

        var ordem = service.salvar(orcamento.getId(), versaoId);

        assertThat(ordem.getNumero()).isPositive();
        assertThat(ordem.getStatus().getCodigo()).isEqualTo("COMPRAR_MATERIAL");
        assertThat(ordem.getObservacao()).isNull();
        assertThat(ordem.getOrigem().getOrcamento().getId()).isEqualTo(orcamento.getId());
        assertThat(ordem.getOrigem().getVersao().getId()).isEqualTo(versaoId);
        assertThat(ordem.getOrigem().getCliente().getId()).isEqualTo(orcamento.getCliente().getId());

        assertThatThrownBy(() -> service.salvar(orcamento.getId(), versaoId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe uma ordem de serviço");
    }

    @ParameterizedTest
    @ValueSource(strings = {"RASCUNHO", "ENVIADO", "RECUSADO", "CANCELADO"})
    void deveRejeitarVersaoSemAprovacao(String codigo) {
        OrcamentoResponse orcamento = criarOrcamento("Origem " + codigo);
        Long versaoId = orcamento.getVersaoAtual().getId();
        if (!"RASCUNHO".equals(codigo)) {
            alterarStatusComercial(orcamento.getId(), versaoId, "ENVIADO");
        }
        if ("RECUSADO".equals(codigo) || "CANCELADO".equals(codigo)) {
            alterarStatusComercial(orcamento.getId(), versaoId, codigo);
        }

        assertThatThrownBy(() -> service.salvar(orcamento.getId(), versaoId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("APROVADA");
    }

    @Test
    void deveValidarOwnershipERecursosInexistentes() {
        OrcamentoResponse primeiro = criarOrcamentoAprovado("Primeiro");
        OrcamentoResponse segundo = criarOrcamentoAprovado("Segundo");

        assertThatThrownBy(() -> service.salvar(
                primeiro.getId(), segundo.getVersaoAtual().getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.salvar(999999L, primeiro.getVersaoAtual().getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.salvar(primeiro.getId(), 999999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deveExecutarFluxoLinearEFecharObservacaoAoConcluir() {
        var ordem = salvarOrigemAprovada(criarOrcamentoAprovado("Fluxo"));

        assertObservacaoEditavel(ordem.getId(), "Preparação");
        assertThat(service.atualizar(ordem.getId(), observacao(null)).getObservacao()).isNull();
        ordem = alterarStatusOperacional(ordem.getId(), "EM_EXECUCAO");
        assertObservacaoEditavel(ordem.getId(), "Execução");
        ordem = alterarStatusOperacional(ordem.getId(), "INSTALAR");
        assertObservacaoEditavel(ordem.getId(), "Instalação");
        ordem = alterarStatusOperacional(ordem.getId(), "CONCLUIDO");

        OrdemServicoUpdateRequest update = observacao("Não pode");
        Long ordemId = ordem.getId();
        assertThatThrownBy(() -> service.atualizar(ordemId, update))
                .isInstanceOf(BusinessException.class);

        var idempotente = service.alterarStatus(ordemId, statusRequest("CONCLUIDO"));
        assertThat(idempotente.getStatus().getCodigo()).isEqualTo("CONCLUIDO");
        assertThatThrownBy(() -> service.alterarStatus(
                ordemId, statusRequest("INSTALAR")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deveRejeitarSaltoStatusCustomizadoEStatusInativo() {
        var ordem = salvarOrigemAprovada(criarOrcamentoAprovado("Transições"));

        assertThatThrownBy(() -> service.alterarStatus(
                ordem.getId(), statusRequest("INSTALAR")))
                .isInstanceOf(BusinessException.class);

        String sufixo = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        StatusOrdemServico customizado = statusRepository.saveAndFlush(
                StatusOrdemServico.builder()
                        .codigo("CUSTOMIZADO_" + sufixo.toUpperCase())
                        .nome("Customizado " + sufixo)
                        .build());
        OrdemServicoStatusRequest customizadoRequest = new OrdemServicoStatusRequest();
        customizadoRequest.setStatusOrdemServicoId(customizado.getId());
        assertThatThrownBy(() -> service.alterarStatus(ordem.getId(), customizadoRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Transição");

        customizado.setAtivo(false);
        statusRepository.saveAndFlush(customizado);
        assertThatThrownBy(() -> service.alterarStatus(ordem.getId(), customizadoRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inativo");
    }

    @Test
    void deveFalharExplicitamenteQuandoStatusInicialEstiverInativo() {
        OrcamentoResponse orcamento = criarOrcamentoAprovado("Inicial inativo");
        StatusOrdemServico inicial = statusRepository
                .findByCodigo("COMPRAR_MATERIAL").orElseThrow();
        inicial.setAtivo(false);
        statusRepository.saveAndFlush(inicial);

        assertThatThrownBy(() -> service.salvar(
                orcamento.getId(), orcamento.getVersaoAtual().getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("está inativo");
    }

    @Test
    void devePreservarIntegralmenteDocumentoComercial() {
        OrcamentoResponse orcamento = criarOrcamento("Separação");
        Long versaoId = orcamento.getVersaoAtual().getId();
        DespesaOrcamentoRequest despesa = new DespesaOrcamentoRequest();
        despesa.setDescricao("Custo previsto");
        despesa.setValor(new BigDecimal("37.50"));
        despesaService.salvar(orcamento.getId(), versaoId, despesa);
        alterarStatusComercial(orcamento.getId(), versaoId, "ENVIADO");
        alterarStatusComercial(orcamento.getId(), versaoId, "APROVADO");
        OrcamentoVersaoResponse antes = versaoService.buscarPorId(orcamento.getId(), versaoId);

        var ordem = service.salvar(orcamento.getId(), versaoId);
        alterarStatusOperacional(ordem.getId(), "EM_EXECUCAO");
        alterarStatusOperacional(ordem.getId(), "INSTALAR");
        alterarStatusOperacional(ordem.getId(), "CONCLUIDO");
        OrcamentoVersaoResponse depois = versaoService.buscarPorId(orcamento.getId(), versaoId);

        assertThat(depois.getStatus().getCodigo()).isEqualTo("APROVADO");
        assertThat(depois.getObservacao()).isEqualTo(antes.getObservacao());
        assertThat(depois.getTotalComercial()).isEqualByComparingTo(antes.getTotalComercial());
        assertThat(depois.getCustoTotalDespesas())
                .isEqualByComparingTo(antes.getCustoTotalDespesas());
        assertThat(depois.getMargemPrevista()).isEqualByComparingTo(antes.getMargemPrevista());
        assertThat(despesaService.listar(orcamento.getId(), versaoId)).hasSize(1);
    }

    @Test
    void deveListarSemNMaisUmEOrdenarPeloNumero() {
        salvarOrigemAprovada(criarOrcamentoAprovado("Lista 1"));
        salvarOrigemAprovada(criarOrcamentoAprovado("Lista 2"));

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        var ordens = service.listar();

        assertThat(ordens).isSortedAccordingTo(
                java.util.Comparator.comparing(response -> response.getNumero()));
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(1);
    }

    private OrcamentoResponse criarOrcamentoAprovado(String nome) {
        OrcamentoResponse orcamento = criarOrcamento(nome);
        Long versaoId = orcamento.getVersaoAtual().getId();
        alterarStatusComercial(orcamento.getId(), versaoId, "ENVIADO");
        alterarStatusComercial(orcamento.getId(), versaoId, "APROVADO");
        return orcamento;
    }

    private br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrdemServicoResponse
            salvarOrigemAprovada(OrcamentoResponse orcamento) {
        return service.salvar(orcamento.getId(), orcamento.getVersaoAtual().getId());
    }

    private OrcamentoResponse criarOrcamento(String nome) {
        Cliente cliente = clienteRepository.saveAndFlush(Cliente.builder()
                .nome(nome + " " + UUID.randomUUID()).build());
        OrcamentoRequest request = new OrcamentoRequest();
        request.setClienteId(cliente.getId());
        request.setObservacao("Observação comercial");
        return orcamentoService.salvar(request);
    }

    private void alterarStatusComercial(Long orcamentoId, Long versaoId, String codigo) {
        OrcamentoVersaoStatusRequest request = new OrcamentoVersaoStatusRequest();
        request.setStatusOrcamentoId(
                statusOrcamentoRepository.findByCodigo(codigo).orElseThrow().getId());
        versaoService.alterarStatus(orcamentoId, versaoId, request);
    }

    private br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrdemServicoResponse
            alterarStatusOperacional(Long ordemId, String codigo) {
        return service.alterarStatus(ordemId, statusRequest(codigo));
    }

    private OrdemServicoStatusRequest statusRequest(String codigo) {
        OrdemServicoStatusRequest request = new OrdemServicoStatusRequest();
        request.setStatusOrdemServicoId(
                statusRepository.findByCodigo(codigo).orElseThrow().getId());
        return request;
    }

    private void assertObservacaoEditavel(Long ordemId, String valor) {
        assertThat(service.atualizar(ordemId, observacao(valor)).getObservacao())
                .isEqualTo(valor);
    }

    private OrdemServicoUpdateRequest observacao(String valor) {
        OrdemServicoUpdateRequest request = new OrdemServicoUpdateRequest();
        request.setObservacao(valor);
        return request;
    }
}
