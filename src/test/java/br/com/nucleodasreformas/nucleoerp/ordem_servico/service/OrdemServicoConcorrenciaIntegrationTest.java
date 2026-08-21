package br.com.nucleodasreformas.nucleoerp.ordem_servico.service;

import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.cliente.repository.ClienteRepository;
import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento.repository.OrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.orcamento.service.OrcamentoService;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.dto.OrcamentoVersaoStatusRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.repository.OrcamentoVersaoRepository;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.service.OrcamentoVersaoService;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.dto.OrdemServicoResponse;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.repository.OrdemServicoRepository;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.repository.StatusOrcamentoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class OrdemServicoConcorrenciaIntegrationTest {

    @Autowired private ClienteRepository clienteRepository;
    @Autowired private OrcamentoService orcamentoService;
    @Autowired private OrcamentoVersaoService versaoService;
    @Autowired private StatusOrcamentoRepository statusRepository;
    @Autowired private OrcamentoRepository orcamentoRepository;
    @Autowired private OrcamentoVersaoRepository versaoRepository;
    @Autowired private OrdemServicoService service;
    @Autowired private OrdemServicoRepository ordemRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    @Test
    void devePersistirSomenteUmaOrdemParaMesmaVersao() throws Exception {
        OrcamentoResponse orcamento = criarOrcamentoAprovado("Mesma origem");
        CountDownLatch largada = new CountDownLatch(1);
        Callable<String> tarefa = () -> {
            largada.await(5, TimeUnit.SECONDS);
            try {
                service.salvar(orcamento.getId(), orcamento.getVersaoAtual().getId());
                return "SUCESSO";
            } catch (BusinessException ex) {
                return "NEGOCIO";
            }
        };

        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Future<String>> resultados = List.of(
                    executor.submit(tarefa), executor.submit(tarefa));
            largada.countDown();
            assertThat(resultados.get(0).get(15, TimeUnit.SECONDS))
                    .isIn("SUCESSO", "NEGOCIO");
            assertThat(resultados.get(1).get(15, TimeUnit.SECONDS))
                    .isIn("SUCESSO", "NEGOCIO");
            assertThat(resultados.stream().map(futuro -> obter(futuro, 1)).toList())
                    .containsExactlyInAnyOrder("SUCESSO", "NEGOCIO");
        }

        assertThat(ordemRepository.existsByOrcamentoVersao_Id(
                orcamento.getVersaoAtual().getId())).isTrue();
    }

    @Test
    void deveGerarNumerosDistintosParaOrigensConcorrentes() throws Exception {
        OrcamentoResponse primeiro = criarOrcamentoAprovado("Número um");
        OrcamentoResponse segundo = criarOrcamentoAprovado("Número dois");
        CountDownLatch largada = new CountDownLatch(1);
        Callable<OrdemServicoResponse> primeiraTarefa = () -> {
            largada.await(5, TimeUnit.SECONDS);
            return service.salvar(primeiro.getId(), primeiro.getVersaoAtual().getId());
        };
        Callable<OrdemServicoResponse> segundaTarefa = () -> {
            largada.await(5, TimeUnit.SECONDS);
            return service.salvar(segundo.getId(), segundo.getVersaoAtual().getId());
        };

        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<OrdemServicoResponse> um = executor.submit(primeiraTarefa);
            Future<OrdemServicoResponse> dois = executor.submit(segundaTarefa);
            largada.countDown();
            assertThat(um.get(15, TimeUnit.SECONDS).getNumero())
                    .isNotEqualTo(dois.get(15, TimeUnit.SECONDS).getNumero());
        }
    }

    @Test
    void deveAguardarAprovacaoConcorrenteAntesDeCriar() throws Exception {
        OrcamentoResponse orcamento = criarOrcamento("Aprovação concorrente");
        Long versaoId = orcamento.getVersaoAtual().getId();
        alterarStatus(orcamento.getId(), versaoId, "ENVIADO");

        CountDownLatch aprovado = new CountDownLatch(1);
        CountDownLatch liberarCommit = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<?> aprovacao = executor.submit(() -> transactionTemplate.executeWithoutResult(ignored -> {
                orcamentoRepository.findByIdForUpdate(orcamento.getId()).orElseThrow();
                var versao = versaoRepository.findByIdAndOrcamentoIdForUpdate(
                        versaoId, orcamento.getId()).orElseThrow();
                versao.setStatusOrcamento(statusRepository.findByCodigo("APROVADO").orElseThrow());
                versaoRepository.saveAndFlush(versao);
                aprovado.countDown();
                aguardar(liberarCommit);
            }));
            assertThat(aprovado.await(5, TimeUnit.SECONDS)).isTrue();

            Future<OrdemServicoResponse> criacao = executor.submit(
                    () -> service.salvar(orcamento.getId(), versaoId));
            assertThatThrownBy(() -> criacao.get(300, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);

            liberarCommit.countDown();
            aprovacao.get(5, TimeUnit.SECONDS);
            assertThat(criacao.get(5, TimeUnit.SECONDS).getStatus().getCodigo())
                    .isEqualTo("COMPRAR_MATERIAL");
        }
    }

    private OrcamentoResponse criarOrcamentoAprovado(String nome) {
        OrcamentoResponse orcamento = criarOrcamento(nome);
        Long versaoId = orcamento.getVersaoAtual().getId();
        alterarStatus(orcamento.getId(), versaoId, "ENVIADO");
        alterarStatus(orcamento.getId(), versaoId, "APROVADO");
        return orcamento;
    }

    private OrcamentoResponse criarOrcamento(String nome) {
        Cliente cliente = clienteRepository.saveAndFlush(Cliente.builder()
                .nome(nome + " " + UUID.randomUUID()).build());
        OrcamentoRequest request = new OrcamentoRequest();
        request.setClienteId(cliente.getId());
        return orcamentoService.salvar(request);
    }

    private void alterarStatus(Long orcamentoId, Long versaoId, String codigo) {
        OrcamentoVersaoStatusRequest request = new OrcamentoVersaoStatusRequest();
        request.setStatusOrcamentoId(statusRepository.findByCodigo(codigo).orElseThrow().getId());
        versaoService.alterarStatus(orcamentoId, versaoId, request);
    }

    private void aguardar(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    private String obter(Future<String> futuro, int segundos) {
        try {
            return futuro.get(segundos, TimeUnit.SECONDS);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
