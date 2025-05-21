package com.es.controller

import com.es.dto.PedidoDTO
import com.es.error.exception.ForbiddenException
import com.es.service.PedidoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/pedidos")
class PedidoController {

    @Autowired
    private lateinit var pedidoService: PedidoService

    private fun isAdmin(auth: Authentication): Boolean =
        auth.authorities.any { it.authority == "ROLE_ADMIN" }

    /**
     * GET /pedidos
     * - ADMIN: todos los pedidos.
     * - USER: solo sus propios pedidos.
     */
    @GetMapping
    fun getAll(authentication: Authentication): ResponseEntity<List<PedidoDTO>> {
        return if (isAdmin(authentication)) {
            ResponseEntity.ok(pedidoService.findAll())
        } else {
            val email = authentication.name
            ResponseEntity.ok(pedidoService.findByUserEmail(email))
        }
    }

    /**
     * GET /pedidos/{id}
     * - ADMIN: cualquier pedido.
     * - USER: solo si es el suyo.
     */
    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: String,
        authentication: Authentication
    ): ResponseEntity<PedidoDTO> {
        val dto = pedidoService.findById(id)
        if (!isAdmin(authentication) && dto.userEmail != authentication.name) {
            throw ForbiddenException("No tienes permiso para ver este pedido")
        }
        return ResponseEntity.ok(dto)
    }

    /**
     * POST /pedidos
     * Crea un pedido; for USER, el email sale del token.
     */
    @PostMapping
    fun create(
        @RequestBody dto: PedidoDTO,
        authentication: Authentication
    ): ResponseEntity<PedidoDTO> {
        val userEmail = if (isAdmin(authentication)) dto.userEmail else authentication.name
        val toCreate = dto.copy(id = null, userEmail = userEmail)
        val created = pedidoService.create(toCreate)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    /**
     * PUT /pedidos/{id}
     * Actualiza un pedido; solo ADMIN o propietario.
     */
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: String,
        @RequestBody dto: PedidoDTO,
        authentication: Authentication
    ): ResponseEntity<PedidoDTO> {
        val existing = pedidoService.findById(id)
        if (!isAdmin(authentication) && existing.userEmail != authentication.name) {
            throw ForbiddenException("No tienes permiso para actualizar este pedido")
        }
        val updated = pedidoService.update(id, dto)
        return ResponseEntity.ok(updated)
    }

    /**
     * DELETE /pedidos/{id}
     * Elimina un pedido; solo ADMIN o propietario.
     */
    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: String,
        authentication: Authentication
    ): ResponseEntity<Void> {
        val existing = pedidoService.findById(id)
        if (!isAdmin(authentication) && existing.userEmail != authentication.name) {
            throw ForbiddenException("No tienes permiso para eliminar este pedido")
        }
        pedidoService.delete(id)
        return ResponseEntity.noContent().build()
    }
}