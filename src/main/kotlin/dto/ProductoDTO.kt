package com.es.dto

data class ProductoDTO(
    val name: String,
    val category: String,
    val stock: Int,
    val description: String,
    val price: Double,
    val image: String?      // incluimos imagen en el DTO
)