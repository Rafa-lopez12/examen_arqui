// app/src/main/java/com/example/tienda_emprendedor/view/CategoriaView.kt
package com.example.tienda_emprendedor.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tienda_emprendedor.model.Categoria

class CategoriaView {

    var nombre by mutableStateOf("")
    var subcategoria by mutableStateOf("")
    var descripcion by mutableStateOf("")
    var categorias by mutableStateOf(listOf<Categoria>())
    var mostrarFormulario by mutableStateOf(false)

    // Estados para dropdowns
    var categoriaSeleccionada by mutableStateOf("")
    var mostrarDropdownCategoria by mutableStateOf(false)
    var mostrarDropdownSubcategoria by mutableStateOf(false)
    var subcategoriasDisponibles by mutableStateOf(listOf<String>())

    // Callbacks
    var onNombreChanged: (String) -> Unit = {}
    var onSubcategoriaChanged: (String) -> Unit = {}
    var onDescripcionChanged: (String) -> Unit = {}
    var onAgregarClick: () -> Unit = {}
    var onEliminarClick: (Categoria) -> Unit = {}

    fun actualizarCategorias(nuevasCategorias: List<Categoria>) {
        categorias = nuevasCategorias
    }

    fun limpiarFormulario() {
        nombre = ""
        subcategoria = ""
        descripcion = ""
        categoriaSeleccionada = ""
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
                text = "🏷️ Gestión de Categorías",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Botón para mostrar/ocultar formulario
            Button(
                onClick = { mostrarFormulario = !mostrarFormulario },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (mostrarFormulario) "❌ Cancelar" else "➕ Nueva Categoría")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (mostrarFormulario) {
                FormularioCategoria()
                Spacer(modifier = Modifier.height(16.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "💡 Categorías Disponibles en BD",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (categorias.isEmpty()) {
                        Text(
                            text = "No hay categorías registradas",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    } else {
                        // Agrupar por nombre principal
                        val agrupadas = categorias.groupBy { it.nombre }
                        agrupadas.forEach { (nombre, lista) ->
                            val subcats = lista.map { it.subcategoria }.joinToString(", ")
                            Text(
                                text = "• $nombre: $subcats",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Lista de Categorías Disponibles",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn {
                items(categorias) { categoria ->
                    CategoriaCard(categoria = categoria)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun FormularioCategoria() {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Crear Nueva Categoría",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Dropdown para Categoría Principal
                ExposedDropdownMenuBox(
                    expanded = mostrarDropdownCategoria,
                    onExpandedChange = { mostrarDropdownCategoria = !mostrarDropdownCategoria }
                ) {
                    OutlinedTextField(
                        value = categoriaSeleccionada,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Categoría Principal") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mostrarDropdownCategoria) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = mostrarDropdownCategoria,
                        onDismissRequest = { mostrarDropdownCategoria = false }
                    ) {
                        // USAR DATOS REALES DE LA BD - no SubcategoriaUtils
                        val categoriasUnicas = categorias.map { it.nombre }.distinct()
                        categoriasUnicas.forEach { categoria ->
                            DropdownMenuItem(
                                text = { Text(categoria) },
                                onClick = {
                                    categoriaSeleccionada = categoria
                                    nombre = categoria
                                    onNombreChanged(categoria)
                                    // Obtener subcategorías reales de esa categoría desde BD
                                    subcategoriasDisponibles = categorias
                                        .filter { it.nombre == categoria }
                                        .map { it.subcategoria }
                                        .distinct()
                                    subcategoria = ""
                                    mostrarDropdownCategoria = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dropdown para Subcategoría
                if (subcategoriasDisponibles.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = mostrarDropdownSubcategoria,
                        onExpandedChange = { mostrarDropdownSubcategoria = !mostrarDropdownSubcategoria }
                    ) {
                        OutlinedTextField(
                            value = subcategoria,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Subcategoría") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mostrarDropdownSubcategoria) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = mostrarDropdownSubcategoria,
                            onDismissRequest = { mostrarDropdownSubcategoria = false }
                        ) {
                            subcategoriasDisponibles.forEach { sub ->
                                DropdownMenuItem(
                                    text = { Text(sub) },
                                    onClick = {
                                        subcategoria = sub
                                        onSubcategoriaChanged(sub)
                                        mostrarDropdownSubcategoria = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = {
                        descripcion = it
                        onDescripcionChanged(it)
                    },
                    label = { Text("Descripción (Opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onAgregarClick,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    enabled = nombre.isNotEmpty() && subcategoria.isNotEmpty()
                ) {
                    Text("🏷️ Crear Categoría")
                }
            }
        }
    }

    @Composable
    private fun CategoriaCard(categoria: Categoria) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = categoria.nombre,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Subcategoría: ${categoria.subcategoria}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (categoria.descripcion.isNotEmpty()) {
                            Text(
                                text = categoria.descripcion,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    TextButton(onClick = { onEliminarClick(categoria) }) {
                        Text("🗑️ Eliminar", color = Color.Red)
                    }
                }
            }
        }
    }
}