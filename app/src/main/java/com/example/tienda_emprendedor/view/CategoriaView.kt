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
import com.example.tienda_emprendedor.utils.SubcategoriaUtils

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
                text = "Lista de Categorías Registradas",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (categorias.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = "No hay categorías registradas en la base de datos",
                        modifier = Modifier.padding(16.dp),
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn {
                    items(categorias) { categoria ->
                        CategoriaCard(categoria = categoria)
                    }
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

                Text(
                    text = "ℹ️ Escribe libremente o selecciona de las predefinidas",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 🔥 CAMPO DE TEXTO LIBRE para Categoría Principal
                OutlinedTextField(
                    value = nombre,
                    onValueChange = {
                        nombre = it
                        categoriaSeleccionada = it
                        onNombreChanged(it)

                        // Si la categoría existe en el sistema, cargar sus subcategorías
                        if (SubcategoriaUtils.categoriasPrincipales.contains(it)) {
                            subcategoriasDisponibles = SubcategoriaUtils.obtenerSubcategorias(it)
                        } else {
                            subcategoriasDisponibles = emptyList()
                        }
                    },
                    label = { Text("* Categoría Principal") },
                    placeholder = { Text("Ej: Split, Ventana, Mini Split, etc.") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        if (nombre.isNotEmpty() && SubcategoriaUtils.categoriasPrincipales.contains(nombre)) {
                            Text(
                                text = "✅ Categoría predefinida detectada",
                                color = Color(0xFF4CAF50),
                                fontSize = 11.sp
                            )
                        } else if (nombre.isNotEmpty()) {
                            Text(
                                text = "✨ Nueva categoría personalizada",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp
                            )
                        }
                    }
                )

                if (nombre.isEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sugerencias:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SubcategoriaUtils.categoriasPrincipales.take(4).forEach { cat ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    nombre = cat
                                    categoriaSeleccionada = cat
                                    onNombreChanged(cat)
                                    subcategoriasDisponibles = SubcategoriaUtils.obtenerSubcategorias(cat)
                                },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = subcategoria,
                    onValueChange = {
                        subcategoria = it
                        onSubcategoriaChanged(it)
                    },
                    label = { Text("* Subcategoría") },
                    placeholder = { Text("Ej: Residencial, Comercial, 12000 BTU, etc.") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = nombre.isNotEmpty()
                )

                // Botones de sugerencias de subcategorías (si hay predefinidas)
                if (subcategoriasDisponibles.isNotEmpty() && subcategoria.isEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sugerencias para $nombre:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        subcategoriasDisponibles.forEach { sub ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    subcategoria = sub
                                    onSubcategoriaChanged(sub)
                                },
                                label = { Text(sub, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Campo de descripción (opcional)
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = {
                        descripcion = it
                        onDescripcionChanged(it)
                    },
                    label = { Text("Descripción (Opcional)") },
                    placeholder = { Text("Ej: Aires acondicionados ${nombre} ${subcategoria}") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Información de ayuda
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "💡 Ayuda:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "• Puedes escribir cualquier categoría y subcategoría\n" +
                                    "• Usa los botones de sugerencias para las predefinidas\n" +
                                    "• Ejemplos: Mini Split - 18000 BTU, Cassette - 4 Vías, etc.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Vista previa de lo que se creará
                if (nombre.isNotEmpty() && subcategoria.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "📋 Vista previa:",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "$nombre - $subcategoria",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (descripcion.isNotEmpty()) {
                                Text(
                                    text = descripcion,
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Botón para crear
                Button(
                    onClick = {
                        println("🔘 Botón Crear clickeado")
                        println("   Nombre: $nombre")
                        println("   Subcategoría: $subcategoria")
                        println("   Descripción: $descripcion")
                        onAgregarClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = nombre.trim().isNotEmpty() && subcategoria.trim().isNotEmpty()
                ) {
                    Text(
                        text = "🏷️ Crear Categoría",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (nombre.trim().isEmpty() || subcategoria.trim().isEmpty()) {
                    Text(
                        text = "* Ambos campos son obligatorios",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 8.dp)
                    )
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