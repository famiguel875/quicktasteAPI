package com.es.repository

import com.es.model.Pedido
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface PedidoRepository : MongoRepository<Pedido, String> {
    fun findByUserEmail(userEmail: String): List<Pedido>
}