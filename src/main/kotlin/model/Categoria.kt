package com.es.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "categorias")
data class Categoria(
    @Id val name: String,    // PK
    val image: String?
)