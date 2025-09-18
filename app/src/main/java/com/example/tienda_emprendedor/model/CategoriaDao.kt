package com.example.tienda_emprendedor.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.PreparedStatement
import java.sql.ResultSet

class CategoriaDao {

    suspend fun obtenerTodasLasCategorias(): List<Categoria> = withContext(Dispatchers.IO) {
        val categorias = mutableListOf<Categoria>()
        val connection = DatabaseConnection.obtenerConexion()

        try {
            val query = "SELECT * FROM categoria WHERE activo = true ORDER BY nombre, subcategoria"
            val statement = connection?.prepareStatement(query)
            val resultSet = statement?.executeQuery()

            while (resultSet?.next() == true) {
                categorias.add(mapearCategoria(resultSet))
            }

            resultSet?.close()
            statement?.close()
        } catch (e: Exception) {
            println("Error al obtener categorías: ${e.message}")
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        categorias
    }

    suspend fun obtenerCategoriaPorId(id: Int): Categoria? = withContext(Dispatchers.IO) {
        var categoria: Categoria? = null
        val connection = DatabaseConnection.obtenerConexion()

        try {
            val query = "SELECT * FROM categoria WHERE id = ? AND activo = true"
            val statement = connection?.prepareStatement(query)
            statement?.setInt(1, id)
            val resultSet = statement?.executeQuery()

            if (resultSet?.next() == true) {
                categoria = mapearCategoria(resultSet)
            }

            resultSet?.close()
            statement?.close()
        } catch (e: Exception) {
            println("Error al obtener categoría por ID: ${e.message}")
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        categoria
    }

    suspend fun insertarCategoria(categoria: Categoria): Boolean = withContext(Dispatchers.IO) {
        val connection = DatabaseConnection.obtenerConexion()
        var exito = false

        try {
            val query = """
                INSERT INTO categoria (nombre, subcategoria, descripcion, activo) 
                VALUES (?, ?, ?, ?) 
                ON CONFLICT (nombre, subcategoria) DO NOTHING
            """.trimIndent()

            val statement = connection?.prepareStatement(query)
            statement?.setString(1, categoria.nombre)
            statement?.setString(2, categoria.subcategoria)
            statement?.setString(3, categoria.descripcion)
            statement?.setBoolean(4, categoria.activo)

            val filasAfectadas = statement?.executeUpdate() ?: 0
            exito = filasAfectadas > 0

            statement?.close()
        } catch (e: Exception) {
            println("Error al insertar categoría: ${e.message}")
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        exito
    }

    suspend fun actualizarCategoria(categoria: Categoria): Boolean = withContext(Dispatchers.IO) {
        val connection = DatabaseConnection.obtenerConexion()
        var exito = false

        try {
            val query = """
                UPDATE categoria 
                SET nombre = ?, subcategoria = ?, descripcion = ?, activo = ? 
                WHERE id = ?
            """.trimIndent()

            val statement = connection?.prepareStatement(query)
            statement?.setString(1, categoria.nombre)
            statement?.setString(2, categoria.subcategoria)
            statement?.setString(3, categoria.descripcion)
            statement?.setBoolean(4, categoria.activo)
            statement?.setInt(5, categoria.id)

            val filasAfectadas = statement?.executeUpdate() ?: 0
            exito = filasAfectadas > 0

            statement?.close()
        } catch (e: Exception) {
            println("Error al actualizar categoría: ${e.message}")
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        exito
    }

    suspend fun eliminarCategoria(id: Int): Boolean = withContext(Dispatchers.IO) {
        val connection = DatabaseConnection.obtenerConexion()
        var exito = false

        try {
            // Soft delete - marcamos como inactiva
            val query = "UPDATE categoria SET activo = false WHERE id = ?"
            val statement = connection?.prepareStatement(query)
            statement?.setInt(1, id)

            val filasAfectadas = statement?.executeUpdate() ?: 0
            exito = filasAfectadas > 0

            statement?.close()
        } catch (e: Exception) {
            println("Error al eliminar categoría: ${e.message}")
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        exito
    }

    suspend fun contarProductosEnCategoria(categoriaId: Int): Int = withContext(Dispatchers.IO) {
        var cantidad = 0
        val connection = DatabaseConnection.obtenerConexion()

        try {
            val query = "SELECT COUNT(*) as total FROM producto WHERE categoria_id = ?"
            val statement = connection?.prepareStatement(query)
            statement?.setInt(1, categoriaId)
            val resultSet = statement?.executeQuery()

            if (resultSet?.next() == true) {
                cantidad = resultSet.getInt("total")
            }

            resultSet?.close()
            statement?.close()
        } catch (e: Exception) {
            println("Error al contar productos en categoría: ${e.message}")
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        cantidad
    }

    suspend fun obtenerCategoriasPorNombre(nombre: String): List<Categoria> = withContext(Dispatchers.IO) {
        val categorias = mutableListOf<Categoria>()
        val connection = DatabaseConnection.obtenerConexion()

        try {
            val query = """
                SELECT * FROM categoria
                WHERE nombre = ? AND activo = true 
                ORDER BY subcategoria
            """.trimIndent()

            val statement = connection?.prepareStatement(query)
            statement?.setString(1, nombre)
            val resultSet = statement?.executeQuery()

            while (resultSet?.next() == true) {
                categorias.add(mapearCategoria(resultSet))
            }

            resultSet?.close()
            statement?.close()
        } catch (e: Exception) {
            println("Error al obtener categorías por nombre: ${e.message}")
            e.printStackTrace()
        } finally {
            DatabaseConnection.cerrarConexion(connection)
        }

        categorias
    }

    private fun mapearCategoria(resultSet: ResultSet): Categoria {
        return Categoria(
            id = resultSet.getInt("id"),
            nombre = resultSet.getString("nombre") ?: "",
            subcategoria = resultSet.getString("subcategoria") ?: "",
            descripcion = resultSet.getString("descripcion") ?: "",
            activo = resultSet.getBoolean("activo")
        )
    }
}