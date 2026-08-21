package br.com.nucleodasreformas.nucleoerp.material_orcamento.mapper;

import br.com.nucleodasreformas.nucleoerp.material.entity.Material;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.dto.MaterialOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.material_orcamento.entity.MaterialOrcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MaterialOrcamentoMapperTest {

    @Test
    void deveCriarEntidadeComSnapshotsEValoresResolvidos() {
        Orcamento orcamento = Orcamento.builder().id(10L).build();
        Material material = material(5L, "Lona", "M2");

        MaterialOrcamento resultado = MaterialOrcamentoMapper.toEntity(
                orcamento, material, "Lona", "M2",
                new BigDecimal("2.5000"), new BigDecimal("75.00"),
                new BigDecimal("187.50"));

        assertThat(resultado.getOrcamento()).isSameAs(orcamento);
        assertThat(resultado.getMaterial()).isSameAs(material);
        assertThat(resultado.getDescricao()).isEqualTo("Lona");
        assertThat(resultado.getUnidade()).isEqualTo("M2");
        assertThat(resultado.getQuantidade()).isEqualByComparingTo("2.5000");
        assertThat(resultado.getCustoUnitario()).isEqualByComparingTo("75.00");
        assertThat(resultado.getCustoTotal()).isEqualByComparingTo("187.50");
        assertThat(resultado.getId()).isNull();
    }

    @Test
    void deveAtualizarSomenteCamposEditaveisESnapshotsResolvidos() {
        Orcamento orcamento = Orcamento.builder().id(10L).build();
        LocalDateTime criadoEm = LocalDateTime.of(2026, 8, 20, 12, 0);
        MaterialOrcamento registro = MaterialOrcamento.builder()
                .id(20L).orcamento(orcamento).material(material(1L, "Anterior", "UN"))
                .descricao("Anterior").unidade("UN").quantidade(BigDecimal.ONE)
                .custoUnitario(BigDecimal.TEN).custoTotal(BigDecimal.TEN)
                .criadoEm(criadoEm).build();
        Material novoMaterial = material(2L, "Novo", "M");

        MaterialOrcamentoMapper.updateEntity(
                registro, novoMaterial, "Descrição negociada", "M",
                new BigDecimal("2.0000"), new BigDecimal("20.00"),
                new BigDecimal("40.00"));

        assertThat(registro.getId()).isEqualTo(20L);
        assertThat(registro.getOrcamento()).isSameAs(orcamento);
        assertThat(registro.getCriadoEm()).isEqualTo(criadoEm);
        assertThat(registro.getMaterial()).isSameAs(novoMaterial);
        assertThat(registro.getDescricao()).isEqualTo("Descrição negociada");
        assertThat(registro.getUnidade()).isEqualTo("M");
        assertThat(registro.getCustoTotal()).isEqualByComparingTo("40.00");
    }

    @Test
    void deveConverterEntidadeParaResponseCompleto() {
        LocalDateTime criadoEm = LocalDateTime.of(2026, 8, 20, 12, 0);
        MaterialOrcamento registro = MaterialOrcamento.builder()
                .id(20L).material(material(5L, "Nome atual", "UN"))
                .descricao("Snapshot").unidade("M2")
                .quantidade(new BigDecimal("2.5000"))
                .custoUnitario(new BigDecimal("75.00"))
                .custoTotal(new BigDecimal("187.50"))
                .criadoEm(criadoEm).build();

        MaterialOrcamentoResponse response = MaterialOrcamentoMapper.toResponse(registro);

        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getMaterial().getId()).isEqualTo(5L);
        assertThat(response.getMaterial().getNome()).isEqualTo("Nome atual");
        assertThat(response.getDescricao()).isEqualTo("Snapshot");
        assertThat(response.getUnidade()).isEqualTo("M2");
        assertThat(response.getQuantidade()).isEqualByComparingTo("2.5000");
        assertThat(response.getCustoUnitario()).isEqualByComparingTo("75.00");
        assertThat(response.getCustoTotal()).isEqualByComparingTo("187.50");
        assertThat(response.getCriadoEm()).isEqualTo(criadoEm);
    }

    private Material material(Long id, String nome, String unidade) {
        return Material.builder().id(id).nome(nome).unidade(unidade).build();
    }
}
