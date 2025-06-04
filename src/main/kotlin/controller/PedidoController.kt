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
     * - ADMIN: all orders.
     * - USER: only their own orders.
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
     * - ADMIN: any order.
     * - USER: only if it belongs to them.
     */
    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: String,
        authentication: Authentication
    ): ResponseEntity<PedidoDTO> {
        val dto = pedidoService.findById(id)
        if (!isAdmin(authentication) && dto.userEmail != authentication.name) {
            throw ForbiddenException("You do not have permission to view this order")
        }
        return ResponseEntity.ok(dto)
    }

    /**
     * POST /pedidos
     * Creates an order; for USER, email is taken from token.
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
     * Updates an order; only ADMIN or owner.
     */
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: String,
        @RequestBody dto: PedidoDTO,
        authentication: Authentication
    ): ResponseEntity<PedidoDTO> {
        val existing = pedidoService.findById(id)
        if (!isAdmin(authentication) && existing.userEmail != authentication.name) {
            throw ForbiddenException("You do not have permission to update this order")
        }
        val updated = pedidoService.update(id, dto)
        return ResponseEntity.ok(updated)
    }

    /**
     * DELETE /pedidos/{id}
     * Deletes an order; only ADMIN or owner.
     */
    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: String,
        authentication: Authentication
    ): ResponseEntity<Void> {
        val existing = pedidoService.findById(id)
        if (!isAdmin(authentication) && existing.userEmail != authentication.name) {
            throw ForbiddenException("You do not have permission to delete this order")
        }
        pedidoService.delete(id)
        return ResponseEntity.noContent().build()
    }
}