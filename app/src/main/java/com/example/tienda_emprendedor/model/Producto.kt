package com.example.tienda_emprendedor.model

class Producto(
    var id: Int = 0,
    var nombre: String = "",
    var descripcion: String = "",
    var precio: Double = 0.0,
    var categoria: String = "",
    var stock: Int = 0,
    var imagen: String = ""
)