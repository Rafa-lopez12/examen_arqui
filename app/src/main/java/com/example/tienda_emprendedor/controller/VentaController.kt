package com.example.tienda_emprendedor.controller

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.tienda_emprendedor.model.*
import com.example.tienda_emprendedor.view.VentaView
import com.example.tienda_emprendedor.service.StripeService

class VentaController {

    private val ventaDao: VentaDao = VentaDao()
    private val clienteDao: ClienteDao = ClienteDao()
    private val productoDao: ProductoDao = ProductoDao()
    private val pagoDao: PagoDao = PagoDao()
    private val vista: VentaView = VentaView()
    private val stripeService: StripeService = StripeService()
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        println("🔧 Inicializando VentaController...")
        configurarEventosVista()
        cargarDatosIniciales()
        println("✅ VentaController inicializado")
    }

    private fun configurarEventosVista() {
        vista.onNuevaVentaClick = {
            iniciarNuevaVenta()
        }

        vista.onSeleccionarClienteClick = { cliente ->
            seleccionarCliente(cliente)
        }

        vista.onAgregarProductoClick = { producto, cantidad ->
            println("✅ Producto agregado desde controlador: ${producto.nombre} x$cantidad")
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

        // ✅ INTEGRACIÓN REAL CON STRIPE
        vista.onProcesarPagoStripeClick = { venta ->
            procesarPagoConStripe(venta)
        }

        vista.onPaymentResultCallback = { success, paymentIntentId, error ->
            manejarResultadoPago(success, paymentIntentId, error)
        }
    }

    private fun procesarPagoConStripe(venta: Venta) {
        println("💳 ===== INICIANDO PAGO REAL CON STRIPE =====")
        println("Venta ID: ${venta.id}")
        println("Cliente: ${venta.nombreCliente} ${venta.apellidoCliente}")
        println("Monto: \$${venta.total}")

        scope.launch {
            try {
                // Actualizar estado de la vista
                vista.estadoPago = "procesando"
                vista.mensajeError = ""

                println("🔧 Creando PaymentIntent...")

                // Convertir dólares a centavos para Stripe
                val amountInCents = stripeService.dollarsToCents(venta.total)
                val description = "Venta #${venta.id} - ${venta.nombreCliente} ${venta.apellidoCliente}"

                println("💰 Monto en centavos: $amountInCents")

                // Crear PaymentIntent real con Stripe
                val result = stripeService.createPaymentIntent(
                    amount = amountInCents,
                    currency = "usd",
                    description = description
                )

                result.onSuccess { clientSecret ->
                    println("✅ PaymentIntent creado exitosamente")
                    println("🔑 Client Secret recibido: ${clientSecret.substring(0, 20)}...")

                    // Guardar el client secret en la vista
                    vista.clientSecret = clientSecret
                    vista.estadoPago = "listo_para_pagar"

                    // Aquí es donde se debe mostrar el PaymentSheet
                    // Esto se maneja en la vista con rememberPaymentSheet

                }.onFailure { error ->
                    println("❌ Error creando PaymentIntent: ${error.message}")
                    vista.estadoPago = "error"
                    vista.mensajeError = error.message ?: "Error desconocido al crear el pago"
                }

            } catch (e: Exception) {
                println("❌ Error general procesando pago: ${e.message}")
                e.printStackTrace()
                vista.estadoPago = "error"
                vista.mensajeError = e.message ?: "Error desconocido"
            }
        }
    }

    // ✅ Manejar resultado del pago de Stripe
    private fun manejarResultadoPago(success: Boolean, paymentIntentId: String?, error: String?) {

        println("🎯 ===== RESULTADO DEL PAGO =====")
        println("Éxito: $success")
        println("PaymentIntent ID: $paymentIntentId")
        println("Error: $error")

        scope.launch {
            try {
                if (success && paymentIntentId != null) {
                    // ✅ PAGO EXITOSO
                    println("✅ Pago completado exitosamente")

                    vista.ventaParaPago?.let { venta ->
                        // 1. Actualizar estado de la venta
                        val ventaActualizada = ventaDao.actualizarEstadoVenta(venta.id, "completada")

                        if (ventaActualizada) {
                            // 2. Registrar el pago en la base de datos
                            val pago = Pago(
                                ventaId = venta.id,
                                monto = venta.total,
                                metodoPago = "stripe",
                                referencia = paymentIntentId,
                                estado = "completado"
                            )

                            val pagoId = pagoDao.insertarPago(pago)

                            if (pagoId > 0) {
                                println("✅ Pago registrado en BD con ID: $pagoId")

                                // 3. Actualizar estado de la vista
                                vista.estadoPago = "completado"
                                vista.mensajeError = ""

                                // 4. Volver a la lista de ventas después de un delay
                                kotlinx.coroutines.delay(2000)
                                vista.vistaActual = "lista"
                                cargarVentasDesdeModelo()

                                println("🎉 Proceso de pago completado exitosamente")

                            } else {
                                println("❌ Error al registrar pago en BD")
                                vista.estadoPago = "error"
                                vista.mensajeError = "Error al registrar el pago en la base de datos"
                            }
                        } else {
                            println("❌ Error al actualizar estado de venta")
                            vista.estadoPago = "error"
                            vista.mensajeError = "Error al actualizar el estado de la venta"
                        }
                    }

                } else {
                    // ❌ PAGO FALLIDO O CANCELADO
                    println("❌ Pago fallido o cancelado")
                    vista.estadoPago = "error"
                    vista.mensajeError = error ?: "El pago fue cancelado o falló"
                }

            } catch (e: Exception) {
                println("❌ Error manejando resultado del pago: ${e.message}")
                e.printStackTrace()
                vista.estadoPago = "error"
                vista.mensajeError = "Error procesando el resultado del pago: ${e.message}"
            }
        }
    }

    private fun iniciarNuevaVenta() {
        vista.limpiarCarrito()
        cargarProductosDisponibles()
        println("🛒 Iniciando nueva venta")
    }

    private fun seleccionarCliente(cliente: Cliente) {
        vista.clienteSeleccionado = cliente
        vista.busquedaCliente = ""
        println("👤 Cliente seleccionado: ${cliente.nombre} ${cliente.apellido}")
    }

    private fun confirmarVenta(venta: Venta, detalles: List<DetalleVenta>) {
        println("🛒 ===== INICIANDO CONFIRMACIÓN DE VENTA =====")
        println("Cliente ID: ${venta.clienteId}")
        println("Total: ${venta.total}")
        println("Cantidad de productos: ${detalles.size}")

        if (detalles.isEmpty()) {
            println("❌ No se puede crear venta sin productos")
            return
        }

        if (venta.clienteId == 0) {
            println("❌ Debe seleccionar un cliente")
            return
        }

        detalles.forEach { detalle ->
            println("  - Producto ID: ${detalle.productoId}, Cantidad: ${detalle.cantidad}, Precio: ${detalle.precioUnitario}")
        }

        scope.launch {
            try {
                println("🔍 Verificando stock disponible...")
                val stockSuficiente = verificarStockDisponible(detalles)
                if (!stockSuficiente) {
                    println("❌ No hay stock suficiente para algunos productos")
                    return@launch
                }

                println("✅ Stock verificado, creando venta...")
                val ventaId = ventaDao.insertarVenta(venta, detalles)

                if (ventaId > 0) {
                    println("✅ Venta creada exitosamente con ID: $ventaId")

                    vista.limpiarCarrito()
                    println("🧹 Carrito limpiado")

                    cargarVentasDesdeModelo()
                    println("📊 Lista de ventas actualizada")

                    // Ir a la vista de pago con la venta creada
                    val ventaCreada = ventaDao.obtenerVentaPorId(ventaId)
                    if (ventaCreada != null) {
                        vista.ventaParaPago = ventaCreada
                        vista.vistaActual = "pago"
                        vista.estadoPago = "inicial" // ✅ Reset estado del pago
                        println("💳 Redirigiendo a vista de pago")
                    } else {
                        println("❌ No se pudo obtener la venta creada")
                    }
                } else {
                    println("❌ Error al crear la venta - ID retornado: $ventaId")
                }
            } catch (e: Exception) {
                println("❌ Error al confirmar venta: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun buscarClientes(busqueda: String) {
        scope.launch {
            val clientes = if (busqueda.trim().length >= 2) {
                clienteDao.buscarClientesPorNombre(busqueda.trim())
            } else {
                clienteDao.obtenerTodosLosClientes().take(10)
            }
            vista.actualizarClientesDisponibles(clientes)
            println("🔍 Búsqueda clientes '$busqueda': ${clientes.size} resultados")
        }
    }

    private fun buscarProductos(busqueda: String) {
        scope.launch {
            val productos = if (busqueda.trim().length >= 2) {
                val todosLosProductos = productoDao.obtenerTodosLosProductosConCategoria()
                todosLosProductos.filter { producto ->
                    producto.nombre.contains(busqueda, ignoreCase = true) ||
                            producto.descripcion.contains(busqueda, ignoreCase = true) ||
                            producto.nombreCategoria.contains(busqueda, ignoreCase = true) ||
                            producto.subcategoria.contains(busqueda, ignoreCase = true)
                }.filter { it.stock > 0 }
            } else {
                productoDao.obtenerTodosLosProductosConCategoria().filter { it.stock > 0 }
            }
            vista.actualizarProductosDisponibles(productos.take(10))
            println("🔍 Búsqueda productos '$busqueda': ${productos.size} resultados")
        }
    }

    private fun irAPago(venta: Venta) {
        vista.ventaParaPago = venta
        vista.vistaActual = "pago"
        vista.estadoPago = "inicial" // ✅ Reset estado
        println("💳 Procesando pago para venta #${venta.id}")
    }

    private fun volverALista() {
        vista.vistaActual = "lista"
        cargarVentasDesdeModelo()
    }

    private fun cargarDatosIniciales() {
        cargarVentasDesdeModelo()
    }

    private fun cargarVentasDesdeModelo() {
        println("📊 Iniciando carga de ventas...")
        scope.launch {
            try {
                val ventas = ventaDao.obtenerTodasLasVentas()
                vista.actualizarVentas(ventas)
                println("🛒 Ventas cargadas exitosamente: ${ventas.size}")
                ventas.forEach { venta ->
                    println("  - Venta #${venta.id}: ${venta.nombreCliente} ${venta.apellidoCliente} - ${venta.total}")
                }
            } catch (e: Exception) {
                println("❌ Error al cargar ventas: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun cargarProductosDisponibles() {
        scope.launch {
            val productos = productoDao.obtenerTodosLosProductosConCategoria()
                .filter { it.stock > 0 }
            vista.actualizarProductosDisponibles(productos)
            println("📦 Productos con stock cargados: ${productos.size}")
        }
    }

    private suspend fun verificarStockDisponible(detalles: List<DetalleVenta>): Boolean {
        return try {
            println("🔍 Verificando stock para ${detalles.size} productos...")
            detalles.all { detalle ->
                println("  - Verificando producto ID: ${detalle.productoId}, cantidad solicitada: ${detalle.cantidad}")
                val producto = productoDao.obtenerProductoPorId(detalle.productoId)
                if (producto != null) {
                    println("    ✅ Producto encontrado: ${producto.nombre}, stock actual: ${producto.stock}")
                    val stockSuficiente = producto.stock >= detalle.cantidad
                    if (!stockSuficiente) {
                        println("    ❌ Stock insuficiente: necesita ${detalle.cantidad}, disponible ${producto.stock}")
                    }
                    stockSuficiente
                } else {
                    println("    ❌ Producto no encontrado con ID: ${detalle.productoId}")
                    false
                }
            }
        } catch (e: Exception) {
            println("❌ Error al verificar stock: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // Método para actualizar estado de venta (útil para pagos)
    fun actualizarEstadoVenta(ventaId: Int, nuevoEstado: String, callback: (Boolean) -> Unit) {
        scope.launch {
            val exito = ventaDao.actualizarEstadoVenta(ventaId, nuevoEstado)
            callback(exito)
            if (exito) {
                cargarVentasDesdeModelo()
                println("✅ Estado de venta #$ventaId actualizado a: $nuevoEstado")
            } else {
                println("❌ Error al actualizar estado de venta #$ventaId")
            }
        }
    }

    // Método para obtener ventas por cliente
    fun obtenerVentasCliente(clienteId: Int, callback: (List<Venta>) -> Unit) {
        scope.launch {
            val ventas = ventaDao.obtenerVentasPorCliente(clienteId)
            callback(ventas)
        }
    }

    // Método para obtener detalle completo de una venta
    fun obtenerDetalleVenta(ventaId: Int, callback: (Venta?) -> Unit) {
        scope.launch {
            val venta = ventaDao.obtenerVentaPorId(ventaId)
            callback(venta)
        }
    }

    // Estadísticas básicas
    fun obtenerEstadisticasVentas(callback: (Map<String, Any>) -> Unit) {
        scope.launch {
            val todasLasVentas = ventaDao.obtenerTodasLasVentas()
            val stats = mapOf(
                "totalVentas" to todasLasVentas.size,
                "ventasCompletadas" to todasLasVentas.count { it.estado == "completada" },
                "ventasPendientes" to todasLasVentas.count { it.estado == "pendiente" },
                "montoTotalVentas" to todasLasVentas.filter { it.estado == "completada" }.sumOf { it.total },
                "promedioVenta" to if (todasLasVentas.isNotEmpty()) {
                    todasLasVentas.filter { it.estado == "completada" }.sumOf { it.total } / todasLasVentas.count { it.estado == "completada" }.coerceAtLeast(1)
                } else 0.0
            )
            callback(stats)
        }
    }

    fun obtenerVista(): VentaView {
        return vista
    }

    fun obtenerModelo(): VentaDao {
        return ventaDao
    }

    fun recargarDatos() {
        cargarVentasDesdeModelo()
        cargarProductosDisponibles()
    }
}