package com.example.tienda_emprendedor.model

data class Categoria(
    var id: Int = 0,
    var nombre: String = "",
    var subcategoria: String = "",
    var descripcion: String = "",
    var activo: Boolean = true
)