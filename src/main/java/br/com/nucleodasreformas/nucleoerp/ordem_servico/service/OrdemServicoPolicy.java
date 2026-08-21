package br.com.nucleodasreformas.nucleoerp.ordem_servico.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.entity.OrdemServico;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class OrdemServicoPolicy {

    public static final String COMPRAR_MATERIAL = "COMPRAR_MATERIAL";
    public static final String EM_EXECUCAO = "EM_EXECUCAO";
    public static final String INSTALAR = "INSTALAR";
    public static final String CONCLUIDO = "CONCLUIDO";

    private static final String STATUS_ORIGEM_APROVADO = "APROVADO";
    private static final Set<String> STATUS_EDITAVEIS =
            Set.of(COMPRAR_MATERIAL, EM_EXECUCAO, INSTALAR);
    private static final Map<String, Set<String>> TRANSICOES = Map.of(
            COMPRAR_MATERIAL, Set.of(EM_EXECUCAO),
            EM_EXECUCAO, Set.of(INSTALAR),
            INSTALAR, Set.of(CONCLUIDO),
            CONCLUIDO, Set.of());

    public void garantirOrigemAprovada(Orcamento orcamento, OrcamentoVersao versao) {
        if (orcamento.getVersaoAtual() == null
                || !orcamento.getVersaoAtual().getId().equals(versao.getId())) {
            throw new BusinessException(
                    "A ordem de serviço deve ser criada a partir da versão atual do orçamento.");
        }
        if (!STATUS_ORIGEM_APROVADO.equals(versao.getStatusOrcamento().getCodigo())) {
            throw new BusinessException(
                    "A ordem de serviço só pode ser criada a partir de uma versão APROVADA.");
        }
    }

    public void garantirObservacaoEditavel(OrdemServico ordemServico) {
        String codigo = ordemServico.getStatusOrdemServico().getCodigo();
        if (!STATUS_EDITAVEIS.contains(codigo)) {
            throw new BusinessException(
                    "A observação não pode ser alterada no status " + codigo + ".");
        }
    }

    public void validarTransicao(String origem, String destino) {
        Set<String> destinos = TRANSICOES.get(origem);
        if (destinos == null || !TRANSICOES.containsKey(destino) || !destinos.contains(destino)) {
            throw new BusinessException(
                    "Transição de status não permitida: " + origem + " -> " + destino + ".");
        }
    }
}
