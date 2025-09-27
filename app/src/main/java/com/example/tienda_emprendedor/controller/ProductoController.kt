package com.example.tienda_emprendedor.controller

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.tienda_emprendedor.model.Producto
import com.example.tienda_emprendedor.model.ProductoDao
import com.example.tienda_emprendedor.model.CategoriaDao
import com.example.tienda_emprendedor.view.ProductoView

class ProductoController {

    private val modelo: ProductoDao = ProductoDao()
    private val categoriaDao: CategoriaDao = CategoriaDao()
    private val vista: ProductoView = ProductoView()
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        configurarEventosVista()
        cargarDatosIniciales()
    }

    private fun configurarEventosVista() {
        vista.onAgregarClick = {
            agregarProducto()
        }

        vista.onEliminarClick = { producto ->
            eliminarProducto(producto)
        }

        vista.onCategoriaChanged = { categoria ->

            vista.subcategoriaSeleccionada = categoria.subcategoria
        }
    }

    private fun agregarProducto() {
            if (vista.nombre.isNotEmpty() &&
                vista.precio.isNotEmpty() &&
                vista.categoriaSeleccionada != null) {

                val categoria = vista.categoriaSeleccionada!!
                val producto = Producto(
                    nombre = vista.nombre,
                    descripcion = vista.descripcion,
                    precio = vista.precio.toDoubleOrNull() ?: 0.0,
                    categoriaId = categoria.id,
                    subcategoria = categoria.subcategoria,
                    stock = vista.stock.toIntOrNull() ?: 0
                )

                scope.launch {
                    val exito = modelo.insertarProducto(producto)
                    if (exito) {
                        vista.limpiarFormulario()
                        cargarProductos()
                        println("Producto agregado: ${producto.nombre}")
                    } else {
                        println("Error al agregar producto")
                    }
                }
            } else {
                println("Faltan campos obligatorios")
            }
        }

        private fun eliminarProducto(producto: Producto) {
            scope.launch {
                val exito = modelo.eliminarProducto(producto.id)
                if (exito) {
                    cargarProductos()
                    println(" eliminado: ${producto.nombre}")
                } else {
                    println("Error al eliminar producto")
                }
            }
        }

        private fun mostrarDetalleProducto(producto: Producto) {
            vista.productoSeleccionado = producto
            vista.mostrarDetalleProducto = true
        }

        private fun cargarDatosIniciales() {
            cargarCategoriasDisponibles()
            cargarProductos()
        }

        private fun cargarCategoriasDisponibles() {
            scope.launch {
                val categorias = categoriaDao.obtenerTodasLasCategorias()
                vista.actualizarCategorias(categorias)
                println("📋 Categorías cargadas para productos: ${categorias.size}")
            }
        }

        private fun cargarProductos() {
            scope.launch {
                val productos = modelo.obtenerTodosLosProductosConCategoria()
                vista.actualizarProductos(productos)
                println("📦 Productos cargados: ${productos.size}")
            }
        }


        fun filtrarPorCategoria(categoriaId: Int) {
            scope.launch {
                val productos = modelo.obtenerProductosPorCategoria(categoriaId)
                vista.actualizarProductos(productos)
            }
        }


        fun filtrarPorSubcategoria(subcategoria: String) {
            scope.launch {
                val productos = modelo.obtenerProductosPorSubcategoria(subcategoria)
                vista.actualizarProductos(productos)
            }
        }

        fun obtenerVista(): ProductoView {
            return vista
        }

        fun obtenerModelo(): ProductoDao {
            return modelo
        }
    }