package br.com.nucleodasreformas.nucleoerp.cliente.controller;

import br.com.nucleodasreformas.nucleoerp.cliente.dto.ClienteRequest;
import br.com.nucleodasreformas.nucleoerp.cliente.dto.ClienteResponse;
import br.com.nucleodasreformas.nucleoerp.cliente.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Operações relacionadas aos clientes")
public class ClienteController {

    private final ClienteService service;

    @Operation(summary = "Cadastrar cliente")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente cadastrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponse salvar(@RequestBody @Valid ClienteRequest request) {

        return service.salvar(request);

    }

    @Operation(summary = "Buscar cliente por id")
    @GetMapping("/{id}")
    public ClienteResponse buscarPorId(@PathVariable Long id) {

        return service.buscarPorId(id);

    }

    @Operation(summary = "Listar de clientes")
    @GetMapping
    public List<ClienteResponse> listar() {

        return service.listar();

    }

    @Operation(summary = "Atualizar cliente")
    @PutMapping("/{id}")
    public ClienteResponse atualizar(@PathVariable Long id,
                                     @RequestBody @Valid ClienteRequest request) {

        return service.atualizar(id, request);

    }

    @Operation(summary = "Excluir cliente")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {

        service.deletar(id);

    }
}