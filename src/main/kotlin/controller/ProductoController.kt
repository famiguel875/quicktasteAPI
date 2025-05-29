package com.es.controller

import com.es.dto.ProductoDTO
import com.es.error.exception.BadRequestException
import com.es.service.ProductoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/productos")
class ProductoController {

    @Autowired
    private lateinit var productoService: ProductoService

    /** GET /productos — todos los productos */
    @GetMapping
    fun getAll(): ResponseEntity<List<ProductoDTO>> =
        ResponseEntity.ok(productoService.findAll())

    /** GET /productos/{name} — busca uno por PK */
    @GetMapping("/{name}")
    fun getByName(@PathVariable name: String): ResponseEntity<ProductoDTO> =
        ResponseEntity.ok(productoService.findByName(name))

    /** GET /productos/categoria/{category} — filtra por categoría */
    @GetMapping("/categoria/{category}")
    fun getByCategory(@PathVariable category: String): ResponseEntity<List<ProductoDTO>> =
        ResponseEntity.ok(productoService.findByCategory(category))

    /** POST /productos — crea uno nuevo (solo ADMIN) */
    @PostMapping
    fun create(@RequestBody dto: ProductoDTO): ResponseEntity<ProductoDTO> {
        val created = productoService.create(dto)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    /** PUT /productos/{name} — actualiza (solo ADMIN) */
    @PutMapping("/{name}")
    fun update(
        @PathVariable name: String,
        @RequestBody dto: ProductoDTO
    ): ResponseEntity<ProductoDTO> =
        ResponseEntity.ok(productoService.update(name, dto))

    /** DELETE /productos/{name} — elimina (solo ADMIN) */
    @DeleteMapping("/{name}")
    fun delete(@PathVariable name: String): ResponseEntity<Void> {
        productoService.delete(name)
        return ResponseEntity.noContent().build()
    }

    /**
     * PUT /productos/{name}/stock
     * Actualiza sólo el stock de un producto.
     * Requiere un JWT válido (USER o ADMIN).
     */
    @PutMapping("/{name}/stock")
    fun updateStockForOrder(
        @PathVariable name: String,
        @RequestBody body: Map<String, Int>
    ): ResponseEntity<ProductoDTO> {
        val newStock = body["stock"]
            ?: throw BadRequestException("Debes indicar el nuevo stock en 'stock'")
        val updated = productoService.updateStockForOrder(name, newStock)
        return ResponseEntity.ok(updated)
    }
}
