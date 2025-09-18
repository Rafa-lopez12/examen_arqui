package com.example.tienda_emprendedor.controller

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.tienda_emprendedor.model.*
import com.example.tienda_emprendedor.view.VentaView

class VentaController {

    private val ventaDao: VentaDao = VentaDao()
    private val clienteDao: ClienteDao = ClienteDao()
    private val productoDao: ProductoDao = ProductoDao()
    private val vista: VentaView = VentaView()
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
            // No necesitamos hacer nada aquí porque ya se maneja en la vista
            println("✅ Producto agregado desde controlador: ${producto.nombre} x$cantidad")
        }

        vista.onEliminarItemCarritoClick = { productoId ->
            // No necesitamos hacer nada aquí porque ya se maneja en la vista
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

    private fun agregarProductoAlCarrito(producto: Producto, cantidad: Int) {
        // Validar stock disponible
        if (cantidad > producto.stock) {
            println("❌ No hay suficiente stock. Disponible: ${producto.stock}, solicitado: $cantidad")
            return
        }

        // Verificar si ya existe en el carrito
        val cantidadEnCarrito = vista.itemsCarrito[producto.id]?.cantidad ?: 0
        val cantidadTotal = cantidadEnCarrito + cantidad

        if (cantidadTotal > producto.stock) {
            println("❌ No se puede agregar. Total en carrito sería: $cantidadTotal, stock disponible: ${producto.stock}")
            return
        }

        vista.agregarAlCarrito(producto, cantidad)
        println("✅ Producto agregado: ${producto.nombre} x$cantidad - Total: $${vista.calcularTotal()}")
    }

    private fun eliminarItemDelCarrito(productoId: Int) {
        val item = vista.itemsCarrito[productoId]
        if (item != null) {
            vista.eliminarDelCarrito(productoId)
            println("🗑️ Producto eliminado del carrito: ${item.nombreProducto}")
        }
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
                // Verificar stock antes de crear la venta
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
                // Buscar por nombre de producto
                val todosLosProductos = productoDao.obtenerTodosLosProductosConCategoria()
                todosLosProductos.filter { producto ->
                    producto.nombre.contains(busqueda, ignoreCase = true) ||
                            producto.descripcion.contains(busqueda, ignoreCase = true) ||
                            producto.nombreCategoria.contains(busqueda, ignoreCase = true) ||
                            producto.subcategoria.contains(busqueda, ignoreCase = true)
                }.filter { it.stock > 0 } // Solo productos con stock
            } else {
                // Mostrar todos los productos con stock
                productoDao.obtenerTodosLosProductosConCategoria().filter { it.stock > 0 }
            }
            vista.actualizarProductosDisponibles(productos.take(10))
            println("🔍 Búsqueda productos '$busqueda': ${productos.size} resultados")
        }
    }

    private fun irAPago(venta: Venta) {
        vista.ventaParaPago = venta
        vista.vistaActual = "pago"
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
            // Cargar productos con stock > 0
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

    // Método para recargar datos
    fun recargarDatos() {
        cargarVentasDesdeModelo()
        cargarProductosDisponibles()
    }
}