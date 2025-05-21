package com.es.repository

import com.es.model.Categoria
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface CategoriaRepository : MongoRepository<Categoria, String>