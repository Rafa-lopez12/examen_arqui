package com.example.tienda_emprendedor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.tienda_emprendedor.ui.theme.Tienda_emprendedorTheme
import com.example.tienda_emprendedor.controller.ProductoController

class MainActivity : ComponentActivity() {

    // Solo necesitamos el controlador
    private lateinit var controller: ProductoController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // El controlador maneja tanto el modelo como la vista
        controller = ProductoController()

        setContent {
            Tienda_emprendedorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Obtenemos la vista del controlador y la renderizamos
                    controller.obtenerVista().Render()
                }
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