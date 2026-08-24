package br.com.nucleodasreformas.nucleoerp.orcamento_versao.service;

import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.repository.CustoTotalDespesasOrcamentoProjection;
import br.com.nucleodasreformas.nucleoerp.despesa_orcamento.repository.DespesaOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.repository.ItemOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.item_orcamento.repository.TotalComercialOrcamentoProjection;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.repository.CustoTotalMaoDeObraOrcamentoProjection;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.repository.MaoDeObraOrcamentoRepository;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.repository.CustoTotalMateriaisOrcamentoProjection;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.repository.MaterialOrcamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrcamentoVersaoTotaisServiceTest {

    @Mock private ItemOrcamentoRepository itemRepository;
    @Mock private MaterialOrcamentoRepository materialRepository;
    @Mock private MaoDeObraOrcamentoRepository maoDeObraRepository;
    @Mock private DespesaOrcamentoRepository despesaRepository;
    @InjectMocks private OrcamentoVersaoTotaisService service;

    @Test
    void deveCalcularTotaisMargemAbsolutaEPercentual() {
        preparar(1L, "1000.00", "250.00", "125.00", "25.00");

        TotaisOrcamentoVersao totais = service.buscarPorVersao(1L);

        assertThat(totais.totalComercial()).isEqualByComparingTo("1000.00");
        assertThat(totais.custoTotalMateriais()).isEqualByComparingTo("250.00");
        assertThat(totais.custoTotalMaoDeObra()).isEqualByComparingTo("125.00");
        assertThat(totais.custoTotalDespesas()).isEqualByComparingTo("25.00");
        assertThat(totais.margemPrevista()).isEqualByComparingTo("600.00");
        assertThat(totais.percentualMargem()).isEqualByComparingTo("60.00");
    }

    @Test
    void deveCalcularMargemNegativaEPercentualNegativo() {
        preparar(1L, "100.00", "80.00", "50.00", "20.00");

        TotaisOrcamentoVersao totais = service.buscarPorVersao(1L);

        assertThat(totais.margemPrevista()).isEqualByComparingTo("-50.00");
        assertThat(totais.percentualMargem()).isEqualByComparingTo("-50.00");
    }

    @Test
    void deveManterPercentualZeroQuandoTotalComercialForZero() {
        preparar(1L, "0.00", "10.00", "20.00", "5.00");

        TotaisOrcamentoVersao totais = service.buscarPorVersao(1L);

        assertThat(totais.margemPrevista()).isEqualByComparingTo("-35.00");
        assertThat(totais.percentualMargem()).isEqualByComparingTo("0.00");
        assertThat(totais.percentualMargem().scale()).isEqualTo(2);
    }

    @Test
    void deveArredondarTotaisEMargemPercentualComHalfUp() {
        preparar(1L, "3.335", "1.111", "0", "0");

        TotaisOrcamentoVersao totais = service.buscarPorVersao(1L);

        assertThat(totais.totalComercial()).isEqualByComparingTo("3.34");
        assertThat(totais.custoTotalMateriais()).isEqualByComparingTo("1.11");
        assertThat(totais.margemPrevista()).isEqualByComparingTo("2.23");
        assertThat(totais.percentualMargem()).isEqualByComparingTo("66.77");
    }

    @Test
    void devePreencherCategoriasAusentesComZeroMonetario() {
        when(itemRepository.somarValorTotalPorVersoes(List.of(1L)))
                .thenReturn(List.of(new TotalComercialOrcamentoProjection(1L, new BigDecimal("50.00"))));
        when(materialRepository.somarCustoTotalPorVersoes(List.of(1L))).thenReturn(List.of());
        when(maoDeObraRepository.somarCustoTotalPorVersoes(List.of(1L))).thenReturn(List.of());
        when(despesaRepository.somarValorPorVersoes(List.of(1L))).thenReturn(List.of());

        TotaisOrcamentoVersao totais = service.buscarPorVersao(1L);

        assertThat(totais.custoTotalMateriais()).isEqualByComparingTo("0.00");
        assertThat(totais.custoTotalMaoDeObra()).isEqualByComparingTo("0.00");
        assertThat(totais.custoTotalDespesas()).isEqualByComparingTo("0.00");
        assertThat(totais.margemPrevista()).isEqualByComparingTo("50.00");
    }

    @Test
    void naoDeveConsultarRepositoriosParaColecaoVazia() {
        Map<Long, TotaisOrcamentoVersao> totais = service.buscarPorVersoes(List.of());

        assertThat(totais).isEmpty();
        verifyNoInteractions(itemRepository, materialRepository, maoDeObraRepository, despesaRepository);
    }

    private void preparar(Long id, String comercial, String materiais, String maoDeObra, String despesas) {
        List<Long> ids = List.of(id);
        when(itemRepository.somarValorTotalPorVersoes(ids)).thenReturn(List.of(
                new TotalComercialOrcamentoProjection(id, new BigDecimal(comercial))));
        when(materialRepository.somarCustoTotalPorVersoes(ids)).thenReturn(List.of(
                new CustoTotalMateriaisOrcamentoProjection(id, new BigDecimal(materiais))));
        when(maoDeObraRepository.somarCustoTotalPorVersoes(ids)).thenReturn(List.of(
                new CustoTotalMaoDeObraOrcamentoProjection(id, new BigDecimal(maoDeObra))));
        when(despesaRepository.somarValorPorVersoes(ids)).thenReturn(List.of(
                new CustoTotalDespesasOrcamentoProjection(id, new BigDecimal(despesas))));
    }
}
