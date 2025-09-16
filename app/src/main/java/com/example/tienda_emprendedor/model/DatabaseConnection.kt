package com.example.tienda_emprendedor.model

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class DatabaseConnection {

    companion object {
        private const val URL = "jdbc:postgresql://localhost:5432/Tienda_Emprendor"
        private const val USER = "postgres"
        private const val PASSWORD = "leyendas13"


        fun obtenerConexion(): Connection? {
            return try {
                Class.forName("org.postgresql.Driver")
                DriverManager.getConnection(URL, USER, PASSWORD)
            } catch (e: SQLException) {
                e.printStackTrace()
                null
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
                null
            }
        }

        fun cerrarConexion(connection: Connection?) {
            try {
                connection?.close()
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
    }
}