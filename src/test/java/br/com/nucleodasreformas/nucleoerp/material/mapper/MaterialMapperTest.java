package br.com.nucleodasreformas.nucleoerp.material.mapper;

import br.com.nucleodasreformas.nucleoerp.material.dto.MaterialRequest;
import br.com.nucleodasreformas.nucleoerp.material.dto.MaterialResponse;
import br.com.nucleodasreformas.nucleoerp.material.entity.Material;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MaterialMapperTest {

    @Test
    void deveConverterRequestParaEntidadeComTodosOsCampos() {
        MaterialRequest request = requestCompleto();
        request.setAtivo(false);

        Material material = MaterialMapper.toEntity(request);

        assertThat(material.getNome()).isEqualTo("Lona");
        assertThat(material.getDescricao()).isEqualTo("Lona reforçada");
        assertThat(material.getUnidade()).isEqualTo("M2");
        assertThat(material.getLargura()).isEqualByComparingTo("1.50");
        assertThat(material.getAtivo()).isFalse();
    }

    @Test
    void deveAplicarAtivoComoVerdadeiroQuandoRequestNaoInformarValor() {
        Material material = MaterialMapper.toEntity(requestCompleto());

        assertThat(material.getAtivo()).isTrue();
    }

    @Test
    void deveConverterEntidadeParaResponseCompleto() {
        LocalDateTime criadoEm = LocalDateTime.of(2026, 8, 19, 11, 30);
        Material material = Material.builder().id(3L).nome("Lona").descricao("Lona reforçada")
                .unidade("M2").largura(new BigDecimal("1.50")).ativo(false).criadoEm(criadoEm).build();

        MaterialResponse response = MaterialMapper.toResponse(material);

        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getNome()).isEqualTo("Lona");
        assertThat(response.getDescricao()).isEqualTo("Lona reforçada");
        assertThat(response.getUnidade()).isEqualTo("M2");
        assertThat(response.getLargura()).isEqualByComparingTo("1.50");
        assertThat(response.getAtivo()).isFalse();
        assertThat(response.getCriadoEm()).isEqualTo(criadoEm);
    }

    @Test
    void deveAtualizarEntidadeEPreservarAtivoQuandoNaoInformado() {
        Material material = Material.builder().nome("Antigo").ativo(false).build();

        MaterialMapper.updateEntity(material, requestCompleto());

        assertThat(material.getNome()).isEqualTo("Lona");
        assertThat(material.getDescricao()).isEqualTo("Lona reforçada");
        assertThat(material.getUnidade()).isEqualTo("M2");
        assertThat(material.getLargura()).isEqualByComparingTo("1.50");
        assertThat(material.getAtivo()).isFalse();
    }

    private MaterialRequest requestCompleto() {
        MaterialRequest request = new MaterialRequest();
        request.setNome("Lona");
        request.setDescricao("Lona reforçada");
        request.setUnidade("M2");
        request.setLargura(new BigDecimal("1.50"));
        return request;
    }
}
