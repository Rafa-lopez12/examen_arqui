package com.example.tienda_emprendedor.controller

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.tienda_emprendedor.model.*
import com.example.tienda_emprendedor.view.VentaView
import com.example.tienda_emprendedor.view.PagoView
import com.example.tienda_emprendedor.service.StripeService

class VentaController {

    private val ventaDao: VentaDao = VentaDao()
    private val clienteDao: ClienteDao = ClienteDao()
    private val productoDao: ProductoDao = ProductoDao()
    private val pagoDao: PagoDao = PagoDao()
    private val vista: VentaView = VentaView()
    private val pagoView: PagoView = PagoView()
    private val stripeService: StripeService = StripeService()
    private val scope = CoroutineScope(Dispatchers.Main)

    init {

        configurarEventosVista()
        configurarEventosPago()
        cargarDatosIniciales()
    }

    private fun configurarEventosVista() {
        vista.onNuevaVentaClick = {
            iniciarNuevaVenta()
        }

        vista.onSeleccionarClienteClick = { cliente ->
            seleccionarCliente(cliente)
        }

        vista.onAgregarProductoClick = { producto, cantidad ->
            println("Producto agregado desde controlador: ${producto.nombre} x$cantidad")
        }

        vista.onEliminarItemCarritoClick = { productoId ->
            println("🗑️ Item eliminado desde controlador: $productoId")
        }

        vista.onConfirmarVentaClick = { venta, detalles ->
            confirmarVenta(venta, detalles)
        }

        vista.onBuscarClientesClick = { busqueda ->
            buscarClientes(busqueda)
        }

        vista.onBuscarProductosClick = { busqueda ->
            buscarProductos(busqueda)
        }

        vista.onIrAPagoClick = { venta ->
            irAPago(venta)
        }

        vista.onVolverAListaClick = {
            volverALista()
        }

        vista.onSeleccionarMetodoPagoClick = { venta, metodoPagoId ->
            procesarVentaConMetodoPago(venta, metodoPagoId)
        }

        vista.onMostrarPagoViewClick = { venta ->
            mostrarPagoView(venta)
        }
    }

    private fun configurarEventosPago() {
        pagoView.onResultadoStripeCallback = { success, paymentIntentId, error ->
            manejarResultadoStripe(success, paymentIntentId, error)
        }

        pagoView.onVolverAVentasClick = {
            volverALista()
        }

        pagoView.onPagoCompletadoClick = { venta ->
            cargarVentas()
        }

        pagoView.onProcesarPagoClick = { venta ->
            procesarPago(venta)
        }

        pagoView.onReintentarPagoClick = { venta ->
            reintentarPago(venta)
        }
    }

    private fun iniciarNuevaVenta() {
        vista.limpiarCarrito()
        cargarProductosDisponibles()
        cargarMetodosPago()
        println("🛒 Iniciando nueva venta")
    }

    private fun seleccionarCliente(cliente: Cliente) {
        vista.clienteSeleccionado = cliente
        vista.busquedaCliente = ""
        println("👤 Cliente seleccionado: ${cliente.nombre} ${cliente.apellido}")
    }

