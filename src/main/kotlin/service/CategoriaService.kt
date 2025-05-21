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
     * Devuelve todas las categorías.
     * Cualquiera autenticado puede acceder.
     */
    fun findAll(): List<CategoriaDTO> =
        categoriaRepository.findAll().map { it.toDTO() }

    /**
     * Busca una categoría por su nombre.
     * Cualquiera autenticado puede acceder.
     */
    fun findByName(name: String): CategoriaDTO {
        val categoria = categoriaRepository.findById(name)
            .orElseThrow { NotFoundException("Categoría '$name' no encontrada") }
        return categoria.toDTO()
    }

    /**
     * Crea una nueva categoría.
     * Solo ADMIN puede ejecutarlo.
     */
    fun create(dto: CategoriaDTO): CategoriaDTO {
        requireAdmin()
        if (categoriaRepository.existsById(dto.name)) {
            throw BadRequestException("Ya existe la categoría '${dto.name}'")
        }
        val entity = dto.toEntity()
        val saved = categoriaRepository.save(entity)
        return saved.toDTO()
    }

    /**
     * Actualiza la imagen de una categoría existente.
     * No permite cambiar el nombre (PK).
     * Solo ADMIN puede ejecutarlo.
     */
    fun update(name: String, dto: CategoriaDTO): CategoriaDTO {
        requireAdmin()
        if (name != dto.name) {
            throw BadRequestException("El nombre de la categoría no puede modificarse")
        }
        val existing = categoriaRepository.findById(name)
            .orElseThrow { NotFoundException("Categoría '$name' no encontrada") }
        val updated = existing.copy(image = dto.image)
        return categoriaRepository.save(updated).toDTO()
    }

    /**
     * Elimina una categoría por su nombre.
     * Solo ADMIN puede ejecutarlo.
     */
    fun delete(name: String) {
        requireAdmin()
        if (!categoriaRepository.existsById(name)) {
            throw NotFoundException("Categoría '$name' no encontrada")
        }
        categoriaRepository.deleteById(name)
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

    // ----------------------------
    // Mappers internos al service
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

