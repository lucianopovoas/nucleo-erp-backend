package br.com.nucleodasreformas.nucleoerp.status_orcamento.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.dto.StatusOrcamentoRequest;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.dto.StatusOrcamentoResponse;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.dto.StatusOrcamentoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.entity.StatusOrcamento;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.mapper.StatusOrcamentoMapper;
import br.com.nucleodasreformas.nucleoerp.status_orcamento.repository.StatusOrcamentoRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StatusOrcamentoService {

    private static final String MENSAGEM_NOME_DUPLICADO = "Já existe um status de orçamento com esse nome.";
    private static final String MENSAGEM_CODIGO_DUPLICADO = "Já existe um status de orçamento com esse código.";
    private static final String INDICE_NOME = "uk_status_orcamento_nome_normalizado";
    private static final String CONSTRAINT_CODIGO = "uk_status_orcamento_codigo";

    private final StatusOrcamentoRepository repository;

    public StatusOrcamentoResponse salvar(StatusOrcamentoRequest request) {
        normalizar(request);
        validarNomeDuplicado(request.getNome());
        validarCodigoDuplicado(request.getCodigo());

        StatusOrcamento statusOrcamento = StatusOrcamentoMapper.toEntity(request);
        statusOrcamento.setAtivo(true);

        return StatusOrcamentoMapper.toResponse(salvarComTratamentoDeConflito(statusOrcamento));
    }

    @Transactional(readOnly = true)
    public StatusOrcamentoResponse buscarPorId(Long id) {
        return StatusOrcamentoMapper.toResponse(buscarStatusOrcamento(id));
    }

    @Transactional(readOnly = true)
    public List<StatusOrcamentoResponse> listar() {
        return repository.findByAtivoTrue()
                .stream()
                .map(StatusOrcamentoMapper::toResponse)
                .toList();
    }

    public StatusOrcamentoResponse atualizar(Long id, StatusOrcamentoUpdateRequest request) {
        StatusOrcamento statusOrcamento = buscarStatusOrcamento(id);

        normalizarNome(request);
        validarNomeDuplicadoNaAtualizacao(request.getNome(), id);
        StatusOrcamentoMapper.updateEntity(statusOrcamento, request);

        return StatusOrcamentoMapper.toResponse(salvarComTratamentoDeConflito(statusOrcamento));
    }

    public void deletar(Long id) {
        StatusOrcamento statusOrcamento = buscarStatusOrcamento(id);
        statusOrcamento.setAtivo(false);
        repository.save(statusOrcamento);
    }

    private void normalizar(StatusOrcamentoRequest request) {
        request.setCodigo(request.getCodigo().trim().toUpperCase(java.util.Locale.ROOT));
        if (request.getNome() != null) {
            request.setNome(request.getNome().trim());
        }
    }

    private void normalizarNome(StatusOrcamentoUpdateRequest request) {
        if (request.getNome() != null) {
            request.setNome(request.getNome().trim());
        }
    }

    private void validarNomeDuplicado(String nome) {
        if (repository.existsByNomeNormalizado(nome)) {
            throw new BusinessException(MENSAGEM_NOME_DUPLICADO);
        }
    }

    private void validarNomeDuplicadoNaAtualizacao(String nome, Long id) {
        if (repository.existsByNomeNormalizadoAndIdNot(nome, id)) {
            throw new BusinessException(MENSAGEM_NOME_DUPLICADO);
        }
    }

    private void validarCodigoDuplicado(String codigo) {
        if (repository.existsByCodigo(codigo)) {
            throw new BusinessException(MENSAGEM_CODIGO_DUPLICADO);
        }
    }

    private StatusOrcamento salvarComTratamentoDeConflito(StatusOrcamento statusOrcamento) {
        try {
            return repository.saveAndFlush(statusOrcamento);
        } catch (DataIntegrityViolationException ex) {
            String constraint = buscarConstraint(ex);
            if (INDICE_NOME.equals(constraint)) {
                throw new BusinessException(MENSAGEM_NOME_DUPLICADO);
            }
            if (CONSTRAINT_CODIGO.equals(constraint)) {
                throw new BusinessException(MENSAGEM_CODIGO_DUPLICADO);
            }
            throw ex;
        }
    }

    private String buscarConstraint(Throwable throwable) {
        Throwable causa = throwable;
        while (causa != null) {
            if (causa instanceof ConstraintViolationException violacao
                    && violacao.getConstraintName() != null) {
                return violacao.getConstraintName();
            }
            causa = causa.getCause();
        }
        return null;
    }

    private StatusOrcamento buscarStatusOrcamento(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Status de orçamento não encontrado. Id: " + id));
    }
}
