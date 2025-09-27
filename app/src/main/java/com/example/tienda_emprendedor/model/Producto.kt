package com.example.tienda_emprendedor.model

data class Producto(
    var id: Int = 0,
    var nombre: String = "",
    var descripcion: String = "",
    var precio: Double = 0.0,
    var categoriaId: Int = 0,
    var subcategoria: String = "",
    var stock: Int = 0,
    var imagen: String = "",

    var nombreCategoria: String = ""
)