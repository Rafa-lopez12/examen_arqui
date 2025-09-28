package com.example.tienda_emprendedor.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.ResultSet

class PagoDao {

    suspend fun obtenerMetodosPago(): List<Pago> = withContext(Dispatchers.IO) {
        val metodosPago = mutableListOf<Pago>()
        val connection = DatabaseConnection.obtenerConexion()

        try {
            val query = "SELECT * FROM metodo_pago ORDER BY id"
            val statement = connection?.prepareStatement(query)
            val resultSet = statement?.executeQuery()

            while (resultSet?.next() == true) {
                metodosPago.add(mapearPago(resultSet))
            }

            resultSet?.close()
            statement?.close()
        } catch (e: Exception) {
            println("Error al obtener métodos de pago: ${e.message}")
            e.printStackTrace()

            // Si la tabla no existe, retornar métodos predeterminados
            metodosPago.addAll(listOf(
                Pago(1, "efectivo"),
                Pago(2, "tarjeta")
            ))
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        metodosPago
    }

    suspend fun obtenerMetodoPagoPorId(id: Int): Pago? = withContext(Dispatchers.IO) {
        var metodoPago: Pago? = null
        val connection = DatabaseConnection.obtenerConexion()

        try {
            val query = "SELECT * FROM metodo_pago WHERE id = ?"
            val statement = connection?.prepareStatement(query)
            statement?.setInt(1, id)
            val resultSet = statement?.executeQuery()

            if (resultSet?.next() == true) {
                metodoPago = mapearPago(resultSet)
            }

            resultSet?.close()
            statement?.close()
        } catch (e: Exception) {
            println("Error al obtener método de pago por ID: ${e.message}")
            e.printStackTrace()

            // Retornar método predeterminado si hay error
            metodoPago = when (id) {
                1 -> Pago(1, "efectivo")
                2 -> Pago(2, "tarjeta")
                else -> null
            }
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        metodoPago
    }

    private fun mapearPago(resultSet: ResultSet): Pago {
        return Pago(
            id = resultSet.getInt("id"),
            nombre = resultSet.getString("nombre") ?: ""
        )
    }
}