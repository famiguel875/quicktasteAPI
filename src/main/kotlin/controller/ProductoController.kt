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

    /** GET /productos — all products */
    @GetMapping
    fun getAll(): ResponseEntity<List<ProductoDTO>> =
        ResponseEntity.ok(productoService.findAll())

    /** GET /productos/{name} — find one by PK */
    @GetMapping("/{name}")
    fun getByName(@PathVariable name: String): ResponseEntity<ProductoDTO> =
        ResponseEntity.ok(productoService.findByName(name))

    /** GET /productos/categoria/{category} — filter by category */
    @GetMapping("/categoria/{category}")
    fun getByCategory(@PathVariable category: String): ResponseEntity<List<ProductoDTO>> =
        ResponseEntity.ok(productoService.findByCategory(category))

    /** POST /productos — create new (only ADMIN) */
    @PostMapping
    fun create(@RequestBody dto: ProductoDTO): ResponseEntity<ProductoDTO> {
        val created = productoService.create(dto)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    /** PUT /productos/{name} — update (only ADMIN) */
    @PutMapping("/{name}")
    fun update(
        @PathVariable name: String,
        @RequestBody dto: ProductoDTO
    ): ResponseEntity<ProductoDTO> =
        ResponseEntity.ok(productoService.update(name, dto))

    /** DELETE /productos/{name} — delete (only ADMIN) */
    @DeleteMapping("/{name}")
    fun delete(@PathVariable name: String): ResponseEntity<Void> {
        productoService.delete(name)
        return ResponseEntity.noContent().build()
    }

    /**
     * PUT /productos/{name}/stock
     * Updates only product stock.
     * Requires a valid JWT (USER or ADMIN).
     */
    @PutMapping("/{name}/stock")
    fun updateStockForOrder(
        @PathVariable name: String,
        @RequestBody body: Map<String, Int>
    ): ResponseEntity<ProductoDTO> {
        val newStock = body["stock"]
            ?: throw BadRequestException("You must specify the new stock in 'stock'")
        val updated = productoService.updateStockForOrder(name, newStock)
        return ResponseEntity.ok(updated)
    }

    /**
     * PUT /productos/{name}/price
     * Updates only product price.
     * Requires a valid JWT (USER or ADMIN).
     */
    @PutMapping("/{name}/price")
    fun updatePrice(
        @PathVariable name: String,
        @RequestBody body: Map<String, Double>
    ): ResponseEntity<ProductoDTO> {
        val newPrice = body["price"]
            ?: throw BadRequestException("You must specify the new price in 'price'")
        val updated = productoService.updatePrice(name, newPrice)
        return ResponseEntity.ok(updated)
    }

    /**
     * PUT /productos/{name}/image
     * Updates only product image URL.
     * Requires ADMIN.
     */
    @PutMapping("/{name}/image")
    fun updateImage(
        @PathVariable name: String,
        @RequestBody body: Map<String, String>
    ): ResponseEntity<ProductoDTO> {
        val newImage = body["image"]
            ?: throw BadRequestException("You must specify the new image URL in 'image'")
        val updated = productoService.updateImage(name, newImage)
        return ResponseEntity.ok(updated)
    }
}