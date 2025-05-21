package com.es.repository

import com.es.model.Producto
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ProductoRepository : MongoRepository<Producto, String> {
    fun findByCategory(category: String): List<Producto>
}