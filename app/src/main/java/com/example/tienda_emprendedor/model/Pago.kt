package com.example.tienda_emprendedor.model

data class Pago(
    var id: Int = 0,
    var ventaId: Int = 0,
    var monto: Double = 0.0,
    var metodoPago: String = "",
    var fechaPago: String = "",
    var referencia: String = "",
    var estado: String = "pendiente"
)