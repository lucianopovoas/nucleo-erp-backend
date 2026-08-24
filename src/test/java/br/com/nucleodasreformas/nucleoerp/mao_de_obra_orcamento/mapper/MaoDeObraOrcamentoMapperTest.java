package br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.mapper;

import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.entity.MaoDeObraOrcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.entity.UnidadeMaoDeObra;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MaoDeObraOrcamentoMapperTest {

    @Test
    void devePreservarReferenciaEDadosInformadosNaEntidade() {
        OrcamentoVersao versao = OrcamentoVersao.builder().id(20L).build();
        UnidadeMaoDeObra unidade = UnidadeMaoDeObra.builder().id(5L).nome("Hora").build();
        MaoDeObraOrcamento linha = MaoDeObraOrcamentoMapper.toEntity(versao, unidade,
                "Instalação", "Hora", valor("2.0000"), valor("250.00"), valor("500.00"));

        assertThat(linha.getOrcamentoVersao()).isSameAs(versao);
        assertThat(linha.getUnidadeMaoDeObra()).isSameAs(unidade);
        assertThat(linha.getUnidade()).isEqualTo("Hora");
        assertThat(linha.getCustoTotal()).isEqualByComparingTo("500.00");

        MaoDeObraOrcamentoMapper.updateEntity(linha, unidade, "Montagem", "Diária",
                valor("1.0000"), valor("400.00"), valor("400.00"));
        assertThat(linha.getDescricao()).isEqualTo("Montagem");
        assertThat(linha.getUnidade()).isEqualTo("Diária");
    }

    @Test
    void deveManterSnapshotSeparadoDoNomeAtualNoResponse() {
        MaoDeObraOrcamento linha = MaoDeObraOrcamento.builder().id(30L)
                .unidadeMaoDeObra(UnidadeMaoDeObra.builder()
                        .id(5L).nome("Nome atual").build())
                .descricao("Instalação").unidade("Snapshot histórico")
                .quantidade(valor("2.0000")).custoUnitario(valor("250.00"))
                .custoTotal(valor("500.00")).build();

        var response = MaoDeObraOrcamentoMapper.toResponse(linha);

        assertThat(response.getUnidadeMaoDeObra().getNome()).isEqualTo("Nome atual");
        assertThat(response.getUnidade()).isEqualTo("Snapshot histórico");
        assertThat(response.getCustoTotal()).isEqualByComparingTo("500.00");
    }

    private BigDecimal valor(String valor) { return new BigDecimal(valor); }
}
