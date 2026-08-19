package br.com.nucleodasreformas.nucleoerp.materialfornecedor.mapper;

import br.com.nucleodasreformas.nucleoerp.fornecedor.entity.Fornecedor;
import br.com.nucleodasreformas.nucleoerp.material.entity.Material;
import br.com.nucleodasreformas.nucleoerp.materialfornecedor.dto.MaterialFornecedorResponse;
import br.com.nucleodasreformas.nucleoerp.materialfornecedor.entity.MaterialFornecedor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MaterialFornecedorMapperTest {

    @Test
    void deveConverterEntidadeParaResponseCompleto() {
        LocalDateTime criadoEm = LocalDateTime.of(2026, 8, 19, 12, 30);
        Material material = Material.builder().id(1L).nome("Lona").unidade("M2").build();
        Fornecedor fornecedor = Fornecedor.builder().id(2L).nome("Fornecedor X").build();
        MaterialFornecedor entity = MaterialFornecedor.builder()
                .id(10L)
                .material(material)
                .fornecedor(fornecedor)
                .precoCompra(new BigDecimal("125.50"))
                .ativo(true)
                .criadoEm(criadoEm)
                .build();

        MaterialFornecedorResponse response = MaterialFornecedorMapper.toResponse(entity);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getMaterial().getId()).isEqualTo(1L);
        assertThat(response.getMaterial().getNome()).isEqualTo("Lona");
        assertThat(response.getFornecedor().getId()).isEqualTo(2L);
        assertThat(response.getFornecedor().getNome()).isEqualTo("Fornecedor X");
        assertThat(response.getPrecoCompra()).isEqualByComparingTo("125.50");
        assertThat(response.getAtivo()).isTrue();
        assertThat(response.getCriadoEm()).isEqualTo(criadoEm);
    }
}
