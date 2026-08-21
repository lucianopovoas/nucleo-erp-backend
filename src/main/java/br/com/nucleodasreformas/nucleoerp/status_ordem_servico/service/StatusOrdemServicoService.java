package br.com.nucleodasreformas.nucleoerp.status_ordem_servico.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.dto.StatusOrdemServicoRequest;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.dto.StatusOrdemServicoResponse;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.dto.StatusOrdemServicoUpdateRequest;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.entity.StatusOrdemServico;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.mapper.StatusOrdemServicoMapper;
import br.com.nucleodasreformas.nucleoerp.status_ordem_servico.repository.StatusOrdemServicoRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class StatusOrdemServicoService {

    private static final String MENSAGEM_NOME_DUPLICADO =
            "Já existe um status de ordem de serviço com esse nome.";
    private static final String MENSAGEM_CODIGO_DUPLICADO =
            "Já existe um status de ordem de serviço com esse código.";
    private static final String INDICE_NOME = "uk_status_ordem_servico_nome_normalizado";
    private static final String CONSTRAINT_CODIGO = "uk_status_ordem_servico_codigo";

    private final StatusOrdemServicoRepository repository;

    public StatusOrdemServicoResponse salvar(StatusOrdemServicoRequest request) {
        request.setCodigo(request.getCodigo().trim().toUpperCase(Locale.ROOT));
        request.setNome(request.getNome().trim());
        validarNomeDuplicado(request.getNome());
        validarCodigoDuplicado(request.getCodigo());

        StatusOrdemServico status = StatusOrdemServicoMapper.toEntity(request);
        status.setAtivo(true);
        return StatusOrdemServicoMapper.toResponse(salvarComTratamentoDeConflito(status));
    }

    @Transactional(readOnly = true)
    public StatusOrdemServicoResponse buscarPorId(Long id) {
        return StatusOrdemServicoMapper.toResponse(buscarStatus(id));
    }

    @Transactional(readOnly = true)
    public List<StatusOrdemServicoResponse> listar() {
        return repository.findByAtivoTrue().stream()
                .map(StatusOrdemServicoMapper::toResponse)
                .toList();
    }

    public StatusOrdemServicoResponse atualizar(
            Long id, StatusOrdemServicoUpdateRequest request) {
        StatusOrdemServico status = buscarStatus(id);
        request.setNome(request.getNome().trim());
        if (repository.existsByNomeNormalizadoAndIdNot(request.getNome(), id)) {
            throw new BusinessException(MENSAGEM_NOME_DUPLICADO);
        }
        StatusOrdemServicoMapper.updateEntity(status, request);
        return StatusOrdemServicoMapper.toResponse(salvarComTratamentoDeConflito(status));
    }

    public void deletar(Long id) {
        StatusOrdemServico status = buscarStatus(id);
        status.setAtivo(false);
        repository.save(status);
    }

    private void validarNomeDuplicado(String nome) {
        if (repository.existsByNomeNormalizado(nome)) {
            throw new BusinessException(MENSAGEM_NOME_DUPLICADO);
        }
    }

    private void validarCodigoDuplicado(String codigo) {
        if (repository.existsByCodigo(codigo)) {
            throw new BusinessException(MENSAGEM_CODIGO_DUPLICADO);
        }
    }

    private StatusOrdemServico salvarComTratamentoDeConflito(StatusOrdemServico status) {
        try {
            return repository.saveAndFlush(status);
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

    private StatusOrdemServico buscarStatus(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Status de ordem de serviço não encontrado. Id: " + id));
    }
}
