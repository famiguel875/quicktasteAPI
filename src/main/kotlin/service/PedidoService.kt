package com.es.service

import com.es.dto.PedidoDTO
import com.es.error.exception.BadRequestException
import com.es.error.exception.NotFoundException
import com.es.model.Pedido
import com.es.repository.PedidoRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PedidoService(
    @Autowired private val pedidoRepository: PedidoRepository
) {

    /** ADMIN: todos los pedidos */
    fun findAll(): List<PedidoDTO> =
        pedidoRepository.findAll().map { it.toDTO() }

    /** Cualquier usuario: su propio pedido */
    fun findById(id: String): PedidoDTO {
        val p = pedidoRepository.findById(id)
            .orElseThrow { NotFoundException("Pedido '$id' no encontrado") }
        return p.toDTO()
    }

    /** ADMIN o propietario: pedidos de un usuario */
    fun findByUserEmail(userEmail: String): List<PedidoDTO> =
        pedidoRepository.findByUserEmail(userEmail).map { it.toDTO() }

    /** Crea un pedido, ignora dto.id para que Mongo genere uno */
    fun create(dto: PedidoDTO): PedidoDTO {
        val entity = Pedido(
            id         = null,
            userEmail  = dto.userEmail,
            productos  = dto.productos,
            cantidad   = dto.cantidad,
            coste      = dto.coste,
            direccion  = dto.direccion
        )
        val saved = pedidoRepository.save(entity)
        return saved.toDTO()
    }

    /** Actualiza un pedido existente (no cambia el id) */
    fun update(id: String, dto: PedidoDTO): PedidoDTO {
        if (id != dto.id) throw BadRequestException("El ID no puede modificarse")
        val existing = pedidoRepository.findById(id)
            .orElseThrow { NotFoundException("Pedido '$id' no encontrado") }
        val updated = existing.copy(
            userEmail = dto.userEmail,
            productos = dto.productos,
            cantidad  = dto.cantidad,
            coste     = dto.coste,
            direccion = dto.direccion
        )
        return pedidoRepository.save(updated).toDTO()
    }

    /** Elimina un pedido por su ID */
    fun delete(id: String) {
        if (!pedidoRepository.existsById(id)) {
            throw NotFoundException("Pedido '$id' no encontrado")
        }
        pedidoRepository.deleteById(id)
    }

    // ——————————————————————————————
    // Mapper interno
    // ——————————————————————————————
    private fun Pedido.toDTO(): PedidoDTO =
        PedidoDTO(
            id         = this.id,
            userEmail  = this.userEmail,
            productos  = this.productos,
            cantidad   = this.cantidad,
            coste      = this.coste,
            direccion  = this.direccion
        )
}

