package com.example.tienda_emprendedor.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tienda_emprendedor.model.Producto

class ProductoView {

    // Estado interno de la vista
    var nombre by mutableStateOf("")
    var descripcion by mutableStateOf("")
    var precio by mutableStateOf("")
    var categoria by mutableStateOf("")
    var stock by mutableStateOf("")
    var productos by mutableStateOf(listOf<Producto>())

    // Callbacks que el controlador configurará
    var onNombreChanged: (String) -> Unit = {}
    var onDescripcionChanged: (String) -> Unit = {}
    var onPrecioChanged: (String) -> Unit = {}
    var onCategoriaChanged: (String) -> Unit = {}
    var onStockChanged: (String) -> Unit = {}
    var onAgregarClick: () -> Unit = {}
    var onEliminarClick: (Producto) -> Unit = {}

    fun actualizarProductos(nuevosProductos: List<Producto>) {
        productos = nuevosProductos
    }

    fun limpiarFormulario() {
        nombre = ""
        descripcion = ""
        precio = ""
        categoria = ""
        stock = ""
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Render() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "🏪 Tienda Emprendedor",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Formulario para agregar productos
            FormularioProducto()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Lista de Productos",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Lista de productos
            LazyColumn {
                items(productos) { producto ->
                    ProductoCard(producto = producto)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun FormularioProducto() {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Agregar Producto",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = nombre,
                    onValueChange = {
                        nombre = it
                        onNombreChanged(it)
                    },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = {
                        descripcion = it
                        onDescripcionChanged(it)
                    },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = precio,
                        onValueChange = {
                            precio = it
                            onPrecioChanged(it)
                        },
                        label = { Text("Precio") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = stock,
                        onValueChange = {
                            stock = it
                            onStockChanged(it)
                        },
                        label = { Text("Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = categoria,
                    onValueChange = {
                        categoria = it
                        onCategoriaChanged(it)
                    },
                    label = { Text("Categoría") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onAgregarClick,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("➕ Agregar Producto")
                }
            }
        }
    }

    @Composable
    private fun ProductoCard(producto: Producto) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = producto.nombre,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$${producto.precio}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = producto.descripcion,
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "📦 Stock: ${producto.stock}",
                        fontSize = 12.sp
                    )
                    Text(
                        text = "🏷️ ${producto.categoria}",
                        fontSize = 12.sp
                    )

                    TextButton(onClick = { onEliminarClick(producto) }) {
                        Text("🗑️ Eliminar", color = Color.Red)
                    }
                }
            }
        }
    }
}