package com.example.tienda_emprendedor.controller

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.tienda_emprendedor.model.Producto
import com.example.tienda_emprendedor.model.ProductoDao
import com.example.tienda_emprendedor.view.ProductoView

class ProductoController {

    // El controlador tiene instancias del modelo y la vista
    private val modelo: ProductoDao = ProductoDao()  // MODELO maneja la BD
    private val vista: ProductoView = ProductoView()  // VISTA maneja UI

    // Scope para operaciones asíncronas
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        configurarEventosVista()
        cargarProductosDesdeModelo()
    }

    private fun configurarEventosVista() {
        // El controlador escucha eventos de la vista
        vista.onAgregarClick = {
            agregarProducto()
        }

        vista.onEliminarClick = { producto ->
            eliminarProducto(producto)
        }
    }

    private fun agregarProducto() {
        // El controlador coordina entre vista y modelo
        if (vista.nombre.isNotEmpty() && vista.precio.isNotEmpty()) {
            val producto = Producto(
                nombre = vista.nombre,
                descripcion = vista.descripcion,
                precio = vista.precio.toDoubleOrNull() ?: 0.0,
                categoria = vista.categoria,
                stock = vista.stock.toIntOrNull() ?: 0
            )

            // El controlador pide al MODELO que guarde (no lo hace directamente)
            scope.launch {
                val exito = modelo.insertarProducto(producto)
                if (exito) {
                    vista.limpiarFormulario()
                    cargarProductosDesdeModelo()
                }
            }
        }
    }

    private fun eliminarProducto(producto: Producto) {
        // El controlador pide al MODELO que elimine
        scope.launch {
            val exito = modelo.eliminarProducto(producto.id)
            if (exito) {
                cargarProductosDesdeModelo()
            }
        }
    }

    private fun cargarProductosDesdeModelo() {
        // El controlador pide datos al MODELO y actualiza la VISTA
        scope.launch {
            val productos = modelo.obtenerTodosLosProductos()
            vista.actualizarProductos(productos)
        }
    }

    // Métodos para el MainActivity
    fun obtenerVista(): ProductoView {
        return vista
    }

    fun obtenerModelo(): ProductoDao {
        return modelo
    }
}