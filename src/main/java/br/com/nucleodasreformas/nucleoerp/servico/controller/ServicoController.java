package br.com.nucleodasreformas.nucleoerp.servico.controller;

import br.com.nucleodasreformas.nucleoerp.servico.dto.ServicoRequest;
import br.com.nucleodasreformas.nucleoerp.servico.dto.ServicoResponse;
import br.com.nucleodasreformas.nucleoerp.servico.service.ServicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/servicos")
@RequiredArgsConstructor
@Tag(name = "Serviços", description = "Operações relacionadas aos serviços")
public class ServicoController {

    private final ServicoService service;

    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Serviço cadastrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @Operation(summary = "Salvar serviço")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServicoResponse salvar(@RequestBody @Valid ServicoRequest request) {
        return service.salvar(request);
    }

    @Operation(summary = "Buscar serviço por id")
    @GetMapping("/{id}")
    public ServicoResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @Operation(summary = "Listar serviços ativos")
    @GetMapping
    public List<ServicoResponse> listar() {
        return service.listar();
    }

    @Operation(summary = "Atualizar serviço")
    @PutMapping("/{id}")
    public ServicoResponse atualizar(
            @PathVariable Long id,
            @RequestBody @Valid ServicoRequest request) {
        return service.atualizar(id, request);
    }

    @Operation(summary = "Deletar serviço")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        service.deletar(id);
    }
}
