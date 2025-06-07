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

    /** ADMIN: all orders */
    fun findAll(): List<PedidoDTO> =
        pedidoRepository.findAll().map { it.toDTO() }

    /** Any user: their own order */
    fun findById(id: String): PedidoDTO {
        val p = pedidoRepository.findById(id)
            .orElseThrow { NotFoundException("Order '$id' not found") }
        return p.toDTO()
    }

    /** ADMIN or owner: orders of a user */
    fun findByUserEmail(userEmail: String): List<PedidoDTO> =
        pedidoRepository.findByUserEmail(userEmail).map { it.toDTO() }

    /** Creates an order, ignoring dto. id so Mongo generates one */
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

    /** Updates an existing order (does not change the id) */
    fun update(id: String, dto: PedidoDTO): PedidoDTO {
        if (id != dto.id) throw BadRequestException("ID no se puede modificar")
        val existing = pedidoRepository.findById(id)
            .orElseThrow { NotFoundException("Pedido '$id' no encontrado") }

        // Construimos la copia respetando el status existente,
        // y luego permitimos al controller sobreescribir solo si es ADMIN.
        val base = existing.copy(
            userEmail = dto.userEmail,
            productos = dto.productos,
            cantidad  = dto.cantidad,
            coste     = dto.coste,
            direccion = dto.direccion,
            status    = existing.status
        )
        val toSave = dto.status.takeIf { it != existing.status }?.let {
            // el controller ya validó que solo ADMIN puede cambiar status
            base.copy(status = it)
        } ?: base

        return pedidoRepository.save(toSave).toDTO()
    }

    /** Deletes an order by its ID */
    fun delete(id: String) {
        if (!pedidoRepository.existsById(id)) {
            throw NotFoundException("Order '$id' not found")
        }
        pedidoRepository.deleteById(id)
    }

    // ——————————————————————————————
    // Internal mapper
    // ——————————————————————————————
    private fun Pedido.toDTO(): PedidoDTO =
        PedidoDTO(
            id        = this.id,
            userEmail = this.userEmail,
            productos = this.productos,
            cantidad  = this.cantidad,
            coste     = this.coste,
            direccion = this.direccion,
            status    = this.status
        )
}