package com.es.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "productos")
data class Producto(
    @Id val name: String,        // PK
    val category: String,    // FK → Categoria.name
    val stock: Int,
    val description: String,
    val price: Double,
    val image: String?       // ruta o URL de la imagen
)