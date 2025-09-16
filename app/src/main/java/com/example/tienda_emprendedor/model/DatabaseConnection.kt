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
                    CREATE TABLE IF NOT EXISTS productos (
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
    }
}

/*
📋 INSTRUCCIONES PARA CONFIGURAR LA IP:

1. 🖥️ Encuentra la IP de tu computadora:

   Windows:
   - Abre CMD y ejecuta: ipconfig
   - Busca "Dirección IPv4" (ej: 192.168.1.100)

   Mac/Linux:
   - Abre Terminal y ejecuta: ifconfig
   - Busca inet (ej: 192.168.1.100)

2. 🔧 Cambia la URL arriba:
   - Reemplaza "192.168.1.100" con tu IP real

3. 🔥 Configurar PostgreSQL para aceptar conexiones externas:

   a) Editar postgresql.conf:
      listen_addresses = '*'

   b) Editar pg_hba.conf, agregar línea:
      host all all 0.0.0.0/0 md5

   c) Reiniciar PostgreSQL

4. 📱 Alternativas de IP según donde ejecutes:

   - Dispositivo físico: IP real de tu computadora (192.168.x.x)
   - Emulador Android Studio: 10.0.2.2 (mapea a localhost de la PC)
   - Genymotion: 10.0.3.2
*/