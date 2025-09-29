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

        }

        vista.onSubcategoriaChanged = { subcategoria ->

        }

        vista.onDescripcionChanged = { descripcion ->

        }

        vista.onAgregarClick = {
            agregarCategoria()
        }

        vista.onEliminarClick = { categoria ->
            eliminarCategoria(categoria)
        }
    }

    private fun agregarCategoria() {
        if (vista.nombre.trim().isEmpty() || vista.subcategoria.trim().isEmpty()) {
            return
        }

        val nombreLimpio = vista.nombre.trim()
        val subcategoriaLimpia = vista.subcategoria.trim()

        if (nombreLimpio.length < 2) {
            return
        }

        if (subcategoriaLimpia.length < 2) {
            return
        }
        val esPredefinida = SubcategoriaUtils.esSubcategoriaValida(nombreLimpio, subcategoriaLimpia)

        if (esPredefinida) {
            println("Creando categoría predefinida: $nombreLimpio - $subcategoriaLimpia")
        } else {
            println("Creando categoría personalizada: $nombreLimpio - $subcategoriaLimpia")
        }

        val categoria = Categoria(
            nombre = nombreLimpio,
            subcategoria = subcategoriaLimpia,
            descripcion = vista.descripcion.trim().ifEmpty {
                "Aires acondicionados $nombreLimpio $subcategoriaLimpia"
            }
        )

        scope.launch {
            try {
                val exito = modelo.insertarCategoria(categoria)
                if (exito) {
                    vista.limpiarFormulario()
                    vista.mostrarFormulario = false
                    cargarCategoriasDesdeModelo()
                } else {
                    println("⚠No se pudo crear la categoría (posiblemente ya existe)")
                    println("   Verifica que la combinación $nombreLimpio - $subcategoriaLimpia no esté duplicada")
                }
            } catch (e: Exception) {
                println(" Error al crear categoría: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun eliminarCategoria(categoria: Categoria) {
        scope.launch {
            val productosUsandoCategoria = modelo.contarProductosEnCategoria(categoria.id)

            if (productosUsandoCategoria > 0) {
                println("⚠No se puede eliminar. Hay $productosUsandoCategoria productos usando esta categoría")
            } else {
                val exito = modelo.eliminarCategoria(categoria.id)
                if (exito) {
                    cargarCategoriasDesdeModelo()
                    println("✅ Categoría eliminada: ${categoria.nombre} - ${categoria.subcategoria}")
                } else {
                    println("Error al eliminar categoría")
                }
            }
        }
    }

    private fun cargarCategoriasDesdeModelo() {
        scope.launch {
            val categorias = modelo.obtenerTodasLasCategorias()
            vista.actualizarCategorias(categorias)
            println("Categorías cargadassss: ${categorias.size}")
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