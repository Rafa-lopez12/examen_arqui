package com.example.tienda_emprendedor.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

class DetalleVentaDao {

    suspend fun obtenerDetallesPorVenta(ventaId: Int): List<DetalleVenta> = withContext(Dispatchers.IO) {
        val detalles = mutableListOf<DetalleVenta>()
        val connection = DatabaseConnection.obtenerConexion()

        try {
            val query = """
                SELECT dv.*, 
                       p.nombre as producto_nombre, 
                       p.descripcion as producto_descripcion,
                       p.stock, 
                       c.nombre as categoria_nombre, 
                       c.subcategoria
                FROM detalle_venta dv
                LEFT JOIN producto p ON dv.producto_id = p.id
                LEFT JOIN categoria c ON p.categoria_id = c.id
                WHERE dv.venta_id = ?
                ORDER BY dv.producto_id
            """.trimIndent()

            val statement = connection?.prepareStatement(query)
            statement?.setInt(1, ventaId)
            val resultSet = statement?.executeQuery()

            while (resultSet?.next() == true) {
                detalles.add(mapearDetalleVenta(resultSet))
            }

            resultSet?.close()
            statement?.close()
        } catch (e: Exception) {
            println("Error al obtener detalles de venta: ${e.message}")
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        detalles
    }

    suspend fun insertarDetalles(
        connection: Connection?,
        ventaId: Int,
        detalles: List<DetalleVenta>
    ): Boolean = withContext(Dispatchers.IO) {
        var exito = true

        try {
            if (detalles.isEmpty()) return@withContext true

            val query = """
                INSERT INTO detalle_venta (venta_id, producto_id, cantidad, precio_unitario, subtotal) 
                VALUES (?, ?, ?, ?, ?)
            """.trimIndent()
            detalles.forEach { detalle ->
                val statement = connection?.prepareStatement(query)
                statement?.setInt(1, ventaId)
                statement?.setInt(2, detalle.productoId)
                statement?.setInt(3, detalle.cantidad)
                statement?.setDouble(4, detalle.precioUnitario)
                statement?.setDouble(5, detalle.calcularSubtotal())

                val filasAfectadas = statement?.executeUpdate() ?: 0
                if (filasAfectadas <= 0) {
                    exito = false
                    println("Error insertando detalle para producto ID: ${detalle.productoId}")
                }

                statement?.close()
            }

            if (exito) {
                println("Detalles insertados exitosamente: ${detalles.size} items")
            }
        } catch (e: Exception) {
            println("Error al insertar detalles de venta: ${e.message}")
            e.printStackTrace()
            exito = false
        }

        exito
    }



    private fun mapearDetalleVenta(resultSet: ResultSet): DetalleVenta {
        return DetalleVenta(
            ventaId = resultSet.getInt("venta_id"),
            productoId = resultSet.getInt("producto_id"),
            cantidad = resultSet.getInt("cantidad"),
            precioUnitario = resultSet.getDouble("precio_unitario"),
            subtotal = resultSet.getDouble("subtotal"),
            nombreProducto = resultSet.getString("producto_nombre") ?: "",
            descripcionProducto = resultSet.getString("producto_descripcion") ?: "",
            stockDisponible = resultSet.getInt("stock"),
            nombreCategoria = resultSet.getString("categoria_nombre") ?: "",
            subcategoria = resultSet.getString("subcategoria") ?: ""
        )
    }
}