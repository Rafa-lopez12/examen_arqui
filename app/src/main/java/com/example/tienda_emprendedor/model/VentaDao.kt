package com.example.tienda_emprendedor.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import com.example.tienda_emprendedor.model.DetalleVentaDao

class VentaDao {

    private val detalleVentaDao = DetalleVentaDao()

    suspend fun obtenerTodasLasVentas(): List<Venta> = withContext(Dispatchers.IO) {
        val ventas = mutableListOf<Venta>()
        val connection = DatabaseConnection.obtenerConexion()

        try {
            val query = """
                SELECT v.*, 
                       c.nombre as cliente_nombre, 
                       c.apellido as cliente_apellido,
                       mp.nombre as metodo_pago_nombre
                FROM venta v
                LEFT JOIN cliente c ON v.cliente_id = c.id
                LEFT JOIN metodo_pago mp ON v.pago_id = mp.id
                ORDER BY v.fecha_venta DESC
            """.trimIndent()

            val statement = connection?.prepareStatement(query)
            val resultSet = statement?.executeQuery()

            while (resultSet?.next() == true) {
                ventas.add(mapearVenta(resultSet))
            }

            resultSet?.close()
            statement?.close()
        } catch (e: Exception) {
            println("Error al obtener ventas: ${e.message}")
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        ventas
    }

    suspend fun obtenerVentaPorId(id: Int): Venta? = withContext(Dispatchers.IO) {
        var venta: Venta? = null
        val connection = DatabaseConnection.obtenerConexion()

        try {
            val query = """
                SELECT v.*, 
                       c.nombre as cliente_nombre, 
                       c.apellido as cliente_apellido,
                       mp.nombre as metodo_pago_nombre
                FROM venta v
                LEFT JOIN cliente c ON v.cliente_id = c.id
                LEFT JOIN metodo_pago mp ON v.pago_id = mp.id
                WHERE v.id = ?
            """.trimIndent()

            val statement = connection?.prepareStatement(query)
            statement?.setInt(1, id)
            val resultSet = statement?.executeQuery()

            if (resultSet?.next() == true) {
                venta = mapearVenta(resultSet)
                venta?.detalles = detalleVentaDao.obtenerDetallesPorVenta(id)
            }

            resultSet?.close()
            statement?.close()
        } catch (e: Exception) {
            println("Error al obtener venta por ID: ${e.message}")
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        venta
    }

    suspend fun generarVenta(venta: Venta, detalles: List<DetalleVenta>): Int = withContext(Dispatchers.IO) {
        val connection = DatabaseConnection.obtenerConexion()
        var ventaId = 0

        try {
            connection?.autoCommit = false

            val queryVenta = """
            INSERT INTO venta (cliente_id, total, descuento, impuestos, pago_id, estado, notas) 
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

            val statementVenta = connection?.prepareStatement(queryVenta, Statement.RETURN_GENERATED_KEYS)
            statementVenta?.setInt(1, venta.clienteId)
            statementVenta?.setDouble(2, venta.total)
            statementVenta?.setDouble(3, venta.descuento)
            statementVenta?.setDouble(4, venta.impuestos)
            statementVenta?.setInt(5, venta.pagoId)
            statementVenta?.setString(6, venta.estado)
            statementVenta?.setString(7, venta.notas)

            val filasAfectadas = statementVenta?.executeUpdate() ?: 0

            if (filasAfectadas > 0) {
                val generatedKeys = statementVenta?.generatedKeys
                if (generatedKeys?.next() == true) {
                    ventaId = generatedKeys.getInt(1)
                    println("Venta insertada con ID: $ventaId")
                }
            }

            statementVenta?.close()

            if (ventaId > 0 && detalles.isNotEmpty()) {
                val detallesExitoso = detalleVentaDao.insertarDetalles(connection, ventaId, detalles)

                if (!detallesExitoso) {
                    throw Exception("Error al insertar detalles de venta")
                }
            }

            actualizarStockProductos(connection, detalles)

            connection?.commit()
            println("Venta creada exitosamente con ID: $ventaId")

        } catch (e: Exception) {
            connection?.rollback()
            println("Error al insertar venta: ${e.message}")
            e.printStackTrace()
            ventaId = 0
        } finally {
            connection?.autoCommit = true
            DatabaseConnection.cerrarConexion(connection)
        }

        ventaId
    }

    suspend fun actualizarEstadoVenta(ventaId: Int, nuevoEstado: String): Boolean = withContext(Dispatchers.IO) {
        val connection = DatabaseConnection.obtenerConexion()
        var exito = false

        try {
            val query = "UPDATE venta SET estado = ? WHERE id = ?"
            val statement = connection?.prepareStatement(query)
            statement?.setString(1, nuevoEstado)
            statement?.setInt(2, ventaId)

            val filasAfectadas = statement?.executeUpdate() ?: 0
            exito = filasAfectadas > 0

            statement?.close()
        } catch (e: Exception) {
            println("Error al actualizar estado de venta: ${e.message}")
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        exito
    }

    suspend fun obtenerVentasPorCliente(clienteId: Int): List<Venta> = withContext(Dispatchers.IO) {
        val ventas = mutableListOf<Venta>()
        val connection = DatabaseConnection.obtenerConexion()

        try {
            val query = """
                SELECT v.*, 
                       c.nombre as cliente_nombre, 
                       c.apellido as cliente_apellido,
                       mp.nombre as metodo_pago_nombre
                FROM venta v
                LEFT JOIN cliente c ON v.cliente_id = c.id
                LEFT JOIN metodo_pago mp ON v.pago_id = mp.id
                WHERE v.cliente_id = ?
                ORDER BY v.fecha_venta DESC
            """.trimIndent()

            val statement = connection?.prepareStatement(query)
            statement?.setInt(1, clienteId)
            val resultSet = statement?.executeQuery()

            while (resultSet?.next() == true) {
                ventas.add(mapearVenta(resultSet))
            }

            resultSet?.close()
            statement?.close()
        } catch (e: Exception) {
            println("Error al obtener ventas por cliente: ${e.message}")
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        ventas
    }

    private suspend fun obtenerDetallesVenta(connection: Connection?, ventaId: Int): List<DetalleVenta> {
        val detalles = mutableListOf<DetalleVenta>()

        try {
            val query = """
                SELECT dv.*, p.nombre as producto_nombre, p.descripcion as producto_descripcion,
                       p.stock, c.nombre as categoria_nombre, c.subcategoria
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
        }

        return detalles
    }

    private fun actualizarStockProductos(connection: Connection?, detalles: List<DetalleVenta>) {
        try {
            val query = "UPDATE producto SET stock = stock - ? WHERE id = ?"

            detalles.forEach { detalle ->
                val statement = connection?.prepareStatement(query)
                statement?.setInt(1, detalle.cantidad)
                statement?.setInt(2, detalle.productoId)

                val filasAfectadas = statement?.executeUpdate() ?: 0
                if (filasAfectadas <= 0) {
                    println("⚠No se pudo actualizar stock para producto ID: ${detalle.productoId}")
                }

                statement?.close()
            }

            println("Stock actualizado para ${detalles.size} productos")

        } catch (e: Exception) {
            println("Error al actualizar stock de productos: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    private fun mapearVenta(resultSet: ResultSet): Venta {
        return Venta(
            id = resultSet.getInt("id"),
            clienteId = resultSet.getInt("cliente_id"),
            fechaVenta = resultSet.getTimestamp("fecha_venta")?.toString() ?: "",
            total = resultSet.getDouble("total"),
            descuento = resultSet.getDouble("descuento"),
            impuestos = resultSet.getDouble("impuestos"),
            pagoId = resultSet.getInt("pago_id"),
            estado = resultSet.getString("estado") ?: "pendiente",
            notas = resultSet.getString("notas") ?: "",
            nombreCliente = resultSet.getString("cliente_nombre") ?: "",
            apellidoCliente = resultSet.getString("cliente_apellido") ?: "",
            metodoPago = resultSet.getString("metodo_pago_nombre") ?: ""
        )
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