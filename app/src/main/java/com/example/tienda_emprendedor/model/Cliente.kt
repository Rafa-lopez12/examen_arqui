package com.example.tienda_emprendedor.model

data class Cliente(
    var id: Int = 0,
    var nombre: String = "",
    var apellido: String = "",
    var telefono: String = "",
    var email: String = "",
    var direccion: String = "",
    var dni: String = "", // O CI según tu país
    var fechaRegistro: String = "", // En formato ISO: "2025-01-15"
    var activo: Boolean = true
)