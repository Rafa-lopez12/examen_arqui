package com.example.tienda_emprendedor.model

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class DatabaseConnection {

    companion object {
        // ⚠️ IMPORTANTE: En Android, NO puedes usar "localhost"
        // Opciones para la URL:

        // Opción 1: IP de tu computadora en la red local (recomendada para testing)
        //private const val URL = "jdbc:postgresql://192.168.1.100:5432/Tienda_Emprendor"

        // Opción 2: Si usas emulador de Android Studio
        private const val URL = "jdbc:postgresql://10.0.2.2:5432/Tienda_Emprendor"

        // Opción 3: Servidor en la nube
        // private const val URL = "jdbc:postgresql://tu-servidor.com:5432/Tienda_Emprendor"

        private const val USER = "postgres"
        private const val PASSWORD = "leyendas13"

        fun obtenerConexion(): Connection? {
            return try {
                Class.forName("org.postgresql.Driver")

                val connection = DriverManager.getConnection(URL, USER, PASSWORD)
                println("Conexión exitosa a PostgreSQL desde Android")
                connection

            } catch (e: SQLException) {
                println("Error SQL: ${e.message}")
                e.printStackTrace()
                null
            } catch (e: ClassNotFoundException) {
                println("Driver PostgreSQL no encontrado: ${e.message}")
                e.printStackTrace()
                null
            } catch (e: Exception) {
                println("Error general de conexión: ${e.message}")
                e.printStackTrace()
                null
            }
        }

        fun cerrarConexion(connection: Connection?) {
            try {
                connection?.close()
                println("Conexión PostgreSQL cerrada")
            } catch (e: SQLException) {
                println("Error al cerrar conexión: ${e.message}")
                e.printStackTrace()
            }
        }

        suspend fun probarConexion(): Boolean {
            return try {
                val connection = obtenerConexion()
                val esExitosa = connection != null
                cerrarConexion(connection)

                if (esExitosa) {
                    println("Test de conexión exitoso")
                } else {
                    println("Test de conexión falló")
                }

                esExitosa
            } catch (e: Exception) {
                println("Error en test de conexión: ${e.message}")
                false
            }
        }

        suspend fun crearTablaProductos(): Boolean {
            val connection = obtenerConexion()
            return try {
                val sql = """
                    CREATE TABLE IF NOT EXISTS producto (
                        id SERIAL PRIMARY KEY,
                        nombre VARCHAR(255) NOT NULL,
                        descripcion TEXT,
                        precio DECIMAL(10,2) NOT NULL,
                        categoria VARCHAR(100),
                        stock INTEGER DEFAULT 0,
                        imagen VARCHAR(500)
                    )
                """.trimIndent()

                val statement = connection?.createStatement()
                statement?.executeUpdate(sql)
                statement?.close()

                println("📦 Tabla 'productos' verificada/creada exitosamente")
                true

            } catch (e: Exception) {
                println("Error al crear tabla: ${e.message}")
                e.printStackTrace()
                false
            } finally {
                cerrarConexion(connection)
            }
        }

        suspend fun crearTablaCategorias(): Boolean {
            val connection = obtenerConexion()
            return try {
                val sql = """
            CREATE TABLE IF NOT EXISTS categoria (
                id SERIAL PRIMARY KEY,
                nombre VARCHAR(100) NOT NULL,
                subcategoria VARCHAR(100) NOT NULL,
                descripcion TEXT,
                activo BOOLEAN DEFAULT true,
                UNIQUE(nombre, subcategoria)
            )
        """.trimIndent()

                val statement = connection?.createStatement()
                statement?.executeUpdate(sql)
                statement?.close()

                println("🏷️ Tabla 'categorias' verificada/creada exitosamente")
                true

            } catch (e: Exception) {
                println("Error al crear tabla categorias: ${e.message}")
                e.printStackTrace()
                false
            } finally {
                cerrarConexion(connection)
            }
        }

        suspend fun crearTablaProductosActualizada(): Boolean {
            val connection = obtenerConexion()
            return try {
                val sql = """
            CREATE TABLE IF NOT EXISTS producto (
                id SERIAL PRIMARY KEY,
                nombre VARCHAR(255) NOT NULL,
                descripcion TEXT,
                precio DECIMAL(10,2) NOT NULL,
                categoria_id INTEGER REFERENCES categoria(id),
                subcategoria VARCHAR(100),
                stock INTEGER DEFAULT 0,
                imagen VARCHAR(500)
            )
        """.trimIndent()

                val statement = connection?.createStatement()
                statement?.executeUpdate(sql)
                statement?.close()

                println("📦 Tabla 'productos' actualizada exitosamente")
                true

            } catch (e: Exception) {
                println("Error al crear tabla productos: ${e.message}")
                e.printStackTrace()
                false
            } finally {
                cerrarConexion(connection)
            }
        }

        suspend fun crearTablaClientes(): Boolean {
            val connection = obtenerConexion()
            return try {
                val sql = """
            CREATE TABLE IF NOT EXISTS cliente (
                id SERIAL PRIMARY KEY,
                nombre VARCHAR(100) NOT NULL,
                apellido VARCHAR(100) NOT NULL,
                telefono VARCHAR(20),
                email VARCHAR(150),
                direccion TEXT,
                dni VARCHAR(20) NOT NULL UNIQUE,
                fecha_registro DATE DEFAULT CURRENT_DATE,
                activo BOOLEAN DEFAULT true
            )
        """.trimIndent()

                val statement = connection?.createStatement()
                statement?.executeUpdate(sql)
                statement?.close()

                println("👥 Tabla 'cliente' verificada/creada exitosamente")
                true

            } catch (e: Exception) {
                println("Error al crear tabla cliente: ${e.message}")
                e.printStackTrace()
                false
            } finally {
                cerrarConexion(connection)
            }
        }

        suspend fun crearTablaVentas(): Boolean {
            val connection = obtenerConexion()
            return try {
                val sql = """
            CREATE TABLE IF NOT EXISTS venta (
                id SERIAL PRIMARY KEY,
                cliente_id INTEGER REFERENCES cliente(id),
                fecha_venta TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                total DECIMAL(10,2) NOT NULL,
                descuento DECIMAL(5,2) DEFAULT 0,
                impuestos DECIMAL(10,2) DEFAULT 0,
                metodo_pago VARCHAR(50),
                estado VARCHAR(20) DEFAULT 'completada',
                notas TEXT
            )
        """.trimIndent()

                val statement = connection?.createStatement()
                statement?.executeUpdate(sql)
                statement?.close()

                println("🛒 Tabla 'venta' verificada/creada exitosamente")
                true

            } catch (e: Exception) {
                println("Error al crear tabla venta: ${e.message}")
                e.printStackTrace()
                false
            } finally {
                cerrarConexion(connection)
            }
        }

        suspend fun crearTablaDetalleVenta(): Boolean {
            val connection = obtenerConexion()
            return try {
                val sql = """
        CREATE TABLE IF NOT EXISTS detalle_venta (
            venta_id INTEGER REFERENCES venta(id) ON DELETE CASCADE,
            producto_id INTEGER REFERENCES producto(id),
            cantidad INTEGER NOT NULL,
            precio_unitario DECIMAL(10,2) NOT NULL,
            subtotal DECIMAL(10,2) NOT NULL,
            PRIMARY KEY (venta_id, producto_id)  -- ✅ Clave primaria compuesta
        )
        """.trimIndent()

                val statement = connection?.createStatement()
                statement?.executeUpdate(sql)
                statement?.close()

                println("📋 Tabla 'detalle_venta' verificada/creada exitosamente")
                true

            } catch (e: Exception) {
                println("Error al crear tabla detalle_venta: ${e.message}")
                e.printStackTrace()
                false
            } finally {
                cerrarConexion(connection)
            }
        }

        suspend fun crearTablaPagos(): Boolean {
            val connection = obtenerConexion()
            return try {
                val sql = """
            CREATE TABLE IF NOT EXISTS pago (
                id SERIAL PRIMARY KEY,
                venta_id INTEGER REFERENCES venta(id) ON DELETE CASCADE,
                monto DECIMAL(10,2) NOT NULL,
                metodo_pago VARCHAR(50) NOT NULL,
                fecha_pago TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                referencia VARCHAR(100),
                estado VARCHAR(20) DEFAULT 'completado'
            )
        """.trimIndent()

                val statement = connection?.createStatement()
                statement?.executeUpdate(sql)
                statement?.close()

                println("💳 Tabla 'pago' verificada/creada exitosamente")
                true

            } catch (e: Exception) {
                println("Error al crear tabla pago: ${e.message}")
                e.printStackTrace()
                false
            } finally {
                cerrarConexion(connection)
            }
        }

        suspend fun inicializarTodasLasTablas(): Boolean {
            return try {
                val resultadoCategorias = crearTablaCategorias()
                val resultadoProductos = crearTablaProductosActualizada()
                val resultadoClientes = crearTablaClientes()
                val resultadoVentas = crearTablaVentas()
                val resultadoDetalleVenta = crearTablaDetalleVenta()
                val resultadoPagos = crearTablaPagos()

                if (resultadoCategorias) {
                    insertarCategoriasIniciales()
                }

                if (resultadoClientes) {
                    insertarClientesIniciales()
                }

                resultadoCategorias && resultadoProductos && resultadoClientes &&
                        resultadoVentas && resultadoDetalleVenta && resultadoPagos
            } catch (e: Exception) {
                println("Error al inicializar tablas: ${e.message}")
                false
            }
        }

        suspend fun insertarCategoriasIniciales(): Boolean {
            val connection = obtenerConexion()
            return try {
                val categorias = listOf(
                    "Split" to listOf("Residencial", "Comercial", "Industrial"),
                    "Ventana" to listOf("Compacto", "Estándar"),
                    "Central" to listOf("Ducto", "Cassette", "Piso-Techo"),
                    "Portátil" to listOf("Doméstico", "Oficina")
                )

                val sql = """
            INSERT INTO categoria (nombre, subcategoria, descripcion) 
            VALUES (?, ?, ?) 
            ON CONFLICT (nombre, subcategoria) DO NOTHING
        """.trimIndent()

                val statement = connection?.prepareStatement(sql)

                categorias.forEach { (categoria, subcategorias) ->
                    subcategorias.forEach { subcategoria ->
                        statement?.setString(1, categoria)
                        statement?.setString(2, subcategoria)
                        statement?.setString(3, "Aires acondicionados $categoria $subcategoria")
                        statement?.addBatch()
                    }
                }

                statement?.executeBatch()
                statement?.close()

                println("🎯 Categorías iniciales insertadas exitosamente")
                true

            } catch (e: Exception) {
                println("Error al insertar categorías iniciales: ${e.message}")
                e.printStackTrace()
                false
            } finally {
                cerrarConexion(connection)
            }
        }

        suspend fun insertarClientesIniciales(): Boolean {
            val connection = obtenerConexion()
            return try {
                // Verificar si ya existen clientes
                val checkSql = "SELECT COUNT(*) as total FROM cliente"
                val checkStatement = connection?.prepareStatement(checkSql)
                val resultSet = checkStatement?.executeQuery()

                var clientesExistentes = 0
                if (resultSet?.next() == true) {
                    clientesExistentes = resultSet.getInt("total")
                }

                resultSet?.close()
                checkStatement?.close()

                // Si ya hay clientes, no insertar datos de prueba
                if (clientesExistentes > 0) {
                    println("👥 Ya existen clientes en la base de datos")
                    return true
                }

                val clientesPrueba = listOf(
                    arrayOf("Juan", "Pérez", "76543210", "juan.perez@email.com", "Av. Principal 123", "12345678"),
                    arrayOf("María", "González", "78912345", "maria.gonzalez@email.com", "Calle 2do Anillo 456", "87654321"),
                    arrayOf("Carlos", "López", "79876543", "carlos.lopez@email.com", "Zona Norte 789", "11223344"),
                    arrayOf("Ana", "Martínez", "77234567", "", "Equipetrol 321", "44332211"),
                    arrayOf("Roberto", "Silva", "75678901", "roberto.silva@email.com", "", "55667788")
                )

                val sql = """
            INSERT INTO cliente (nombre, apellido, telefono, email, direccion, dni, fecha_registro, activo) 
            VALUES (?, ?, ?, ?, ?, ?, CURRENT_DATE, true)
            ON CONFLICT (dni) DO NOTHING
        """.trimIndent()

                val statement = connection?.prepareStatement(sql)

                clientesPrueba.forEach { cliente ->
                    statement?.setString(1, cliente[0]) // nombre
                    statement?.setString(2, cliente[1]) // apellido
                    statement?.setString(3, cliente[2]) // telefono
                    statement?.setString(4, cliente[3]) // email
                    statement?.setString(5, cliente[4]) // direccion
                    statement?.setString(6, cliente[5]) // dni
                    statement?.addBatch()
                }

                statement?.executeBatch()
                statement?.close()

                println("👥 Clientes iniciales insertados exitosamente")
                true

            } catch (e: Exception) {
                println("Error al insertar clientes iniciales: ${e.message}")
                e.printStackTrace()
                false
            } finally {
                cerrarConexion(connection)
            }
        }
    }
}

