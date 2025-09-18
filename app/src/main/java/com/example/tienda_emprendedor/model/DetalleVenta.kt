package com.example.tienda_emprendedor.model

data class DetalleVenta(
    var ventaId: Int = 0,
    var productoId: Int = 0,
    var cantidad: Int = 1,
    var precioUnitario: Double = 0.0,
    var subtotal: Double = 0.0,

    var nombreProducto: String = "",
    var descripcionProducto: String = "",
    var stockDisponible: Int = 0,
    var nombreCategoria: String = "",
    var subcategoria: String = ""
) {
    fun calcularSubtotal(): Double {
        return cantidad * precioUnitario
    }
}