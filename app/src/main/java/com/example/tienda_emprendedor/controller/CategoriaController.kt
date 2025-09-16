package com.example.tienda_emprendedor.controller

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.tienda_emprendedor.model.Categoria
import com.example.tienda_emprendedor.model.CategoriaDao
import com.example.tienda_emprendedor.view.CategoriaView
import com.example.tienda_emprendedor.utils.SubcategoriaUtils

class CategoriaController {

    private val modelo: CategoriaDao = CategoriaDao()
    private val vista: CategoriaView = CategoriaView()
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        configurarEventosVista()
        cargarCategoriasDesdeModelo()
    }

    private fun configurarEventosVista() {
        vista.onNombreChanged = { nombre ->
            // El nombre se actualiza automáticamente cuando selecciona del dropdown
        }

        vista.onSubcategoriaChanged = { subcategoria ->
            // La subcategoría se actualiza automáticamente cuando selecciona del dropdown
        }

        vista.onDescripcionChanged = { descripcion ->
            // Se maneja automáticamente en la vista
        }

        vista.onAgregarClick = {
            agregarCategoria()
        }

        vista.onEliminarClick = { categoria ->
            eliminarCategoria(categoria)
        }
    }

    private fun agregarCategoria() {
        if (vista.nombre.isNotEmpty() && vista.subcategoria.isNotEmpty()) {
            // Validar que sea una combinación válida
            if (SubcategoriaUtils.esSubcategoriaValida(vista.nombre, vista.subcategoria)) {
                val categoria = Categoria(
                    nombre = vista.nombre,
                    subcategoria = vista.subcategoria,
                    descripcion = vista.descripcion.ifEmpty {
                        "Aires acondicionados ${vista.nombre} ${vista.subcategoria}"
                    }
                )

                scope.launch {
                    val exito = modelo.insertarCategoria(categoria)
                    if (exito) {
                        vista.limpiarFormulario()
                        vista.mostrarFormulario = false
                        cargarCategoriasDesdeModelo()
                        println("✅ Categoría creada: ${categoria.nombre} - ${categoria.subcategoria}")
                    } else {
                        println("❌ Error al crear categoría (posiblemente duplicada)")
                    }
                }
            } else {
                println("❌ Combinación de categoría/subcategoría no válida")
            }
        } else {
            println("❌ Faltan campos obligatorios")
        }
    }

    private fun eliminarCategoria(categoria: Categoria) {
        scope.launch {
            // Primero verificar si hay productos usando esta categoría
            val productosUsandoCategoria = modelo.contarProductosEnCategoria(categoria.id)

            if (productosUsandoCategoria > 0) {
                println("⚠️ No se puede eliminar. Hay $productosUsandoCategoria productos usando esta categoría")
            } else {
                val exito = modelo.eliminarCategoria(categoria.id)
                if (exito) {
                    cargarCategoriasDesdeModelo()
                    println("✅ Categoría eliminada: ${categoria.nombre} - ${categoria.subcategoria}")
                } else {
                    println("❌ Error al eliminar categoría")
                }
            }
        }
    }

    private fun cargarCategoriasDesdeModelo() {
        scope.launch {
            val categorias = modelo.obtenerTodasLasCategorias()
            vista.actualizarCategorias(categorias)
            println("📋 Categorías cargadassss: ${categorias.size}")
        }
    }

    fun obtenerVista(): CategoriaView {
        return vista
    }

    fun obtenerModelo(): CategoriaDao {
        return modelo
    }

    // Método útil para otros controladores
    fun obtenerCategoriasDisponibles(): List<Categoria> {
        return vista.categorias
    }
}