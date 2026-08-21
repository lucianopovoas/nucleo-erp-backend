package br.com.nucleodasreformas.nucleoerp.ordem_servico.mapper;

import br.com.nucleodasreformas.nucleoerp.cliente.entity.Cliente;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.entity.OrdemServico;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.entity.StatusOrdemServico;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OrdemServicoMapperTest {

    @Test
    void deveMapearOrdemEOrigemSemCopiarConteudoComercial() {
        Cliente cliente = Cliente.builder().id(1L).nome("Cliente").build();
        Orcamento orcamento = Orcamento.builder()
                .id(2L).numero(125L).cliente(cliente).build();
        OrcamentoVersao versao = OrcamentoVersao.builder()
                .id(3L).numeroVersao(4).orcamento(orcamento).build();
        StatusOrdemServico status = StatusOrdemServico.builder()
                .id(5L).codigo("COMPRAR_MATERIAL").nome("Comprar material").build();
        LocalDateTime criadoEm = LocalDateTime.of(2026, 8, 21, 10, 0);
        OrdemServico ordem = OrdemServico.builder()
                .id(6L).numero(45L).orcamentoVersao(versao)
                .statusOrdemServico(status).observacao("Observação")
                .criadoEm(criadoEm).build();

        var response = OrdemServicoMapper.toResponse(ordem);

        assertThat(response.getId()).isEqualTo(6L);
        assertThat(response.getNumero()).isEqualTo(45L);
        assertThat(response.getStatus().getCodigo()).isEqualTo("COMPRAR_MATERIAL");
        assertThat(response.getOrigem().getOrcamento().getNumero()).isEqualTo(125L);
        assertThat(response.getOrigem().getVersao().getNumeroVersao()).isEqualTo(4);
        assertThat(response.getOrigem().getCliente().getNome()).isEqualTo("Cliente");
        assertThat(response.getCriadoEm()).isEqualTo(criadoEm);
    }
}
