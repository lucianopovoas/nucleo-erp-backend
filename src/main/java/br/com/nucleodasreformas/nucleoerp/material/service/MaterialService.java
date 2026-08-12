package br.com.nucleodasreformas.nucleoerp.material.service;


import br.com.nucleodasreformas.nucleoerp.exception.BusinessException;
import br.com.nucleodasreformas.nucleoerp.exception.ResourceNotFoundException;
import br.com.nucleodasreformas.nucleoerp.material.dto.MaterialRequest;
import br.com.nucleodasreformas.nucleoerp.material.dto.MaterialResponse;
import br.com.nucleodasreformas.nucleoerp.material.entity.Material;
import br.com.nucleodasreformas.nucleoerp.material.mapper.MaterialMapper;
import br.com.nucleodasreformas.nucleoerp.material.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Transactional
@Service
public class MaterialService {

    private final MaterialRepository repository;

    public MaterialResponse salvar(MaterialRequest request){

        Material material = MaterialMapper.toEntity(request);

        repository.save(material);

        return MaterialMapper.toResponse(material);
    }

    @Transactional(readOnly = true)
    public MaterialResponse buscarPorId(Long id){
        Material material = buscarMaterialId(id);

        return MaterialMapper.toResponse(material);
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> listar(){

        return repository.findAll()
                .stream()
                .map(MaterialMapper::toResponse)
                .toList();
    }

    public MaterialResponse atualizar(Long id, MaterialRequest request){

        Material material = buscarMaterialId(id);

        MaterialMapper.updateEntity(material, request);

        material = repository.save(material);

        return MaterialMapper.toResponse(material);
    }

    public void deletar(Long id){

        Material material = buscarMaterialId(id);
        material.setAtivo(false);
        repository.save(material);
    }

    private Material buscarMaterialId(Long id){
        return repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Material não encontrado Id:" + id));
    }
}
