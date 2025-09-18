package com.example.tienda_emprendedor.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.text.SimpleDateFormat
import java.util.*

class ClienteDao {

    suspend fun obtenerTodosLosClientes(): List<Cliente> = withContext(Dispatchers.IO) {
        val clientes = mutableListOf<Cliente>()
        val connection = DatabaseConnection.obtenerConexion()

        try {
            val query = "SELECT * FROM cliente WHERE activo = true ORDER BY apellido, nombre"
            val statement = connection?.prepareStatement(query)
            val resultSet = statement?.executeQuery()

            while (resultSet?.next() == true) {
                clientes.add(mapearCliente(resultSet))
            }

            resultSet?.close()
            statement?.close()
        } catch (e: Exception) {
            println("Error al obtener clientes: ${e.message}")
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        clientes
    }

    suspend fun obtenerClientePorId(id: Int): Cliente? = withContext(Dispatchers.IO) {
        var cliente: Cliente? = null
        val connection = DatabaseConnection.obtenerConexion()

        try {
            val query = "SELECT * FROM cliente WHERE id = ? AND activo = true"
            val statement = connection?.prepareStatement(query)
            statement?.setInt(1, id)
            val resultSet = statement?.executeQuery()

            if (resultSet?.next() == true) {
                cliente = mapearCliente(resultSet)
            }

            resultSet?.close()
            statement?.close()
        } catch (e: Exception) {
            println("Error al obtener cliente por ID: ${e.message}")
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        cliente
    }

    suspend fun buscarClientesPorNombre(busqueda: String): List<Cliente> = withContext(Dispatchers.IO) {
        val clientes = mutableListOf<Cliente>()
        val connection = DatabaseConnection.obtenerConexion()

        try {
            val query = """
                SELECT * FROM cliente 
                WHERE activo = true AND (
                    LOWER(nombre) LIKE LOWER(?) OR 
                    LOWER(apellido) LIKE LOWER(?) OR
                    LOWER(CONCAT(nombre, ' ', apellido)) LIKE LOWER(?)
                )
                ORDER BY apellido, nombre
            """.trimIndent()

            val statement = connection?.prepareStatement(query)
            val parametroBusqueda = "%$busqueda%"
            statement?.setString(1, parametroBusqueda)
            statement?.setString(2, parametroBusqueda)
            statement?.setString(3, parametroBusqueda)

            val resultSet = statement?.executeQuery()

            while (resultSet?.next() == true) {
                clientes.add(mapearCliente(resultSet))
            }

            resultSet?.close()
            statement?.close()
        } catch (e: Exception) {
            println("Error al buscar clientes: ${e.message}")
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        clientes
    }

    suspend fun insertarCliente(cliente: Cliente): Boolean = withContext(Dispatchers.IO) {
        val connection = DatabaseConnection.obtenerConexion()
        var exito = false

        try {
            val query = """
                INSERT INTO cliente (nombre, apellido, telefono, email, direccion, dni, fecha_registro, activo) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            val statement = connection?.prepareStatement(query)
            statement?.setString(1, cliente.nombre)
            statement?.setString(2, cliente.apellido)
            statement?.setString(3, cliente.telefono)
            statement?.setString(4, cliente.email)
            statement?.setString(5, cliente.direccion)
            statement?.setString(6, cliente.dni)
            // Convertir fecha string a java.sql.Date (compatible con API 24)
            val fechaRegistro = if (cliente.fechaRegistro.isNotEmpty()) {
                try {
                    java.sql.Date.valueOf(cliente.fechaRegistro)
                } catch (e: Exception) {
                    // Si hay error, usar fecha actual
                    java.sql.Date(System.currentTimeMillis())
                }
            } else {
                // Usar fecha actual
                java.sql.Date(System.currentTimeMillis())
            }
            statement?.setDate(7, fechaRegistro)
            statement?.setBoolean(8, cliente.activo)

            val filasAfectadas = statement?.executeUpdate() ?: 0
            exito = filasAfectadas > 0

            statement?.close()
        } catch (e: Exception) {
            println("Error al insertar cliente: ${e.message}")
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        exito
    }

    suspend fun actualizarCliente(cliente: Cliente): Boolean = withContext(Dispatchers.IO) {
        val connection = DatabaseConnection.obtenerConexion()
        var exito = false

        try {
            val query = """
                UPDATE cliente 
                SET nombre = ?, apellido = ?, telefono = ?, email = ?, direccion = ?, dni = ?, activo = ?
                WHERE id = ?
            """.trimIndent()

            val statement = connection?.prepareStatement(query)
            statement?.setString(1, cliente.nombre)
            statement?.setString(2, cliente.apellido)
            statement?.setString(3, cliente.telefono)
            statement?.setString(4, cliente.email)
            statement?.setString(5, cliente.direccion)
            statement?.setString(6, cliente.dni)
            statement?.setBoolean(7, cliente.activo)
            statement?.setInt(8, cliente.id)

            val filasAfectadas = statement?.executeUpdate() ?: 0
            exito = filasAfectadas > 0

            statement?.close()
        } catch (e: Exception) {
            println("Error al actualizar cliente: ${e.message}")
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        exito
    }

    suspend fun eliminarCliente(id: Int): Boolean = withContext(Dispatchers.IO) {
        val connection = DatabaseConnection.obtenerConexion()
        var exito = false

        try {
            // Soft delete - marcamos como inactivo
            val query = "UPDATE cliente SET activo = false WHERE id = ?"
            val statement = connection?.prepareStatement(query)
            statement?.setInt(1, id)

            val filasAfectadas = statement?.executeUpdate() ?: 0
            exito = filasAfectadas > 0

            statement?.close()
        } catch (e: Exception) {
            println("Error al eliminar cliente: ${e.message}")
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        exito
    }

    suspend fun verificarDniExistente(dni: String, idActual: Int = 0): Boolean = withContext(Dispatchers.IO) {
        var existe = false
        val connection = DatabaseConnection.obtenerConexion()

        try {
            val query = "SELECT COUNT(*) as total FROM cliente WHERE dni = ? AND id != ? AND activo = true"
            val statement = connection?.prepareStatement(query)
            statement?.setString(1, dni)
            statement?.setInt(2, idActual)
            val resultSet = statement?.executeQuery()

            if (resultSet?.next() == true) {
                existe = resultSet.getInt("total") > 0
            }

            resultSet?.close()
            statement?.close()
        } catch (e: Exception) {
            println("Error al verificar DNI: ${e.message}")
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        existe
    }

    suspend fun contarVentasCliente(clienteId: Int): Int = withContext(Dispatchers.IO) {
        var cantidad = 0
        val connection = DatabaseConnection.obtenerConexion()

        try {
            val query = "SELECT COUNT(*) as total FROM venta WHERE cliente_id = ?"
            val statement = connection?.prepareStatement(query)
            statement?.setInt(1, clienteId)
            val resultSet = statement?.executeQuery()

            if (resultSet?.next() == true) {
                cantidad = resultSet.getInt("total")
            }

            resultSet?.close()
            statement?.close()
        } catch (e: Exception) {
            println("Error al contar ventas del cliente: ${e.message}")
            e.printStackTrace()
            // Si no existe la tabla venta aún, devolver 0
            cantidad = 0
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        cantidad
    }

    private fun mapearCliente(resultSet: ResultSet): Cliente {
        return Cliente(
            id = resultSet.getInt("id"),
            nombre = resultSet.getString("nombre") ?: "",
            apellido = resultSet.getString("apellido") ?: "",
            telefono = resultSet.getString("telefono") ?: "",
            email = resultSet.getString("email") ?: "",
            direccion = resultSet.getString("direccion") ?: "",
            dni = resultSet.getString("dni") ?: "",
            fechaRegistro = resultSet.getDate("fecha_registro")?.toString() ?: "",
            activo = resultSet.getBoolean("activo")
        )
    }
}