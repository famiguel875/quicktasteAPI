package com.es.service

import com.es.dto.ProductoDTO
import com.es.error.exception.BadRequestException
import com.es.error.exception.ForbiddenException
import com.es.error.exception.NotFoundException
import com.es.model.Producto
import com.es.repository.ProductoRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class ProductoService(
    @Autowired private val productoRepository: ProductoRepository
) {

    /**
     * Devuelve todos los productos.
     * Cualquiera autenticado puede acceder.
     */
    fun findAll(): List<ProductoDTO> =
        productoRepository.findAll().map { it.toDTO() }

    /**
     * Busca un producto por su nombre (PK).
     * Cualquiera autenticado puede acceder.
     */
    fun findByName(name: String): ProductoDTO {
        val producto = productoRepository.findById(name)
            .orElseThrow { NotFoundException("Producto '$name' no encontrado") }
        return producto.toDTO()
    }

    /**
     * Lista todos los productos de una categoría dada.
     * Cualquiera autenticado puede acceder.
     */
    fun findByCategory(category: String): List<ProductoDTO> =
        productoRepository.findByCategory(category).map { it.toDTO() }

    /**
     * Crea un nuevo producto.
     * Solo ADMIN puede hacerlo.
     */
    fun create(dto: ProductoDTO): ProductoDTO {
        requireAdmin()
        if (productoRepository.existsById(dto.name)) {
            throw BadRequestException("Ya existe un producto con nombre '${dto.name}'")
        }
        val entity = dto.toEntity()
        return productoRepository.save(entity).toDTO()
    }

    /**
     * Actualiza un producto existente.
     * No permite cambiar la PK name.
     * Solo ADMIN puede hacerlo.
     */
    fun update(name: String, dto: ProductoDTO): ProductoDTO {
        requireAdmin()
        if (name != dto.name) {
            throw BadRequestException("El nombre del producto no puede modificarse")
        }
        val existing = productoRepository.findById(name)
            .orElseThrow { NotFoundException("Producto '$name' no encontrado") }
        val updated = existing.copy(
            category    = dto.category,
            stock       = dto.stock,
            description = dto.description,
            price       = dto.price,
            image       = dto.image
        )
        return productoRepository.save(updated).toDTO()
    }

    /**
     * Elimina un producto por su nombre.
     * Solo ADMIN puede hacerlo.
     */
    fun delete(name: String) {
        requireAdmin()
        if (!productoRepository.existsById(name)) {
            throw NotFoundException("Producto '$name' no encontrado")
        }
        productoRepository.deleteById(name)
    }

    fun updateStockForOrder(name: String, newStock: Int): ProductoDTO {
        val existing = productoRepository.findById(name)
            .orElseThrow { NotFoundException("Producto '$name' no encontrado") }
        val updated = existing.copy(stock = newStock)
        return productoRepository.save(updated).toDTO()
    }

    // -----------------------------------
    // Verificación de rol ADMIN interna
    // -----------------------------------
    private fun requireAdmin() {
        val auth = SecurityContextHolder.getContext().authentication
        val isAdmin = auth.authorities.any { it.authority == "ROLE_ADMIN" }
        if (!isAdmin) {
            throw ForbiddenException("Solo administradores pueden realizar esta operación")
        }
    }

    // ------------------------------------
    // Mappers internos (dentro del service)
    // ------------------------------------
    private fun Producto.toDTO(): ProductoDTO =
        ProductoDTO(
            name        = this.name,
            category    = this.category,
            stock       = this.stock,
            description = this.description,
            price       = this.price,
            image       = this.image
        )

    private fun ProductoDTO.toEntity(): Producto =
        Producto(
            name        = this.name,
            category    = this.category,
            stock       = this.stock,
            description = this.description,
            price       = this.price,
            image       = this.image
        )
}
