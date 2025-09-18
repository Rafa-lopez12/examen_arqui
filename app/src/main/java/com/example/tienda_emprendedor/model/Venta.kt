package com.example.tienda_emprendedor.model

data class Venta(
    var id: Int = 0,
    var clienteId: Int = 0,
    var fechaVenta: String = "", // timestamp
    var total: Double = 0.0,
    var descuento: Double = 0.0,
    var impuestos: Double = 0.0,
    var metodoPago: String = "",
    var estado: String = "pendiente", // "pendiente", "completada", "cancelada"
    var notas: String = "",

    // Para mostrar en la UI (no se guarda en BD)
    var nombreCliente: String = "",
    var apellidoCliente: String = "",
    var detalles: List<DetalleVenta> = listOf() // Lista de productos
)