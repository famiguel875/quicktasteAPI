package com.es.service

import com.es.dto.CategoriaDTO
import com.es.error.exception.BadRequestException
import com.es.error.exception.ForbiddenException
import com.es.error.exception.NotFoundException
import com.es.model.Categoria
import com.es.repository.CategoriaRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class CategoriaService(
    @Autowired private val categoriaRepository: CategoriaRepository
) {

    /**
     * Returns all categories.
     * Any authenticated user can access.
     */
    fun findAll(): List<CategoriaDTO> =
        categoriaRepository.findAll().map { it.toDTO() }

    /**
     * Finds a category by its name.
     * Any authenticated user can access.
     */
    fun findByName(name: String): CategoriaDTO {
        val categoria = categoriaRepository.findById(name)
            .orElseThrow { NotFoundException("Category '$name' not found") }
        return categoria.toDTO()
    }

    /**
     * Creates a new category.
     * Only ADMIN can execute.
     */
    fun create(dto: CategoriaDTO): CategoriaDTO {
        requireAdmin()
        if (categoriaRepository.existsById(dto.name)) {
            throw BadRequestException("Category '${dto.name}' already exists")
        }
        val entity = dto.toEntity()
        val saved = categoriaRepository.save(entity)
        return saved.toDTO()
    }

    /**
     * Updates the image of an existing category.
     * Does not allow changing the name (PK).
     * Only ADMIN can execute.
     */
    fun update(name: String, dto: CategoriaDTO): CategoriaDTO {
        requireAdmin()
        if (name != dto.name) {
            throw BadRequestException("Category name cannot be changed")
        }
        val existing = categoriaRepository.findById(name)
            .orElseThrow { NotFoundException("Category '$name' not found") }
        val updated = existing.copy(image = dto.image)
        return categoriaRepository.save(updated).toDTO()
    }

    /**
     * Deletes a category by its name.
     * Only ADMIN can execute.
     */
    fun delete(name: String) {
        requireAdmin()
        if (!categoriaRepository.existsById(name)) {
            throw NotFoundException("Category '$name' not found")
        }
        categoriaRepository.deleteById(name)
    }

    // -----------------------------------
    // Internal ADMIN role check
    // -----------------------------------
    private fun requireAdmin() {
        val auth = SecurityContextHolder.getContext().authentication
        val isAdmin = auth.authorities.any { it.authority == "ROLE_ADMIN" }
        if (!isAdmin) {
            throw ForbiddenException("Only administrators can perform this operation")
        }
    }

    // ----------------------------
    // Internal mappers in service
    // ----------------------------
    private fun Categoria.toDTO(): CategoriaDTO =
        CategoriaDTO(
            name  = this.name,
            image = this.image
        )

    private fun CategoriaDTO.toEntity(): Categoria =
        Categoria(
            name  = this.name,
            image = this.image
        )
}