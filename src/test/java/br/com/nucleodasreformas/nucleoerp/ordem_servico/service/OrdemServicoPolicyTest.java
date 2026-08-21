package br.com.nucleodasreformas.nucleoerp.ordem_servico.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.entity.OrdemServico;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.entity.StatusOrcamento;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.entity.StatusOrdemServico;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrdemServicoPolicyTest {

    private final OrdemServicoPolicy policy = new OrdemServicoPolicy();

    @Test
    void deveAceitarSomenteTransicoesLineares() {
        assertThatCode(() -> policy.validarTransicao("COMPRAR_MATERIAL", "EM_EXECUCAO"))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.validarTransicao("EM_EXECUCAO", "INSTALAR"))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.validarTransicao("INSTALAR", "CONCLUIDO"))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> policy.validarTransicao("COMPRAR_MATERIAL", "INSTALAR"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> policy.validarTransicao("EM_EXECUCAO", "CONCLUIDO"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> policy.validarTransicao("INSTALAR", "EM_EXECUCAO"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> policy.validarTransicao("CONCLUIDO", "COMPRAR_MATERIAL"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> policy.validarTransicao("CUSTOMIZADO", "CONCLUIDO"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> policy.validarTransicao("INSTALAR", "CUSTOMIZADO"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deveValidarOrigemPeloCodigoAprovadoESomenteNaVersaoAtual() {
        Orcamento orcamento = Orcamento.builder().id(1L).build();
        OrcamentoVersao aprovada = versao(2L, orcamento, "APROVADO");
        orcamento.setVersaoAtual(aprovada);

        assertThatCode(() -> policy.garantirOrigemAprovada(orcamento, aprovada))
                .doesNotThrowAnyException();

        OrcamentoVersao enviada = versao(3L, orcamento, "ENVIADO");
        orcamento.setVersaoAtual(enviada);
        assertThatThrownBy(() -> policy.garantirOrigemAprovada(orcamento, enviada))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("APROVADA");

        OrcamentoVersao customizada = versao(4L, orcamento, "CUSTOMIZADO");
        orcamento.setVersaoAtual(customizada);
        assertThatThrownBy(() -> policy.garantirOrigemAprovada(orcamento, customizada))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("APROVADA");

        orcamento.setVersaoAtual(aprovada);
        assertThatThrownBy(() -> policy.garantirOrigemAprovada(orcamento, enviada))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("versão atual");
    }

    @Test
    void devePermitirObservacaoSomenteNosTresStatusNaoTerminais() {
        assertThatCode(() -> policy.garantirObservacaoEditavel(ordem("COMPRAR_MATERIAL")))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.garantirObservacaoEditavel(ordem("EM_EXECUCAO")))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.garantirObservacaoEditavel(ordem("INSTALAR")))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.garantirObservacaoEditavel(ordem("CONCLUIDO")))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> policy.garantirObservacaoEditavel(ordem("CUSTOMIZADO")))
                .isInstanceOf(BusinessException.class);
    }

    private OrcamentoVersao versao(Long id, Orcamento orcamento, String codigo) {
        return OrcamentoVersao.builder()
                .id(id)
                .orcamento(orcamento)
                .statusOrcamento(StatusOrcamento.builder().codigo(codigo).build())
                .build();
    }

    private OrdemServico ordem(String codigo) {
        return OrdemServico.builder()
                .statusOrdemServico(StatusOrdemServico.builder().codigo(codigo).build())
                .build();
    }
}
