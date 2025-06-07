package com.es.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "pedidos")
data class Pedido(
    @Id val id: String? = null,      // PK autogenerado
    val userEmail: String,           // FK → Usuario.email
    val productos: List<String>,     // Lista de Producto.name
    val cantidad: Int,               // total de ítems
    val coste: Double,               // suma de price×cantidad
    val direccion: String,
    val status: PedidoStatus = PedidoStatus.PENDING
)

