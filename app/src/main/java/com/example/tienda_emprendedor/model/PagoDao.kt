package com.example.tienda_emprendedor.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement

class PagoDao {

    suspend fun obtenerPagosPorVenta(ventaId: Int): List<Pago> = withContext(Dispatchers.IO) {
        val pagos = mutableListOf<Pago>()
        val connection = DatabaseConnection.obtenerConexion()

        try {
            val query = """
                SELECT * FROM pago 
                WHERE venta_id = ? 
                ORDER BY fecha_pago DESC
            """.trimIndent()

            val statement = connection?.prepareStatement(query)
            statement?.setInt(1, ventaId)
            val resultSet = statement?.executeQuery()

            while (resultSet?.next() == true) {
                pagos.add(mapearPago(resultSet))
            }

            resultSet?.close()
            statement?.close()
        } catch (e: Exception) {
            println("Error al obtener pagos por venta: ${e.message}")
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        pagos
    }

    suspend fun insertarPago(pago: Pago): Int = withContext(Dispatchers.IO) {
        val connection = DatabaseConnection.obtenerConexion()
        var pagoId = 0

        try {
            val query = """
                INSERT INTO pago (venta_id, monto, metodo_pago, referencia, estado) 
                VALUES (?, ?, ?, ?, ?)
            """.trimIndent()

            val statement = connection?.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)
            statement?.setInt(1, pago.ventaId)
            statement?.setDouble(2, pago.monto)
            statement?.setString(3, pago.metodoPago)
            statement?.setString(4, pago.referencia)
            statement?.setString(5, pago.estado)

            val filasAfectadas = statement?.executeUpdate() ?: 0

            if (filasAfectadas > 0) {
                val generatedKeys = statement?.generatedKeys
                if (generatedKeys?.next() == true) {
                    pagoId = generatedKeys.getInt(1)
                }
            }

            statement?.close()
        } catch (e: Exception) {
            println("Error al insertar pago: ${e.message}")
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        pagoId
    }

    suspend fun actualizarEstadoPago(pagoId: Int, nuevoEstado: String, referencia: String = ""): Boolean = withContext(Dispatchers.IO) {
        val connection = DatabaseConnection.obtenerConexion()
        var exito = false

        try {
            val query = if (referencia.isNotEmpty()) {
                "UPDATE pago SET estado = ?, referencia = ? WHERE id = ?"
            } else {
                "UPDATE pago SET estado = ? WHERE id = ?"
            }

            val statement = connection?.prepareStatement(query)
            statement?.setString(1, nuevoEstado)

            if (referencia.isNotEmpty()) {
                statement?.setString(2, referencia)
                statement?.setInt(3, pagoId)
            } else {
                statement?.setInt(2, pagoId)
            }

            val filasAfectadas = statement?.executeUpdate() ?: 0
            exito = filasAfectadas > 0

            statement?.close()
        } catch (e: Exception) {
            println("Error al actualizar estado de pago: ${e.message}")
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        exito
    }

    suspend fun obtenerPagoPorId(id: Int): Pago? = withContext(Dispatchers.IO) {
        var pago: Pago? = null
        val connection = DatabaseConnection.obtenerConexion()

        try {
            val query = "SELECT * FROM pago WHERE id = ?"
            val statement = connection?.prepareStatement(query)
            statement?.setInt(1, id)
            val resultSet = statement?.executeQuery()

            if (resultSet?.next() == true) {
                pago = mapearPago(resultSet)
            }

            resultSet?.close()
            statement?.close()
        } catch (e: Exception) {
            println("Error al obtener pago por ID: ${e.message}")
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        pago
    }

    suspend fun obtenerTodosLosPagos(): List<Pago> = withContext(Dispatchers.IO) {
        val pagos = mutableListOf<Pago>()
        val connection = DatabaseConnection.obtenerConexion()

        try {
            val query = "SELECT * FROM pago ORDER BY fecha_pago DESC"
            val statement = connection?.prepareStatement(query)
            val resultSet = statement?.executeQuery()

            while (resultSet?.next() == true) {
                pagos.add(mapearPago(resultSet))
            }

            resultSet?.close()
            statement?.close()
        } catch (e: Exception) {
            println("Error al obtener todos los pagos: ${e.message}")
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        pagos
    }

    suspend fun calcularTotalPagadoVenta(ventaId: Int): Double = withContext(Dispatchers.IO) {
        var total = 0.0
        val connection = DatabaseConnection.obtenerConexion()

        try {
            val query = """
                SELECT SUM(monto) as total_pagado 
                FROM pago 
                WHERE venta_id = ? AND estado = 'completado'
            """.trimIndent()

            val statement = connection?.prepareStatement(query)
            statement?.setInt(1, ventaId)
            val resultSet = statement?.executeQuery()

            if (resultSet?.next() == true) {
                total = resultSet.getDouble("total_pagado")
            }

            resultSet?.close()
            statement?.close()
        } catch (e: Exception) {
            println("Error al calcular total pagado: ${e.message}")
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        total
    }

    suspend fun verificarVentaPagadaCompleta(ventaId: Int): Boolean = withContext(Dispatchers.IO) {
        var estaCompleta = false
        val connection = DatabaseConnection.obtenerConexion()

        try {
            val query = """
                SELECT v.total, 
                       COALESCE(SUM(p.monto), 0) as total_pagado
                FROM venta v
                LEFT JOIN pago p ON v.id = p.venta_id AND p.estado = 'completado'
                WHERE v.id = ?
                GROUP BY v.id, v.total
            """.trimIndent()

            val statement = connection?.prepareStatement(query)
            statement?.setInt(1, ventaId)
            val resultSet = statement?.executeQuery()

            if (resultSet?.next() == true) {
                val totalVenta = resultSet.getDouble("total")
                val totalPagado = resultSet.getDouble("total_pagado")
                estaCompleta = totalPagado >= totalVenta
            }

            resultSet?.close()
            statement?.close()
        } catch (e: Exception) {
            println("Error al verificar si venta está pagada completa: ${e.message}")
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        estaCompleta
    }

    private fun mapearPago(resultSet: ResultSet): Pago {
        return Pago(
            id = resultSet.getInt("id"),
            ventaId = resultSet.getInt("venta_id"),
            monto = resultSet.getDouble("monto"),
            metodoPago = resultSet.getString("metodo_pago") ?: "",
            fechaPago = resultSet.getTimestamp("fecha_pago")?.toString() ?: "",
            referencia = resultSet.getString("referencia") ?: "",
            estado = resultSet.getString("estado") ?: "pendiente"
        )
    }
}