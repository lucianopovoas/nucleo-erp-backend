package br.com.nucleodasreformas.nucleoerp.despesa_orcamento.service;

import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.dto.DespesaOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.entity.DespesaOrcamento;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.repository.DespesaOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.service.OrcamentoVersaoGuard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DespesaOrcamentoServiceTest {

    @Mock private DespesaOrcamentoRepository repository;
    @Mock private OrcamentoVersaoGuard versaoGuard;
    @InjectMocks private DespesaOrcamentoService service;

    @Test
    void deveAceitarZeroEPreservarEscalaMonetariaInformada() {
        prepararCriacao();

        DespesaOrcamentoResponse response = service.salvar(1L, 2L, request("  Frete  ", "0.00"));

        assertThat(response.getDescricao()).isEqualTo("Frete");
        assertThat(response.getValor()).isEqualByComparingTo("0.00");
        assertThat(response.getValor().scale()).isEqualTo(2);
    }

    @Test
    void deveAceitarValorComUmaCasaSemArredondarSilenciosamente() {
        prepararCriacao();

        DespesaOrcamentoResponse response = service.salvar(1L, 2L, request("Frete", "10.5"));

        assertThat(response.getValor()).isEqualByComparingTo("10.5");
        assertThat(response.getValor().scale()).isEqualTo(1);
    }

    @Test
    void deveRejeitarValorNegativo() {
        when(versaoGuard.bloquearEditavel(1L, 2L)).thenReturn(versao());

        assertThatThrownBy(() -> service.salvar(1L, 2L, request("Frete", "-0.01")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O valor não pode ser negativo.");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveRejeitarEscalaSuperiorADuasCasasSemArredondar() {
        when(versaoGuard.bloquearEditavel(1L, 2L)).thenReturn(versao());

        assertThatThrownBy(() -> service.salvar(1L, 2L, request("Frete", "10.001")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O valor deve ter no máximo 2 casas decimais.");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void devePreservarCamposOmitidosNaAtualizacaoParcial() {
        DespesaOrcamento despesa = DespesaOrcamento.builder()
                .id(10L).orcamentoVersao(versao()).descricao("Frete")
                .valor(new BigDecimal("25.00")).build();
        when(repository.findByIdAndOrcamentoVersaoIdForUpdate(10L, 2L)).thenReturn(Optional.of(despesa));
        when(repository.saveAndFlush(despesa)).thenReturn(despesa);

        DespesaOrcamentoResponse response = service.atualizar(
                1L, 2L, 10L, new DespesaOrcamentoUpdateRequest());

        assertThat(response.getDescricao()).isEqualTo("Frete");
        assertThat(response.getValor()).isEqualByComparingTo("25.00");
    }

    @Test
    void deveRejeitarDescricaoNulaQuandoExplicitamenteEnviada() {
        DespesaOrcamento despesa = DespesaOrcamento.builder()
                .id(10L).orcamentoVersao(versao()).descricao("Frete")
                .valor(new BigDecimal("25.00")).build();
        DespesaOrcamentoUpdateRequest request = new DespesaOrcamentoUpdateRequest();
        request.setDescricao(null);
        when(repository.findByIdAndOrcamentoVersaoIdForUpdate(10L, 2L)).thenReturn(Optional.of(despesa));

        assertThatThrownBy(() -> service.atualizar(1L, 2L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não pode ser nula ou vazia");
    }

    private void prepararCriacao() {
        when(versaoGuard.bloquearEditavel(1L, 2L)).thenReturn(versao());
        when(repository.saveAndFlush(any(DespesaOrcamento.class))).thenAnswer(invocation -> {
            DespesaOrcamento despesa = invocation.getArgument(0);
            despesa.setId(10L);
            return despesa;
        });
    }

    private DespesaOrcamentoRequest request(String descricao, String valor) {
        DespesaOrcamentoRequest request = new DespesaOrcamentoRequest();
        request.setDescricao(descricao);
        request.setValor(new BigDecimal(valor));
        return request;
    }

    private OrcamentoVersao versao() {
        return OrcamentoVersao.builder().id(2L).build();
    }
}
