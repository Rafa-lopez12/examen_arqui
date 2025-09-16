package com.example.tienda_emprendedor.model

data class Producto(
    var id: Int = 0,
    var nombre: String = "",
    var descripcion: String = "",
    var precio: Double = 0.0,
    var categoriaId: Int = 0, // 🔗 RELACIÓN 1:N con Categoria
    var subcategoria: String = "", // "Residencial", "Comercial", "Compacto", etc.
    var stock: Int = 0,
    var imagen: String = "",

    // Para mostrar en la UI (no se guarda en BD)
    var nombreCategoria: String = "" // "Split", "Ventana", etc.
)