package com.example.tienda_emprendedor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tienda_emprendedor.ui.theme.Tienda_emprendedorTheme
import com.example.tienda_emprendedor.controller.ProductoController
import com.example.tienda_emprendedor.controller.CategoriaController
import com.example.tienda_emprendedor.controller.ClienteController
import com.example.tienda_emprendedor.controller.VentaController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.tienda_emprendedor.model.DatabaseConnection

class MainActivity : ComponentActivity() {

    // Controladores
    private lateinit var productoController: ProductoController
    private lateinit var categoriaController: CategoriaController
    private lateinit var clienteController: ClienteController
    private lateinit var ventaController: VentaController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        inicializarBaseDatos()

        productoController = ProductoController()
        categoriaController = CategoriaController()
        clienteController = ClienteController()
        ventaController = VentaController()

        setContent {
            Tienda_emprendedorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppContent()
                }
            }
        }
    }

    private fun inicializarBaseDatos() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val conexionExitosa = DatabaseConnection.probarConexion()
                if (conexionExitosa) {
                    DatabaseConnection.inicializarTodasLasTablas()
                }
            } catch (e: Exception) {
                println("Error al inicializar BD: ${e.message}")
            }
        }
    }

    @Composable
    fun AppContent() {
        var pantallaActual by remember { mutableStateOf("productos") }

        Column(modifier = Modifier.fillMaxSize()) {
            // Header con navegación
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🌬️ Aires Acondicionados",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { pantallaActual = "productos" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (pantallaActual == "productos")
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("🌬️ Productos", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { pantallaActual = "categorias" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (pantallaActual == "categorias")
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("🏷️ Categorías", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { pantallaActual = "clientes" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (pantallaActual == "clientes")
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("👥 Clientes", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { pantallaActual = "ventas" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (pantallaActual == "ventas")
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("🛒 Ventas", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Contenido según pantalla seleccionada
            when (pantallaActual) {
                "productos" -> productoController.obtenerVista().Render()
                "categorias" -> categoriaController.obtenerVista().Render()
                "clientes" -> clienteController.obtenerVista().Render()
                "ventas" -> ventaController.obtenerVista().Render()
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Tienda_emprendedorTheme {
        Greeting("Android")
    }
}