package br.com.nucleodasreformas.nucleoerp.orcamento_versao.service;

import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.cliente.repository.ClienteRepository;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.service.DespesaOrcamentoService;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento.dto.OrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.orcamento.repository.OrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.orcamento.service.OrcamentoService;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.dto.OrcamentoVersaoStatusRequest;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.repository.OrcamentoVersaoRepository;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.repository.StatusOrcamentoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
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
class OrcamentoVersaoConcorrenciaIntegrationTest {

    @Autowired private ClienteRepository clienteRepository;
    @Autowired private OrcamentoService orcamentoService;
    @Autowired private OrcamentoVersaoService versaoService;
    @Autowired private StatusOrcamentoRepository statusRepository;
    @Autowired private OrcamentoRepository orcamentoRepository;
    @Autowired private OrcamentoVersaoRepository versaoRepository;
    @Autowired private DespesaOrcamentoService despesaService;
    @Autowired private TransactionTemplate transactionTemplate;

    @Test
    void deveSerializarCriacoesConcorrentesDaMesmaOrigem() throws Exception {
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nome("Concorrência " + UUID.randomUUID()).build());
        OrcamentoRequest request = new OrcamentoRequest();
        request.setClienteId(cliente.getId());
        OrcamentoResponse orcamento = orcamentoService.salvar(request);
        Long v1 = orcamento.getVersaoAtual().getId();

        OrcamentoVersaoStatusRequest enviado = new OrcamentoVersaoStatusRequest();
        enviado.setStatusOrcamentoId(
                statusRepository.findByCodigo("ENVIADO").orElseThrow().getId());
        versaoService.alterarStatus(orcamento.getId(), v1, enviado);

        CountDownLatch largada = new CountDownLatch(1);
        Callable<Boolean> tarefa = () -> {
            largada.await(5, TimeUnit.SECONDS);
            try {
                versaoService.criarNovaVersao(orcamento.getId(), v1);
                return true;
            } catch (RuntimeException ex) {
                return false;
            }
        };

        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> resultados = List.of(
                    executor.submit(tarefa), executor.submit(tarefa));
            largada.countDown();
            long sucessos = 0;
            for (Future<Boolean> resultado : resultados) {
                if (resultado.get(15, TimeUnit.SECONDS)) {
                    sucessos++;
                }
            }
            assertThat(sucessos).isEqualTo(1);
        }

        assertThat(versaoService.listar(orcamento.getId()))
                .extracting(response -> response.getNumeroVersao())
                .containsExactly(1, 2);
    }

    @Test
    void deveImpedirEscritaQueAguardavaEnquantoVersaoEraEnviada() throws Exception {
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nome("Concorrência envio " + UUID.randomUUID()).build());
        OrcamentoRequest request = new OrcamentoRequest();
        request.setClienteId(cliente.getId());
        OrcamentoResponse orcamento = orcamentoService.salvar(request);
        Long versaoId = orcamento.getVersaoAtual().getId();

        CountDownLatch statusAlterado = new CountDownLatch(1);
        CountDownLatch liberarCommit = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<?> envio = executor.submit(() -> transactionTemplate.executeWithoutResult(ignored -> {
                orcamentoRepository.findByIdForUpdate(orcamento.getId()).orElseThrow();
                var versao = versaoRepository.findByIdAndOrcamentoIdForUpdate(
                        versaoId, orcamento.getId()).orElseThrow();
                versao.setStatusOrcamento(statusRepository.findByCodigo("ENVIADO").orElseThrow());
                versaoRepository.saveAndFlush(versao);
                statusAlterado.countDown();
                try {
                    liberarCommit.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                }
            }));
            assertThat(statusAlterado.await(5, TimeUnit.SECONDS)).isTrue();

            Future<Boolean> escrita = executor.submit(() -> {
                DespesaOrcamentoRequest despesa = new DespesaOrcamentoRequest();
                despesa.setDescricao("Não pode atravessar o envio");
                despesa.setValor(new BigDecimal("1.00"));
                try {
                    despesaService.salvar(orcamento.getId(), versaoId, despesa);
                    return true;
                } catch (RuntimeException ex) {
                    return false;
                }
            });

            assertThatThrownBy(() -> escrita.get(300, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            liberarCommit.countDown();
            envio.get(5, TimeUnit.SECONDS);
            assertThat(escrita.get(5, TimeUnit.SECONDS)).isFalse();
        }

        assertThat(despesaService.listar(orcamento.getId(), versaoId)).isEmpty();
        assertThat(versaoService.buscarPorId(orcamento.getId(), versaoId)
                .getStatus().getCodigo()).isEqualTo("ENVIADO");
    }
}
