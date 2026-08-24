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
import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrdemServicoFiltroRequest;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrdemServicoStatusRequest;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrdemServicoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.repository.StatusOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.entity.StatusOrdemServico;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.repository.StatusOrdemServicoRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
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
    @Autowired private EntityManager entityManager;
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
        Cliente cliente = criarCliente("Cliente da lista performática");
        salvarOrigemAprovada(criarOrcamentoAprovado(cliente, "Lista 1"));
        salvarOrigemAprovada(criarOrcamentoAprovado(cliente, "Lista 2"));

        entityManager.flush();
        entityManager.clear();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        OrdemServicoFiltroRequest filtroStatus = filtros();
        filtroStatus.setStatus("COMPRAR_MATERIAL");
        filtroStatus.setClienteId(cliente.getId());
        var ordens = service.listar(filtroStatus);

        assertThat(ordens).isSortedAccordingTo(
                java.util.Comparator.comparing(response -> response.getNumero()));
        assertThat(ordens).hasSize(2);
        assertThat(ordens).extracting(response -> response.getId()).doesNotHaveDuplicates();
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    @Test
    void deveListarTodasSemFiltrosEOrdenarPeloNumero() {
        var primeira = salvarOrigemAprovada(criarOrcamentoAprovado("Sem filtro 1"));
        var segunda = salvarOrigemAprovada(criarOrcamentoAprovado("Sem filtro 2"));

        var ordens = service.listar(filtros());

        assertThat(ordens).isSortedAccordingTo(
                java.util.Comparator.comparing(response -> response.getNumero()));
        assertThat(ordens).extracting(response -> response.getId())
                .contains(primeira.getId(), segunda.getId());
    }

    @Test
    void deveFiltrarPorNumeroEManterRespostaDeColecao() {
        var primeira = salvarOrigemAprovada(criarOrcamentoAprovado("Número 1"));
        salvarOrigemAprovada(criarOrcamentoAprovado("Número 2"));

        OrdemServicoFiltroRequest porNumero = filtros();
        porNumero.setNumero(primeira.getNumero());
        assertThat(service.listar(porNumero))
                .extracting(response -> response.getId())
                .containsExactly(primeira.getId());

        OrdemServicoFiltroRequest inexistente = filtros();
        inexistente.setNumero(Long.MAX_VALUE);
        assertThat(service.listar(inexistente)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"COMPRAR_MATERIAL", "EM_EXECUCAO", "INSTALAR", "CONCLUIDO"})
    void deveFiltrarPorStatusNormalizado(String codigo) {
        Cliente cliente = criarCliente("Cliente status " + codigo);
        var ordem = levarAoStatus(
                salvarOrigemAprovada(criarOrcamentoAprovado(cliente, "Status " + codigo)), codigo);

        OrdemServicoFiltroRequest filtros = filtros();
        filtros.setStatus("  " + codigo.toLowerCase(Locale.ROOT) + "  ");

        var resultado = service.listar(filtros);
        assertThat(resultado).extracting(response -> response.getId()).contains(ordem.getId());
        assertThat(resultado).allMatch(response -> codigo.equals(response.getStatus().getCodigo()));
    }

    @Test
    void deveRetornarVazioParaStatusInexistente() {
        salvarOrigemAprovada(criarOrcamentoAprovado("Status inexistente"));
        OrdemServicoFiltroRequest filtros = filtros();
        filtros.setStatus("STATUS_QUE_NAO_EXISTE");

        assertThat(service.listar(filtros)).isEmpty();
    }

    @Test
    void devePermitirStatusInativoComoFiltroHistorico() {
        Cliente cliente = criarCliente("Cliente status inativo");
        var ordem = salvarOrigemAprovada(criarOrcamentoAprovado(cliente, "Status inativo"));
        StatusOrdemServico status = statusRepository.findByCodigo("COMPRAR_MATERIAL").orElseThrow();
        status.setAtivo(false);
        statusRepository.saveAndFlush(status);
        entityManager.clear();

        OrdemServicoFiltroRequest filtros = filtros();
        filtros.setStatus("comprar_material");

        var resultado = service.listar(filtros);
        assertThat(resultado).extracting(response -> response.getId()).contains(ordem.getId());
        assertThat(resultado).allMatch(
                response -> "COMPRAR_MATERIAL".equals(response.getStatus().getCodigo()));
    }

    @Test
    void deveFiltrarPelasRelacoesDeClienteEOrcamento() {
        Cliente cliente = criarCliente("Cliente com ordens");
        OrcamentoResponse primeiroOrcamento = criarOrcamentoAprovado(cliente, "Primeiro");
        OrcamentoResponse segundoOrcamento = criarOrcamentoAprovado(cliente, "Segundo");
        var primeira = salvarOrigemAprovada(primeiroOrcamento);
        var segunda = salvarOrigemAprovada(segundoOrcamento);
        salvarOrigemAprovada(criarOrcamentoAprovado("Outro cliente"));

        OrdemServicoFiltroRequest porCliente = filtros();
        porCliente.setClienteId(cliente.getId());
        assertThat(service.listar(porCliente))
                .extracting(response -> response.getId())
                .containsExactly(primeira.getId(), segunda.getId());

        OrdemServicoFiltroRequest porOrcamento = filtros();
        porOrcamento.setOrcamentoId(segundoOrcamento.getId());
        assertThat(service.listar(porOrcamento))
                .extracting(response -> response.getId())
                .containsExactly(segunda.getId());

        Cliente semOrdem = criarCliente("Cliente sem ordem");
        OrdemServicoFiltroRequest clienteSemOrdens = filtros();
        clienteSemOrdens.setClienteId(semOrdem.getId());
        assertThat(service.listar(clienteSemOrdens)).isEmpty();
    }

    @Test
    void deveAplicarLimitesDeDataComInicioInclusivoEFimExclusivo() {
        Cliente cliente = criarCliente("Cliente das datas");
        var noInicio = salvarOrigemAprovada(criarOrcamentoAprovado(cliente, "Data início"));
        var dentroDoFim = salvarOrigemAprovada(criarOrcamentoAprovado(cliente, "Data fim"));
        var noDiaSeguinte = salvarOrigemAprovada(criarOrcamentoAprovado(cliente, "Data seguinte"));
        definirCriadoEm(noInicio.getId(), LocalDateTime.of(2026, 8, 1, 0, 0));
        definirCriadoEm(dentroDoFim.getId(), LocalDateTime.of(2026, 8, 31, 23, 59, 59));
        definirCriadoEm(noDiaSeguinte.getId(), LocalDateTime.of(2026, 9, 1, 0, 0));
        entityManager.clear();

        OrdemServicoFiltroRequest somenteDe = filtros();
        somenteDe.setCriadoDe(LocalDate.of(2026, 8, 1));
        assertThat(service.listar(somenteDe))
                .extracting(response -> response.getId())
                .contains(noInicio.getId(), dentroDoFim.getId(), noDiaSeguinte.getId());

        OrdemServicoFiltroRequest somenteAte = filtros();
        somenteAte.setCriadoAte(LocalDate.of(2026, 8, 31));
        assertThat(service.listar(somenteAte))
                .extracting(response -> response.getId())
                .contains(noInicio.getId(), dentroDoFim.getId())
                .doesNotContain(noDiaSeguinte.getId());

        OrdemServicoFiltroRequest intervalo = filtros();
        intervalo.setCriadoDe(LocalDate.of(2026, 8, 1));
        intervalo.setCriadoAte(LocalDate.of(2026, 8, 31));
        assertThat(service.listar(intervalo))
                .extracting(response -> response.getId())
                .contains(noInicio.getId(), dentroDoFim.getId())
                .doesNotContain(noDiaSeguinte.getId());
    }

    @Test
    void deveRejeitarIntervaloDeDatasInvertido() {
        OrdemServicoFiltroRequest filtros = filtros();
        filtros.setCriadoDe(LocalDate.of(2026, 8, 31));
        filtros.setCriadoAte(LocalDate.of(2026, 8, 1));

        assertThatThrownBy(() -> service.listar(filtros))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A data inicial de criação não pode ser posterior à data final.");
    }

    @Test
    void deveCombinarFiltrosComAnd() {
        Cliente cliente = criarCliente("Cliente combinado");
        OrcamentoResponse primeiroOrcamento = criarOrcamentoAprovado(cliente, "Combinado 1");
        OrcamentoResponse segundoOrcamento = criarOrcamentoAprovado(cliente, "Combinado 2");
        var instalar = levarAoStatus(salvarOrigemAprovada(primeiroOrcamento), "INSTALAR");
        var comprar = salvarOrigemAprovada(segundoOrcamento);
        var outra = salvarOrigemAprovada(criarOrcamentoAprovado("Outro combinado"));
        definirCriadoEm(instalar.getId(), LocalDateTime.of(2026, 8, 15, 10, 0));
        definirCriadoEm(comprar.getId(), LocalDateTime.of(2026, 7, 15, 10, 0));
        definirCriadoEm(outra.getId(), LocalDateTime.of(2026, 8, 15, 10, 0));
        entityManager.clear();

        OrdemServicoFiltroRequest statusECliente = filtros();
        statusECliente.setStatus("INSTALAR");
        statusECliente.setClienteId(cliente.getId());
        assertThat(service.listar(statusECliente))
                .extracting(response -> response.getId())
                .containsExactly(instalar.getId());

        OrdemServicoFiltroRequest statusEIntervalo = filtros();
        statusEIntervalo.setStatus("INSTALAR");
        statusEIntervalo.setCriadoDe(LocalDate.of(2026, 8, 1));
        statusEIntervalo.setCriadoAte(LocalDate.of(2026, 8, 31));
        statusEIntervalo.setClienteId(cliente.getId());
        assertThat(service.listar(statusEIntervalo))
                .extracting(response -> response.getId())
                .containsExactly(instalar.getId());

        OrdemServicoFiltroRequest clienteEOrcamento = filtros();
        clienteEOrcamento.setClienteId(cliente.getId());
        clienteEOrcamento.setOrcamentoId(segundoOrcamento.getId());
        assertThat(service.listar(clienteEOrcamento))
                .extracting(response -> response.getId())
                .containsExactly(comprar.getId());

        OrdemServicoFiltroRequest semResultado = filtros();
        semResultado.setClienteId(cliente.getId());
        semResultado.setOrcamentoId(outra.getOrigem().getOrcamento().getId());
        assertThat(service.listar(semResultado)).isEmpty();
    }

    private OrcamentoResponse criarOrcamentoAprovado(String nome) {
        OrcamentoResponse orcamento = criarOrcamento(nome);
        aprovar(orcamento);
        return orcamento;
    }

    private OrcamentoResponse criarOrcamentoAprovado(Cliente cliente, String nome) {
        OrcamentoResponse orcamento = criarOrcamento(cliente, nome);
        aprovar(orcamento);
        return orcamento;
    }

    private void aprovar(OrcamentoResponse orcamento) {
        Long versaoId = orcamento.getVersaoAtual().getId();
        alterarStatusComercial(orcamento.getId(), versaoId, "ENVIADO");
        alterarStatusComercial(orcamento.getId(), versaoId, "APROVADO");
    }

    private br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrdemServicoResponse
            salvarOrigemAprovada(OrcamentoResponse orcamento) {
        return service.salvar(orcamento.getId(), orcamento.getVersaoAtual().getId());
    }

    private OrcamentoResponse criarOrcamento(String nome) {
        return criarOrcamento(criarCliente(nome), nome);
    }

    private OrcamentoResponse criarOrcamento(Cliente cliente, String nome) {
        OrcamentoRequest request = new OrcamentoRequest();
        request.setClienteId(cliente.getId());
        request.setObservacao("Observação comercial " + nome);
        return orcamentoService.salvar(request);
    }

    private Cliente criarCliente(String nome) {
        return clienteRepository.saveAndFlush(Cliente.builder()
                .nome(nome + " " + UUID.randomUUID()).build());
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

    private OrdemServicoFiltroRequest filtros() {
        return new OrdemServicoFiltroRequest();
    }

    private br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrdemServicoResponse
            levarAoStatus(
                    br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrdemServicoResponse ordem,
                    String codigo) {
        if ("COMPRAR_MATERIAL".equals(codigo)) {
            return ordem;
        }
        ordem = alterarStatusOperacional(ordem.getId(), "EM_EXECUCAO");
        if ("EM_EXECUCAO".equals(codigo)) {
            return ordem;
        }
        ordem = alterarStatusOperacional(ordem.getId(), "INSTALAR");
        if ("INSTALAR".equals(codigo)) {
            return ordem;
        }
        return alterarStatusOperacional(ordem.getId(), "CONCLUIDO");
    }

    private void definirCriadoEm(Long ordemId, LocalDateTime criadoEm) {
        entityManager.createNativeQuery("""
                        UPDATE ordem_servico
                        SET criado_em = :criadoEm
                        WHERE id = :ordemId
                        """)
                .setParameter("criadoEm", criadoEm)
                .setParameter("ordemId", ordemId)
                .executeUpdate();
    }
}
