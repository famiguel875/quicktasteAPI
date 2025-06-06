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
     * Returns all products.
     * Any authenticated user can access.
     */
    fun findAll(): List<ProductoDTO> =
        productoRepository.findAll().map { it.toDTO() }

    /**
     * Finds a product by its name (PK).
     * Any authenticated user can access.
     */
    fun findByName(name: String): ProductoDTO {
        val producto = productoRepository.findById(name)
            .orElseThrow { NotFoundException("Product '$name' not found") }
        return producto.toDTO()
    }

    /**
     * Lists all products in a given category.
     * Any authenticated user can access.
     */
    fun findByCategory(category: String): List<ProductoDTO> =
        productoRepository.findByCategory(category).map { it.toDTO() }

    /**
     * Creates a new product.
     * Only ADMIN can execute.
     */
    fun create(dto: ProductoDTO): ProductoDTO {
        requireAdmin()
        if (productoRepository.existsById(dto.name)) {
            throw BadRequestException("A product with name '${dto.name}' already exists")
        }
        val entity = dto.toEntity()
        return productoRepository.save(entity).toDTO()
    }

    /**
     * Updates an existing product.
     * Does not allow changing the PK name.
     * Only ADMIN can execute.
     */
    fun update(name: String, dto: ProductoDTO): ProductoDTO {
        requireAdmin()
        if (name != dto.name) {
            throw BadRequestException("Product name cannot be changed")
        }
        val existing = productoRepository.findById(name)
            .orElseThrow { NotFoundException("Product '$name' not found") }
        val updated = existing.copy(
            category    = dto.category,
            stock       = dto.stock,
            description = dto.description,
            price       = dto.price,
            image       = dto.image,
            allowedEmails = dto.allowedEmails
        )
        return productoRepository.save(updated).toDTO()
    }

    /**
     * Deletes a product by its name.
     * Only ADMIN can execute.
     */
    fun delete(name: String) {
        requireAdmin()
        if (!productoRepository.existsById(name)) {
            throw NotFoundException("Product '$name' not found")
        }
        productoRepository.deleteById(name)
    }

    fun updateStockForOrder(name: String, newStock: Int): ProductoDTO {
        val existing = productoRepository.findById(name)
            .orElseThrow { NotFoundException("Product '$name' not found") }
        val updated = existing.copy(stock = newStock)
        return productoRepository.save(updated).toDTO()
    }

    fun updatePrice(name: String, newPrice: Double): ProductoDTO {
        requireAdmin()  // Verifies that the caller has ROLE_ADMIN
        val existing = productoRepository.findById(name)
            .orElseThrow { NotFoundException("Product '$name' not found") }
        val updated = existing.copy(price = newPrice)
        return productoRepository.save(updated).toDTO()
    }

    /**
     * Updates only the image of a product.
     * Does not allow changing the name (PK).
     * Only ADMIN can execute.
     */
    fun updateImage(name: String, newImage: String): ProductoDTO {
        requireAdmin()
        val existing = productoRepository.findById(name)
            .orElseThrow { NotFoundException("Product '$name' not found") }
        val updated = existing.copy(image = newImage)
        return productoRepository.save(updated).toDTO()
    }

    // -----------------------------------
    // Internal ADMIN role verification
    // -----------------------------------
    private fun requireAdmin() {
        val auth = SecurityContextHolder.getContext().authentication
        val isAdmin = auth.authorities.any { it.authority == "ROLE_ADMIN" }
        if (!isAdmin) {
            throw ForbiddenException("Only administrators can perform this operation")
        }
    }

    // ------------------------------------
    // Internal mappers (within the service)
    // ------------------------------------
    private fun Producto.toDTO(): ProductoDTO =
        ProductoDTO(
            name        = this.name,
            category    = this.category,
            stock       = this.stock,
            description = this.description,
            price       = this.price,
            image       = this.image,
            allowedEmails = this.allowedEmails
        )

    private fun ProductoDTO.toEntity(): Producto =
        Producto(
            name        = this.name,
            category    = this.category,
            stock       = this.stock,
            description = this.description,
            price       = this.price,
            image       = this.image,
            allowedEmails = this.allowedEmails
        )
}