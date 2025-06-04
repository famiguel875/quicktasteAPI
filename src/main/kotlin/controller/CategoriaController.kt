package com.es.controller

import com.es.dto.CategoriaDTO
import com.es.service.CategoriaService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/categorias")
class CategoriaController {

    @Autowired
    private lateinit var categoriaService: CategoriaService

    /**
     * GET /categorias
     * Returns all categories.
     */
    @GetMapping
    fun getAll(): ResponseEntity<List<CategoriaDTO>> {
        val categorias = categoriaService.findAll()
        return ResponseEntity.ok(categorias)
    }

    /**
     * GET /categorias/{name}
     * Returns a category by name.
     */
    @GetMapping("/{name}")
    fun getByName(@PathVariable name: String): ResponseEntity<CategoriaDTO> {
        val dto = categoriaService.findByName(name)
        return ResponseEntity.ok(dto)
    }

    /**
     * POST /categorias
     * Creates a new category. Only ADMIN.
     */
    @PostMapping
    fun create(@RequestBody dto: CategoriaDTO): ResponseEntity<CategoriaDTO> {
        val created = categoriaService.create(dto)
        return ResponseEntity(created, HttpStatus.CREATED)
    }

    /**
     * PUT /categorias/{name}
     * Updates the image of an existing category. Only ADMIN.
     */
    @PutMapping("/{name}")
    fun update(
        @PathVariable name: String,
        @RequestBody dto: CategoriaDTO
    ): ResponseEntity<CategoriaDTO> {
        val updated = categoriaService.update(name, dto)
        return ResponseEntity.ok(updated)
    }

    /**
     * DELETE /categorias/{name}
     * Deletes a category by name. Only ADMIN.
     */
    @DeleteMapping("/{name}")
    fun delete(@PathVariable name: String): ResponseEntity<Void> {
        categoriaService.delete(name)
        return ResponseEntity.noContent().build()
    }
}