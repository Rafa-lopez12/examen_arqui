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
import com.example.tienda_emprendedor.model.Categoria

class ProductoView {

    var nombre by mutableStateOf("")
    var descripcion by mutableStateOf("")
    var precio by mutableStateOf("")
    var categoriaSeleccionada by mutableStateOf<Categoria?>(null)
    var subcategoriaSeleccionada by mutableStateOf("")
    var stock by mutableStateOf("")
    var productos by mutableStateOf(listOf<Producto>())
    var categorias by mutableStateOf(listOf<Categoria>())
    var subcategoriasDisponibles by mutableStateOf(listOf<String>())

    // Estados del UI
    var mostrarDropdownCategoria by mutableStateOf(false)
    var mostrarDropdownSubcategoria by mutableStateOf(false)
    var mostrarDetalleProducto by mutableStateOf(false)
    var productoSeleccionado by mutableStateOf<Producto?>(null)

    // Callbacks
    var onNombreChanged: (String) -> Unit = {}
    var onDescripcionChanged: (String) -> Unit = {}
    var onPrecioChanged: (String) -> Unit = {}
    var onCategoriaChanged: (Categoria) -> Unit = {}
    var onSubcategoriaChanged: (String) -> Unit = {}
    var onStockChanged: (String) -> Unit = {}
    var onAgregarClick: () -> Unit = {}
    var onEliminarClick: (Producto) -> Unit = {}
    var onProductoClick: (Producto) -> Unit = {}

    fun actualizarProductos(nuevosProductos: List<Producto>) {
        productos = nuevosProductos
    }

    fun actualizarCategorias(nuevasCategorias: List<Categoria>) {
        categorias = nuevasCategorias
    }

    fun actualizarSubcategorias(nuevasSubcategorias: List<String>) {
        subcategoriasDisponibles = nuevasSubcategorias
    }

    fun limpiarFormulario() {
        nombre = ""
        descripcion = ""
        precio = ""
        categoriaSeleccionada = null
        subcategoriaSeleccionada = ""
        stock = ""
        subcategoriasDisponibles = emptyList()
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
                text = "🌬️ Gestión de Aires Acondicionados",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            FormularioProducto()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Lista de Productos",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn {
                items(productos) { producto ->
                    ProductoCard(producto = producto)
                }
            }
        }

        // Dialog para mostrar detalle del producto
        if (mostrarDetalleProducto) {
            ProductoDetailDialog(
                producto = productoSeleccionado,
                onDismiss = {
                    mostrarDetalleProducto = false
                    productoSeleccionado = null
                }
            )
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
                    text = "Agregar Aire Acondicionado",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Campo Nombre
                OutlinedTextField(
                    value = nombre,
                    onValueChange = {
                        nombre = it
                        onNombreChanged(it)
                    },
                    label = { Text("Nombre/Modelo") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Campo Descripción
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

                // Row con Precio y Stock
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

                // Dropdown Categoría
                ExposedDropdownMenuBox(
                    expanded = mostrarDropdownCategoria,
                    onExpandedChange = { mostrarDropdownCategoria = !mostrarDropdownCategoria }
                ) {
                    OutlinedTextField(
                        value = categoriaSeleccionada?.let { "${it.nombre} - ${it.subcategoria}" } ?: "",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mostrarDropdownCategoria) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = mostrarDropdownCategoria,
                        onDismissRequest = { mostrarDropdownCategoria = false }
                    ) {
                        categorias.forEach { categoria ->
                            DropdownMenuItem(
                                text = { Text("${categoria.nombre} - ${categoria.subcategoria}") },
                                onClick = {
                                    categoriaSeleccionada = categoria
                                    subcategoriaSeleccionada = categoria.subcategoria
                                    onCategoriaChanged(categoria)
                                    mostrarDropdownCategoria = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onAgregarClick,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    enabled = nombre.isNotEmpty() && precio.isNotEmpty() && categoriaSeleccionada != null
                ) {
                    Text("🌬️ Agregar Aire Acondicionado")
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = producto.nombre,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = producto.descripcion,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row {
                            Text(
                                text = "$${producto.precio}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "📦 ${producto.stock}",
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = "🏷️ ${producto.nombreCategoria} - ${producto.subcategoria}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Column {
                        TextButton(onClick = { onProductoClick(producto) }) {
                            Text("👁️ Ver")
                        }
                        TextButton(onClick = { onEliminarClick(producto) }) {
                            Text("🗑️ Eliminar", color = Color.Red)
                        }
                    }
                }
            }
        }
    }
}