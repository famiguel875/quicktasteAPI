package com.es.dto

import com.es.model.PedidoStatus

data class PedidoDTO(
    val id: String? = null,
    val userEmail: String,
    val productos: List<String>,
    val cantidad: Int,
    val coste: Double,
    val direccion: String,
    val status: PedidoStatus = PedidoStatus.PENDING
)
