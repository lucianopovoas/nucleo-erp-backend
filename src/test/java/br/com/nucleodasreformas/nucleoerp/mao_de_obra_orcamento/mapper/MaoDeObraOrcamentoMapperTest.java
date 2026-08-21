package br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.mapper;

import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.dto.MaoDeObraOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.mao_de_obra_orcamento.entity.MaoDeObraOrcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.entity.UnidadeMaoDeObra;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MaoDeObraOrcamentoMapperTest {

    @Test
    void deveCriarEntidadeComSnapshotEValoresResolvidos() {
        Orcamento orcamento = Orcamento.builder().id(10L).build();
        UnidadeMaoDeObra unidade = unidade(5L, "Diária");

        MaoDeObraOrcamento resultado = MaoDeObraOrcamentoMapper.toEntity(
                orcamento,
                unidade,
                "Instalação",
                "Diária",
                new BigDecimal("2.0000"),
                new BigDecimal("250.00"),
                new BigDecimal("500.00"));

        assertThat(resultado.getOrcamento()).isSameAs(orcamento);
        assertThat(resultado.getUnidadeMaoDeObra()).isSameAs(unidade);
        assertThat(resultado.getDescricao()).isEqualTo("Instalação");
        assertThat(resultado.getUnidade()).isEqualTo("Diária");
        assertThat(resultado.getQuantidade()).isEqualByComparingTo("2.0000");
        assertThat(resultado.getCustoUnitario()).isEqualByComparingTo("250.00");
        assertThat(resultado.getCustoTotal()).isEqualByComparingTo("500.00");
        assertThat(resultado.getId()).isNull();
    }

    @Test
    void deveAtualizarSomenteCamposEditaveisEValoresResolvidos() {
        Orcamento orcamento = Orcamento.builder().id(10L).build();
        LocalDateTime criadoEm = LocalDateTime.of(2026, 8, 21, 12, 0);
        MaoDeObraOrcamento registro = MaoDeObraOrcamento.builder()
                .id(20L)
                .orcamento(orcamento)
                .unidadeMaoDeObra(unidade(5L, "Diária"))
                .descricao("Anterior")
                .unidade("Diária")
                .quantidade(BigDecimal.ONE)
                .custoUnitario(BigDecimal.TEN)
                .custoTotal(BigDecimal.TEN)
                .criadoEm(criadoEm)
                .build();
        UnidadeMaoDeObra novaUnidade = unidade(6L, "Hora");

        MaoDeObraOrcamentoMapper.updateEntity(
                registro,
                novaUnidade,
                "Nova descrição",
                "Hora",
                new BigDecimal("8.0000"),
                new BigDecimal("30.00"),
                new BigDecimal("240.00"));

        assertThat(registro.getId()).isEqualTo(20L);
        assertThat(registro.getOrcamento()).isSameAs(orcamento);
        assertThat(registro.getCriadoEm()).isEqualTo(criadoEm);
        assertThat(registro.getUnidadeMaoDeObra()).isSameAs(novaUnidade);
        assertThat(registro.getDescricao()).isEqualTo("Nova descrição");
        assertThat(registro.getUnidade()).isEqualTo("Hora");
        assertThat(registro.getCustoTotal()).isEqualByComparingTo("240.00");
    }

    @Test
    void deveConverterEntidadeParaResponseComNomeAtualESnapshot() {
        LocalDateTime criadoEm = LocalDateTime.of(2026, 8, 21, 12, 0);
        MaoDeObraOrcamento registro = MaoDeObraOrcamento.builder()
                .id(20L)
                .orcamento(Orcamento.builder().id(10L).build())
                .unidadeMaoDeObra(unidade(5L, "Nome atual"))
                .descricao("Instalação")
                .unidade("Diária")
                .quantidade(new BigDecimal("2.0000"))
                .custoUnitario(new BigDecimal("250.00"))
                .custoTotal(new BigDecimal("500.00"))
                .criadoEm(criadoEm)
                .build();

        MaoDeObraOrcamentoResponse response = MaoDeObraOrcamentoMapper.toResponse(registro);

        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getUnidadeMaoDeObra().getId()).isEqualTo(5L);
        assertThat(response.getUnidadeMaoDeObra().getNome()).isEqualTo("Nome atual");
        assertThat(response.getDescricao()).isEqualTo("Instalação");
        assertThat(response.getUnidade()).isEqualTo("Diária");
        assertThat(response.getQuantidade()).isEqualByComparingTo("2.0000");
        assertThat(response.getCustoUnitario()).isEqualByComparingTo("250.00");
        assertThat(response.getCustoTotal()).isEqualByComparingTo("500.00");
        assertThat(response.getCriadoEm()).isEqualTo(criadoEm);
    }

    private UnidadeMaoDeObra unidade(Long id, String nome) {
        return UnidadeMaoDeObra.builder().id(id).nome(nome).ativo(true).build();
    }
}
