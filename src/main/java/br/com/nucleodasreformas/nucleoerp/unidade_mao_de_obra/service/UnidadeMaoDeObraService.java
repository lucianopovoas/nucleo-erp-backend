package br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.service;

import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.dto.UnidadeMaoDeObraRequest;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.dto.UnidadeMaoDeObraResponse;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.entity.UnidadeMaoDeObra;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.mapper.UnidadeMaoDeObraMapper;
import br.com.nucleodasreformas.nucleoerp.unidade_mao_de_obra.repository.UnidadeMaoDeObraRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UnidadeMaoDeObraService {

    private static final String MENSAGEM_DUPLICIDADE =
            "Já existe uma unidade de mão de obra com esse nome.";
    private static final String INDICE_UNICIDADE =
            "uk_unidade_mao_de_obra_nome_normalizado";

    private final UnidadeMaoDeObraRepository repository;

    public UnidadeMaoDeObraResponse salvar(UnidadeMaoDeObraRequest request) {
        normalizarNome(request);
        validarNomeDuplicado(request.getNome());

        UnidadeMaoDeObra unidadeMaoDeObra = UnidadeMaoDeObraMapper.toEntity(request);
        unidadeMaoDeObra.setAtivo(true);

        return UnidadeMaoDeObraMapper.toResponse(
                salvarComTratamentoDeConflito(unidadeMaoDeObra));
    }

    @Transactional(readOnly = true)
    public UnidadeMaoDeObraResponse buscarPorId(Long id) {
        return UnidadeMaoDeObraMapper.toResponse(buscarUnidadeMaoDeObra(id));
    }

    @Transactional(readOnly = true)
    public List<UnidadeMaoDeObraResponse> listar() {
        return repository.findByAtivoTrue()
                .stream()
                .map(UnidadeMaoDeObraMapper::toResponse)
                .toList();
    }

    public UnidadeMaoDeObraResponse atualizar(Long id, UnidadeMaoDeObraRequest request) {
        UnidadeMaoDeObra unidadeMaoDeObra = buscarUnidadeMaoDeObra(id);

        normalizarNome(request);
        validarNomeDuplicadoNaAtualizacao(request.getNome(), id);
        UnidadeMaoDeObraMapper.updateEntity(unidadeMaoDeObra, request);

        return UnidadeMaoDeObraMapper.toResponse(
                salvarComTratamentoDeConflito(unidadeMaoDeObra));
    }

    public void deletar(Long id) {
        UnidadeMaoDeObra unidadeMaoDeObra = buscarUnidadeMaoDeObra(id);
        unidadeMaoDeObra.setAtivo(false);
        repository.save(unidadeMaoDeObra);
    }

    private void normalizarNome(UnidadeMaoDeObraRequest request) {
        if (request.getNome() != null) {
            request.setNome(request.getNome().trim());
        }
    }

    private void validarNomeDuplicado(String nome) {
        if (repository.existsByNomeNormalizado(nome)) {
            throw new BusinessException(MENSAGEM_DUPLICIDADE);
        }
    }

    private void validarNomeDuplicadoNaAtualizacao(String nome, Long id) {
        if (repository.existsByNomeNormalizadoAndIdNot(nome, id)) {
            throw new BusinessException(MENSAGEM_DUPLICIDADE);
        }
    }

    private UnidadeMaoDeObra salvarComTratamentoDeConflito(
            UnidadeMaoDeObra unidadeMaoDeObra) {
        try {
            return repository.saveAndFlush(unidadeMaoDeObra);
        } catch (DataIntegrityViolationException ex) {
            if (causadoPeloIndiceUnico(ex)) {
                throw new BusinessException(MENSAGEM_DUPLICIDADE);
            }
            throw ex;
        }
    }

    private boolean causadoPeloIndiceUnico(Throwable throwable) {
        Throwable causa = throwable;
        while (causa != null) {
            if (causa instanceof ConstraintViolationException violacao
                    && INDICE_UNICIDADE.equals(violacao.getConstraintName())) {
                return true;
            }
            causa = causa.getCause();
        }
        return false;
    }

    private UnidadeMaoDeObra buscarUnidadeMaoDeObra(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Unidade de mão de obra não encontrada. Id: " + id));
    }
}
