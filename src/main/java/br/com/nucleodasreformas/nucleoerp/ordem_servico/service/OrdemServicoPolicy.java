package br.com.nucleodasreformas.nucleoerp.ordem_servico.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.orcamento.entity.Orcamento;
import br.com.nucleodasreformas.nucleoerp.orcamento_versao.entity.OrcamentoVersao;
import br.com.nucleodasreformas.nucleoerp.ordem_servico.entity.OrdemServico;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.List;
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
    private static final Map<String, List<String>> TRANSICOES = Map.of(
            COMPRAR_MATERIAL, List.of(EM_EXECUCAO),
            EM_EXECUCAO, List.of(INSTALAR),
            INSTALAR, List.of(CONCLUIDO),
            CONCLUIDO, List.of());

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
        if (!podeEditarObservacao(codigo)) {
            throw new BusinessException(
                    "A observação não pode ser alterada no status " + codigo + ".");
        }
    }

    public void validarTransicao(String origem, String destino) {
        if (!destinosPermitidos(origem).contains(destino)) {
            throw new BusinessException(
                    "Transição de status não permitida: " + origem + " -> " + destino + ".");
        }
    }

    public boolean podeEditarObservacao(String codigoStatus) {
        return STATUS_EDITAVEIS.contains(codigoStatus);
    }

    public List<String> destinosPermitidos(String origem) {
        return TRANSICOES.getOrDefault(origem, List.of());
    }
}
