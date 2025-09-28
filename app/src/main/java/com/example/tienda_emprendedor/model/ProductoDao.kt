package com.example.tienda_emprendedor.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.PreparedStatement
import java.sql.ResultSet

class ProductoDao {


    suspend fun obtenerTodosLosProductos(): List<Producto> = withContext(Dispatchers.IO) {
        val productos = mutableListOf<Producto>()
        val connection = DatabaseConnection.obtenerConexion()

        try {
            val query = """
                SELECT p.*, c.nombre as categoria_nombre
                FROM producto p
                LEFT JOIN categoria c ON p.categoria_id = c.id
                ORDER BY p.id
            """.trimIndent()

            val statement = connection?.prepareStatement(query)
            val resultSet = statement?.executeQuery()

            while (resultSet?.next() == true) {
                productos.add(mapearProductoConCategoria(resultSet))
            }

            resultSet?.close()
            statement?.close()
        } catch (e: Exception) {
            println("Error al obtener productos con categoría: ${e.message}")
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
            if (connection == null) {
                return@withContext null
            }

            val query = """
                SELECT p.*, c.nombre as categoria_nombre
                FROM producto p
                LEFT JOIN categoria c ON p.categoria_id = c.id
                WHERE p.id = ?
            """.trimIndent()

            val statement = connection.prepareStatement(query)
            statement.setInt(1, id)
            val resultSet = statement.executeQuery()

            if (resultSet.next()) {
                producto = mapearProductoConCategoria(resultSet)
            }

            resultSet.close()
            statement.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        return@withContext producto
    }

    suspend fun obtenerProductosPorCategoria(categoriaId: Int): List<Producto> = withContext(Dispatchers.IO) {
        val productos = mutableListOf<Producto>()
        val connection = DatabaseConnection.obtenerConexion()

        try {
            val query = """
                SELECT p.*, c.nombre as categoria_nombre
                FROM producto p
                LEFT JOIN categoria c ON p.categoria_id = c.id
                WHERE p.categoria_id = ?
                ORDER BY p.id
            """.trimIndent()

            val statement = connection?.prepareStatement(query)
            statement?.setInt(1, categoriaId)
            val resultSet = statement?.executeQuery()

            while (resultSet?.next() == true) {
                productos.add(mapearProductoConCategoria(resultSet))
            }

            resultSet?.close()
            statement?.close()
        } catch (e: Exception) {
            println("Error al obtener productos por categoría: ${e.message}")
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        productos
    }

    suspend fun obtenerProductosPorSubcategoria(subcategoria: String): List<Producto> = withContext(Dispatchers.IO) {
        val productos = mutableListOf<Producto>()
        val connection = DatabaseConnection.obtenerConexion()

        try {
            val query = """
                SELECT p.*, c.nombre as categoria_nombre
                FROM producto p
                LEFT JOIN categoria c ON p.categoria_id = c.id
                WHERE p.subcategoria = ?
                ORDER BY p.id
            """.trimIndent()

            val statement = connection?.prepareStatement(query)
            statement?.setString(1, subcategoria)
            val resultSet = statement?.executeQuery()

            while (resultSet?.next() == true) {
                productos.add(mapearProductoConCategoria(resultSet))
            }

            resultSet?.close()
            statement?.close()
        } catch (e: Exception) {
            println("Error al obtener productos por subcategoría: ${e.message}")
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
                INSERT INTO producto (nombre, descripcion, precio, categoria_id, subcategoria, stock, imagen) 
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            val statement = connection?.prepareStatement(query)
            statement?.setString(1, producto.nombre)
            statement?.setString(2, producto.descripcion)
            statement?.setDouble(3, producto.precio)
            statement?.setInt(4, producto.categoriaId)
            statement?.setString(5, producto.subcategoria)
            statement?.setInt(6, producto.stock)
            statement?.setString(7, producto.imagen)

            val filasAfectadas = statement?.executeUpdate() ?: 0
            exito = filasAfectadas > 0

            statement?.close()
        } catch (e: Exception) {
            println("Error al insertar producto: ${e.message}")
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
                UPDATE producto 
                SET nombre = ?, descripcion = ?, precio = ?, categoria_id = ?, subcategoria = ?, stock = ?, imagen = ? 
                WHERE id = ?
            """.trimIndent()

            val statement = connection?.prepareStatement(query)
            statement?.setString(1, producto.nombre)
            statement?.setString(2, producto.descripcion)
            statement?.setDouble(3, producto.precio)
            statement?.setInt(4, producto.categoriaId)
            statement?.setString(5, producto.subcategoria)
            statement?.setInt(6, producto.stock)
            statement?.setString(7, producto.imagen)
            statement?.setInt(8, producto.id)

            val filasAfectadas = statement?.executeUpdate() ?: 0
            exito = filasAfectadas > 0

            statement?.close()
        } catch (e: Exception) {
            println("Error al actualizar producto: ${e.message}")
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
            val query = "DELETE FROM producto WHERE id = ?"
            val statement = connection?.prepareStatement(query)
            statement?.setInt(1, id)

            val filasAfectadas = statement?.executeUpdate() ?: 0
            exito = filasAfectadas > 0

            statement?.close()
        } catch (e: Exception) {
            println("Error al eliminar producto: ${e.message}")
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
            categoriaId = resultSet.getInt("categoria_id"),
            subcategoria = resultSet.getString("subcategoria") ?: "",
            stock = resultSet.getInt("stock"),
            imagen = resultSet.getString("imagen") ?: ""
        )
    }

    private fun mapearProductoConCategoria(resultSet: ResultSet): Producto {
        return Producto(
            id = resultSet.getInt("id"),
            nombre = resultSet.getString("nombre") ?: "",
            descripcion = resultSet.getString("descripcion") ?: "",
            precio = resultSet.getDouble("precio"),
            categoriaId = resultSet.getInt("categoria_id"),
            subcategoria = resultSet.getString("subcategoria") ?: "",
            stock = resultSet.getInt("stock"),
            imagen = resultSet.getString("imagen") ?: "",
            nombreCategoria = resultSet.getString("categoria_nombre") ?: ""
        )
    }
}