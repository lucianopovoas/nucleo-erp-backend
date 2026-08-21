package br.com.nucleodasreformas.nucleoerp.despesa_orcamento.service;

import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.entity.DespesaOrcamento;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.repository.DespesaOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.repository.OrcamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DespesaOrcamentoServiceTest {

    @Mock
    private DespesaOrcamentoRepository repository;

    @Mock
    private OrcamentoRepository orcamentoRepository;

    @InjectMocks
    private DespesaOrcamentoService service;

    @Test
    void deveCriarDespesaComDescricaoAparadaEValorInformado() {
        prepararCriacao();

        DespesaOrcamentoResponse response = service.salvar(
                10L, request("  Frete  ", "180.00"));

        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getDescricao()).isEqualTo("Frete");
        assertThat(response.getValor()).isEqualByComparingTo("180.00");
        assertThat(response.getCriadoEm()).isNotNull();
    }

    @Test
    void devePermitirValorZero() {
        prepararCriacao();

        DespesaOrcamentoResponse response = service.salvar(
                10L, request("Estacionamento cortesia", "0.00"));

        assertThat(response.getValor()).isEqualByComparingTo("0.00");
    }

    @Test
    void deveFalharComOrcamentoInexistente() {
        when(orcamentoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.salvar(99L, request("Frete", "10.00")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Orçamento não encontrado. Id: 99");

        verifyNoInteractions(repository);
    }

    @Test
    void deveRejeitarDescricaoNulaOuVaziaNaCriacao() {
        when(orcamentoRepository.findById(10L)).thenReturn(Optional.of(orcamento(10L)));

        assertThatThrownBy(() -> service.salvar(10L, request(null, "10.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A descrição informada não pode ser nula ou vazia.");
        assertThatThrownBy(() -> service.salvar(10L, request("   ", "10.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A descrição informada não pode ser nula ou vazia.");
    }

    @Test
    void deveRejeitarValorNuloNaCriacao() {
        when(orcamentoRepository.findById(10L)).thenReturn(Optional.of(orcamento(10L)));

        assertThatThrownBy(() -> service.salvar(10L, request("Frete", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O valor é obrigatório.");
    }

    @Test
    void deveRejeitarValorNegativo() {
        when(orcamentoRepository.findById(10L)).thenReturn(Optional.of(orcamento(10L)));

        assertThatThrownBy(() -> service.salvar(10L, request("Frete", "-0.01")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O valor não pode ser negativo.");
    }

    @Test
    void deveRejeitarValorComMaisDeDuasCasasSemArredondar() {
        when(orcamentoRepository.findById(10L)).thenReturn(Optional.of(orcamento(10L)));

        assertThatThrownBy(() -> service.salvar(10L, request("Frete", "10.001")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O valor deve ter no máximo 2 casas decimais.");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void devePermitirDespesasRepetidasNoMesmoOrcamento() {
        when(orcamentoRepository.findById(10L)).thenReturn(Optional.of(orcamento(10L)));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.salvar(10L, request("Frete", "100.00"));
        service.salvar(10L, request("Frete", "50.00"));

        verify(repository, times(2)).saveAndFlush(any());
    }

    @Test
    void deveBuscarSomenteDespesaPertencenteAoOrcamento() {
        when(repository.findByIdAndOrcamento_Id(20L, 10L))
                .thenReturn(Optional.of(registro(20L, 10L)));

        DespesaOrcamentoResponse response = service.buscarPorId(10L, 20L);

        assertThat(response.getId()).isEqualTo(20L);
        verify(repository).findByIdAndOrcamento_Id(20L, 10L);
    }

    @Test
    void deveTratarDespesaDeOutroOrcamentoComoInexistente() {
        when(repository.findByIdAndOrcamento_Id(20L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L, 20L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Despesa do orçamento não encontrada. Id: 20, orçamento: 99");
    }

    @Test
    void deveListarDespesasDoOrcamentoExistente() {
        when(orcamentoRepository.existsById(10L)).thenReturn(true);
        when(repository.findByOrcamento_IdOrderByIdAsc(10L)).thenReturn(List.of(
                registro(20L, 10L), registro(21L, 10L)));

        List<DespesaOrcamentoResponse> responses = service.listar(10L);

        assertThat(responses).extracting(DespesaOrcamentoResponse::getId)
                .containsExactly(20L, 21L);
    }

    @Test
    void deveFalharAoListarOrcamentoInexistente() {
        when(orcamentoRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.listar(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Orçamento não encontrado. Id: 99");

        verifyNoInteractions(repository);
    }

    @Test
    void deveAtualizarDescricaoEValor() {
        DespesaOrcamento registro = registro(20L, 10L);
        DespesaOrcamentoUpdateRequest request = new DespesaOrcamentoUpdateRequest();
        request.setDescricao("  Pedágio e estacionamento  ");
        request.setValor(new BigDecimal("75.50"));
        prepararAtualizacao(registro);

        DespesaOrcamentoResponse response = service.atualizar(10L, 20L, request);

        assertThat(response.getDescricao()).isEqualTo("Pedágio e estacionamento");
        assertThat(response.getValor()).isEqualByComparingTo("75.50");
    }

    @Test
    void devePreservarCamposOmitidosEValorNullNoPut() {
        DespesaOrcamento registro = registro(20L, 10L);
        DespesaOrcamentoUpdateRequest request = new DespesaOrcamentoUpdateRequest();
        request.setValor(null);
        prepararAtualizacao(registro);

        DespesaOrcamentoResponse response = service.atualizar(10L, 20L, request);

        assertThat(response.getDescricao()).isEqualTo("Frete");
        assertThat(response.getValor()).isEqualByComparingTo("180.00");
    }

    @Test
    void deveRejeitarDescricaoExplicitaNulaOuVaziaNoPut() {
        DespesaOrcamento registro = registro(20L, 10L);
        when(repository.findByIdAndOrcamento_Id(20L, 10L)).thenReturn(Optional.of(registro));

        DespesaOrcamentoUpdateRequest nula = new DespesaOrcamentoUpdateRequest();
        nula.setDescricao(null);
        assertThatThrownBy(() -> service.atualizar(10L, 20L, nula))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A descrição informada não pode ser nula ou vazia.");

        DespesaOrcamentoUpdateRequest vazia = new DespesaOrcamentoUpdateRequest();
        vazia.setDescricao("   ");
        assertThatThrownBy(() -> service.atualizar(10L, 20L, vazia))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A descrição informada não pode ser nula ou vazia.");
    }

    @Test
    void deveRejeitarValorNegativoOuEscalaInvalidaNoPut() {
        DespesaOrcamento registro = registro(20L, 10L);
        when(repository.findByIdAndOrcamento_Id(20L, 10L)).thenReturn(Optional.of(registro));

        DespesaOrcamentoUpdateRequest negativo = new DespesaOrcamentoUpdateRequest();
        negativo.setValor(new BigDecimal("-0.01"));
        assertThatThrownBy(() -> service.atualizar(10L, 20L, negativo))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O valor não pode ser negativo.");

        DespesaOrcamentoUpdateRequest escalaInvalida = new DespesaOrcamentoUpdateRequest();
        escalaInvalida.setValor(new BigDecimal("10.001"));
        assertThatThrownBy(() -> service.atualizar(10L, 20L, escalaInvalida))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O valor deve ter no máximo 2 casas decimais.");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveExcluirFisicamenteSomenteDespesaPertencenteAoOrcamento() {
        DespesaOrcamento registro = registro(20L, 10L);
        when(repository.findByIdAndOrcamento_Id(20L, 10L)).thenReturn(Optional.of(registro));

        service.deletar(10L, 20L);

        verify(repository).delete(registro);
        verify(orcamentoRepository, never()).delete(any());
    }

    private void prepararCriacao() {
        when(orcamentoRepository.findById(10L)).thenReturn(Optional.of(orcamento(10L)));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> {
            DespesaOrcamento registro = invocation.getArgument(0);
            registro.setId(20L);
            registro.setCriadoEm(LocalDateTime.of(2026, 8, 21, 12, 0));
            return registro;
        });
    }

    private void prepararAtualizacao(DespesaOrcamento registro) {
        when(repository.findByIdAndOrcamento_Id(20L, 10L)).thenReturn(Optional.of(registro));
        when(repository.saveAndFlush(registro)).thenReturn(registro);
    }

    private DespesaOrcamentoRequest request(String descricao, String valor) {
        DespesaOrcamentoRequest request = new DespesaOrcamentoRequest();
        request.setDescricao(descricao);
        request.setValor(valor == null ? null : new BigDecimal(valor));
        return request;
    }

    private DespesaOrcamento registro(Long id, Long orcamentoId) {
        return DespesaOrcamento.builder()
                .id(id)
                .orcamento(orcamento(orcamentoId))
                .descricao("Frete")
                .valor(new BigDecimal("180.00"))
                .criadoEm(LocalDateTime.of(2026, 8, 21, 12, 0))
                .build();
    }

    private Orcamento orcamento(Long id) {
        return Orcamento.builder().id(id).numero(1000L + id).build();
    }
}
