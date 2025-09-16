package com.example.tienda_emprendedor.controller

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.tienda_emprendedor.model.Producto
import com.example.tienda_emprendedor.model.ProductoDao
import com.example.tienda_emprendedor.view.ProductoView

class ProductoController {


    private val modelo: ProductoDao = ProductoDao()
    private val vista: ProductoView = ProductoView()


    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        configurarEventosVista()
        cargarProductosDesdeModelo()
    }

    private fun configurarEventosVista() {
        vista.onAgregarClick = {
            agregarProducto()
        }

        vista.onEliminarClick = { producto ->
            eliminarProducto(producto)
        }
    }

    private fun agregarProducto() {
        if (vista.nombre.isNotEmpty() && vista.precio.isNotEmpty()) {
            val producto = Producto(
                nombre = vista.nombre,
                descripcion = vista.descripcion,
                precio = vista.precio.toDoubleOrNull() ?: 0.0,
                categoria = vista.categoria,
                stock = vista.stock.toIntOrNull() ?: 0
            )

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
        scope.launch {
            val exito = modelo.eliminarProducto(producto.id)
            if (exito) {
                cargarProductosDesdeModelo()
            }
        }
    }

    private fun cargarProductosDesdeModelo() {
        scope.launch {
            val productos = modelo.obtenerTodosLosProductos()
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