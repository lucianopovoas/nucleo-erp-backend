package br.com.nucleodasreformas.nucleoerp.orcamento_versao.mapper;

import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.service.TotaisOrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.entity.StatusOrcamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OrcamentoVersaoMapperTest {

    @Test
    void deveCriarEntidadeComRaizStatusNumeroEObservacao() {
        Orcamento raiz = Orcamento.builder().id(10L).build();
        StatusOrcamento status = StatusOrcamento.builder().id(1L).codigo("RASCUNHO").build();

        OrcamentoVersao versao = OrcamentoVersaoMapper.toEntity(
                raiz, 2, status, "Proposta revisada");

        assertThat(versao.getOrcamento()).isSameAs(raiz);
        assertThat(versao.getStatusOrcamento()).isSameAs(status);
        assertThat(versao.getNumeroVersao()).isEqualTo(2);
        assertThat(versao.getObservacao()).isEqualTo("Proposta revisada");
    }

    @Test
    void deveConverterStatusTotaisEDataParaResponse() {
        LocalDateTime criadoEm = LocalDateTime.of(2026, 8, 23, 10, 0);
        OrcamentoVersao versao = OrcamentoVersao.builder()
                .id(20L).numeroVersao(2).observacao("Proposta")
                .statusOrcamento(StatusOrcamento.builder()
                        .id(1L).codigo("RASCUNHO").nome("Rascunho").build())
                .criadoEm(criadoEm).build();
        TotaisOrcamentoVersao totais = new TotaisOrcamentoVersao(
                valor("1000.00"), valor("300.00"), valor("200.00"),
                valor("100.00"), valor("400.00"), valor("40.00"));

        var response = OrcamentoVersaoMapper.toResponse(versao, totais);

        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getNumeroVersao()).isEqualTo(2);
        assertThat(response.getStatus().getCodigo()).isEqualTo("RASCUNHO");
        assertThat(response.getTotalComercial()).isEqualByComparingTo("1000.00");
        assertThat(response.getCustoTotalMateriais()).isEqualByComparingTo("300.00");
        assertThat(response.getCustoTotalMaoDeObra()).isEqualByComparingTo("200.00");
        assertThat(response.getCustoTotalDespesas()).isEqualByComparingTo("100.00");
        assertThat(response.getMargemPrevista()).isEqualByComparingTo("400.00");
        assertThat(response.getPercentualMargem()).isEqualByComparingTo("40.00");
        assertThat(response.getCriadoEm()).isEqualTo(criadoEm);
    }

    private BigDecimal valor(String valor) {
        return new BigDecimal(valor);
    }
}
