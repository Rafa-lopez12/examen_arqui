package com.example.tienda_emprendedor.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.PreparedStatement
import java.sql.ResultSet

class ProductoDao {

    // El MODELO es responsable de acceder a la base de datos

    suspend fun obtenerTodosLosProductos(): List<Producto> = withContext(Dispatchers.IO) {
        val productos = mutableListOf<Producto>()
        val connection = DatabaseConnection.obtenerConexion()

        try {
            val query = "SELECT * FROM productos ORDER BY id"
            val statement = connection?.prepareStatement(query)
            val resultSet = statement?.executeQuery()

            while (resultSet?.next() == true) {
                productos.add(mapearProducto(resultSet))
            }

            resultSet?.close()
            statement?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        productos
    }

    suspend fun obtenerProductoPorId(id: Int): Producto? = withContext(Dispatchers.IO) {
        var producto: Producto? = null
        val connection = DatabaseConnection.obtenerConexion()

        try {
            val query = "SELECT * FROM productos WHERE id = ?"
            val statement = connection?.prepareStatement(query)
            statement?.setInt(1, id)
            val resultSet = statement?.executeQuery()

            if (resultSet?.next() == true) {
                producto = mapearProducto(resultSet)
            }

            resultSet?.close()
            statement?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        producto
    }

    suspend fun obtenerProductosPorCategoria(categoria: String): List<Producto> = withContext(Dispatchers.IO) {
        val productos = mutableListOf<Producto>()
        val connection = DatabaseConnection.obtenerConexion()

        try {
            val query = "SELECT * FROM productos WHERE categoria = ? ORDER BY id"
            val statement = connection?.prepareStatement(query)
            statement?.setString(1, categoria)
            val resultSet = statement?.executeQuery()

            while (resultSet?.next() == true) {
                productos.add(mapearProducto(resultSet))
            }

            resultSet?.close()
            statement?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        productos
    }

    suspend fun insertarProducto(producto: Producto): Boolean = withContext(Dispatchers.IO) {
        val connection = DatabaseConnection.obtenerConexion()
        var exito = false

        try {
            val query = """
                INSERT INTO productos (nombre, descripcion, precio, categoria, stock, imagen) 
                VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent()

            val statement = connection?.prepareStatement(query)
            statement?.setString(1, producto.nombre)
            statement?.setString(2, producto.descripcion)
            statement?.setDouble(3, producto.precio)
            statement?.setString(4, producto.categoria)
            statement?.setInt(5, producto.stock)
            statement?.setString(6, producto.imagen)

            val filasAfectadas = statement?.executeUpdate() ?: 0
            exito = filasAfectadas > 0

            statement?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        exito
    }

    suspend fun actualizarProducto(producto: Producto): Boolean = withContext(Dispatchers.IO) {
        val connection = DatabaseConnection.obtenerConexion()
        var exito = false

        try {
            val query = """
                UPDATE productos 
                SET nombre = ?, descripcion = ?, precio = ?, categoria = ?, stock = ?, imagen = ? 
                WHERE id = ?
            """.trimIndent()

            val statement = connection?.prepareStatement(query)
            statement?.setString(1, producto.nombre)
            statement?.setString(2, producto.descripcion)
            statement?.setDouble(3, producto.precio)
            statement?.setString(4, producto.categoria)
            statement?.setInt(5, producto.stock)
            statement?.setString(6, producto.imagen)
            statement?.setInt(7, producto.id)

            val filasAfectadas = statement?.executeUpdate() ?: 0
            exito = filasAfectadas > 0

            statement?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        exito
    }

    suspend fun eliminarProducto(id: Int): Boolean = withContext(Dispatchers.IO) {
        val connection = DatabaseConnection.obtenerConexion()
        var exito = false

        try {
            val query = "DELETE FROM productos WHERE id = ?"
            val statement = connection?.prepareStatement(query)
            statement?.setInt(1, id)

            val filasAfectadas = statement?.executeUpdate() ?: 0
            exito = filasAfectadas > 0

            statement?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        exito
    }

    private fun mapearProducto(resultSet: ResultSet): Producto {
        return Producto(
            id = resultSet.getInt("id"),
            nombre = resultSet.getString("nombre") ?: "",
            descripcion = resultSet.getString("descripcion") ?: "",
            precio = resultSet.getDouble("precio"),
            categoria = resultSet.getString("categoria") ?: "",
            stock = resultSet.getInt("stock"),
            imagen = resultSet.getString("imagen") ?: ""
        )
    }
}