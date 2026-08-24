package br.com.nucleodasreformas.nucleoerp.orcamento_versao.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.entity.StatusOrcamento;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrcamentoVersaoPolicyTest {

    private final OrcamentoVersaoPolicy policy = new OrcamentoVersaoPolicy();

    @Test
    void deveAceitarSomenteTransicoesDaMatrizCanonica() {
        assertThatCode(() -> policy.validarTransicao("RASCUNHO", "ENVIADO"))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.validarTransicao("RASCUNHO", "CANCELADO"))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.validarTransicao("ENVIADO", "APROVADO"))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.validarTransicao("ENVIADO", "RECUSADO"))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.validarTransicao("ENVIADO", "CANCELADO"))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> policy.validarTransicao("ENVIADO", "RASCUNHO"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> policy.validarTransicao("APROVADO", "ENVIADO"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> policy.validarTransicao("CUSTOMIZADO", "ENVIADO"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deveDerivarCapacidadesDaMesmaPoliticaDeValidacao() {
        Orcamento orcamento = Orcamento.builder().id(1L).build();
        OrcamentoVersao rascunho = versao(2L, orcamento, "RASCUNHO");
        orcamento.setVersaoAtual(rascunho);

        assertThat(policy.podeEditarConteudo(orcamento, rascunho)).isTrue();
        assertThat(policy.podeOriginarNovaVersao(rascunho)).isFalse();
        assertThat(policy.destinosPermitidos("RASCUNHO"))
                .containsExactly("ENVIADO", "CANCELADO");

        OrcamentoVersao enviada = versao(3L, orcamento, "ENVIADO");
        orcamento.setVersaoAtual(enviada);
        assertThat(policy.podeEditarConteudo(orcamento, enviada)).isFalse();
        assertThat(policy.podeOriginarNovaVersao(enviada)).isTrue();
        assertThat(policy.destinosPermitidos("ENVIADO"))
                .containsExactly("APROVADO", "RECUSADO", "CANCELADO");

        assertThat(policy.podeEditarConteudo(orcamento, rascunho)).isFalse();
        assertThat(policy.destinosPermitidos("CUSTOMIZADO")).isEmpty();
    }

    private OrcamentoVersao versao(Long id, Orcamento orcamento, String codigo) {
        return OrcamentoVersao.builder()
                .id(id)
                .orcamento(orcamento)
                .statusOrcamento(StatusOrcamento.builder().codigo(codigo).build())
                .build();
    }
}