    private fun confirmarVenta(venta: Venta, detalles: List<DetalleVenta>) {

        if (detalles.isEmpty()) {
            return
        }

        if (venta.clienteId == 0) {
            return
        }

        detalles.forEach { detalle ->
        }

        scope.launch {
            try {
                val stockSuficiente = verificarStockDisponible(detalles)
                if (!stockSuficiente) {
                    return@launch
                }

                vista.ventaParaPago = venta
                vista.detallesParaPago = detalles
                vista.vistaActual = "seleccion_pago"


            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun procesarVentaConMetodoPago(venta: Venta, metodoPagoId: Int) {

        scope.launch {
            try {
                venta.pagoId = metodoPagoId

                val metodoPago = pagoDao.obtenerMetodoPagoPorId(metodoPagoId)

                if (metodoPagoId == 1) {
                    procesarPagoEfectivo(venta, vista.detallesParaPago)

                } else if (metodoPagoId == 2) { // Tarjeta
                    procesarPagoTarjeta(venta, vista.detallesParaPago)

                } else {
                    println("Método de pago no válido: $metodoPagoId")
                }

            } catch (e: Exception) {
                println("Error procesando venta con método de pago: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private suspend fun procesarPagoEfectivo(venta: Venta, detalles: List<DetalleVenta>) {
        try {

            venta.estado = "completada"

            val ventaId = ventaDao.generarVenta(venta, detalles)

            if (ventaId > 0) {

                vista.limpiarCarrito()
                cargarVentas()
                vista.vistaActual = "lista"


            } else {
                println("Error al crear la venta en efectivo")
            }

        } catch (e: Exception) {
            println("Error procesando pago en efectivo: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun procesarPagoTarjeta(venta: Venta, detalles: List<DetalleVenta>) {
        try {
            venta.estado = "pendiente"
            val ventaId = ventaDao.generarVenta(venta, detalles)

            if (ventaId > 0) {

                val ventaCreada = ventaDao.obtenerVentaPorId(ventaId)
                if (ventaCreada != null) {
                    configurarVentaParaPago(ventaCreada)
                    vista.ventaParaPago = ventaCreada
                    vista.pagoView = pagoView // Pasar PagoView a VentaView
                    vista.vistaActual = "pago_stripe"
                } else {
                    println("No se pudo obtener la venta creada")
                }

            } else {
                println("Error al crear la venta para tarjeta")
            }

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }

    private fun configurarVentaParaPago(venta: Venta) {
        pagoView.ventaParaPago = venta
        pagoView.estadoPago = "inicial"
        pagoView.mensajeError = ""
        pagoView.clientSecret = ""
    }


    private fun procesarPago(venta: Venta) {

        scope.launch {
            try {
                pagoView.estadoPago = "procesando"
                pagoView.mensajeError = ""

                val amountInCents = stripeService.dollarsToCents(venta.total)
                val description = "Venta #${venta.id} - ${venta.nombreCliente} ${venta.apellidoCliente}"
                val result = stripeService.createPaymentIntent(
                    amount = amountInCents,
                    currency = "usd",
                    description = description
                )

                result.onSuccess { clientSecret ->
                    pagoView.clientSecret = clientSecret
                    pagoView.estadoPago = "listo_para_pagar"

                }.onFailure { error ->
                    pagoView.estadoPago = "error"
                    pagoView.mensajeError = error.message ?: "Error desconocido al crear el pago"
                }

            } catch (e: Exception) {
                e.printStackTrace()
                pagoView.estadoPago = "error"
                pagoView.mensajeError = e.message ?: "Error desconocido"
            }
        }
    }

    private fun manejarResultadoStripe(success: Boolean, paymentIntentId: String?, error: String?) {

        scope.launch {
            try {
                if (success && paymentIntentId != null) {


                    pagoView.ventaParaPago?.let { venta ->
                        val ventaActualizada = ventaDao.actualizarEstadoVenta(venta.id, "completada")
                        if (ventaActualizada) {

                            pagoView.estadoPago = "completado"
                            pagoView.mensajeError = ""
                            pagoView.paymentIntentId = paymentIntentId


                            kotlinx.coroutines.delay(3000) // 3 segundos para ver el éxito
                            volverALista()

                        } else {
                            pagoView.estadoPago = "error"
                            pagoView.mensajeError = "Error al actualizar el estado de la venta"
                        }
                    }

                } else {
                    pagoView.estadoPago = "error"
                    pagoView.mensajeError = error ?: "El pago fue cancelado o falló"
                }

            } catch (e: Exception) {
                e.printStackTrace()
                pagoView.estadoPago = "error"
                pagoView.mensajeError = "Error procesando el resultado del pago: ${e.message}"
            }
        }
    }

    private fun reintentarPago(venta: Venta) {
        pagoView.estadoPago = "inicial"
        pagoView.mensajeError = ""
        pagoView.clientSecret = ""
    }


    private fun mostrarPagoView(venta: Venta) {
        configurarVentaParaPago(venta)
        vista.pagoView = pagoView // Pasar la vista de pago a VentaView
        vista.vistaActual = "pago_stripe"
    }

    private fun buscarClientes(busqueda: String) {
        scope.launch {
            val clientes = if (busqueda.trim().length >= 2) {
                clienteDao.buscarClientesPorNombre(busqueda.trim())
            } else {
                clienteDao.obtenerTodosLosClientes().take(10)
            }
            vista.actualizarClientesDisponibles(clientes)
        }
    }

    private fun buscarProductos(busqueda: String) {
        scope.launch {
            val productos = if (busqueda.trim().length >= 2) {
                val todosLosProductos = productoDao.obtenerTodosLosProductos()
                todosLosProductos.filter { producto ->
                    producto.nombre.contains(busqueda, ignoreCase = true) ||
                            producto.descripcion.contains(busqueda, ignoreCase = true) ||
                            producto.nombreCategoria.contains(busqueda, ignoreCase = true) ||
                            producto.subcategoria.contains(busqueda, ignoreCase = true)
                }.filter { it.stock > 0 }
            } else {
                productoDao.obtenerTodosLosProductos().filter { it.stock > 0 }
            }
            vista.actualizarProductosDisponibles(productos.take(10))
        }
    }

    private fun irAPago(venta: Venta) {
        mostrarPagoView(venta)
    }

    private fun volverALista() {
        vista.vistaActual = "lista"
        limpiarDatosPago()
        cargarVentas()
    }

    private fun limpiarDatosPago() {
        pagoView.ventaParaPago = null
        pagoView.estadoPago = "inicial"
        pagoView.mensajeError = ""
        pagoView.clientSecret = ""
        pagoView.paymentIntentId = ""
    }

    private fun cargarDatosIniciales() {
        cargarVentas()
        cargarMetodosPago()
    }

    private fun cargarVentas() {
        scope.launch {
            try {
                val ventas = ventaDao.obtenerTodasLasVentas()
                vista.actualizarVentas(ventas)
                ventas.forEach { venta ->
                    println("  - Venta #${venta.id}: ${venta.nombreCliente} ${venta.apellidoCliente} - ${venta.total} - ${venta.metodoPago}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun cargarProductosDisponibles() {
        scope.launch {
            val productos = productoDao.obtenerTodosLosProductos()
                .filter { it.stock > 0 }
            vista.actualizarProductosDisponibles(productos)
        }
    }

    private fun cargarMetodosPago() {
        scope.launch {
            try {
                val metodosPago = pagoDao.obtenerMetodosPago()
                vista.actualizarMetodosPago(metodosPago)
                metodosPago.forEach { metodo ->
                    println("  - ${metodo.id}: ${metodo.nombre}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun verificarStockDisponible(detalles: List<DetalleVenta>): Boolean {
        return try {
            detalles.all { detalle ->

                val producto = productoDao.obtenerProductoPorId(detalle.productoId)
                if (producto != null) {

                    val stockSuficiente = producto.stock >= detalle.cantidad
                    if (!stockSuficiente) {
                        println("Stock insuficiente: necesita ${detalle.cantidad}, disponible ${producto.stock}")
                    }
                    stockSuficiente
                } else {
                    println("Producto no encontrado con ID: ${detalle.productoId}")
                    false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    fun obtenerVista(): VentaView {
        return vista
    }

    fun obtenerModelo(): VentaDao {
        return ventaDao
    }

    fun recargarDatos() {
        cargarVentas()
        cargarProductosDisponibles()
        cargarMetodosPago()
    }
}