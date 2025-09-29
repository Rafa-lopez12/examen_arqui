package com.example.tienda_emprendedor.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tienda_emprendedor.model.*
import java.text.DecimalFormat

class VentaView {

    var clienteSeleccionado by mutableStateOf<Cliente?>(null)
    var productosDisponibles by mutableStateOf(listOf<Producto>())
    var clientesDisponibles by mutableStateOf(listOf<Cliente>())
    var metodosPago by mutableStateOf(listOf<Pago>())
    var itemsCarrito by mutableStateOf<Map<Int, DetalleVenta>>(emptyMap())
    var descuento by mutableStateOf("")
    var notas by mutableStateOf("")

    // Estados de UI
    var vistaActual by mutableStateOf("lista") // "lista", "nueva_venta", "seleccion_pago", "pago_stripe"
    var busquedaCliente by mutableStateOf("")
    var busquedaProducto by mutableStateOf("")

    var ventas by mutableStateOf(listOf<Venta>())
    var ventaParaPago by mutableStateOf<Venta?>(null)
    var detallesParaPago by mutableStateOf(listOf<DetalleVenta>())

    // 🆕 Referencia a PagoView
    var pagoView by mutableStateOf<PagoView?>(null)

    // Estados para productos
    var cantidadProducto by mutableStateOf("1")

    // Callbacks
    var onNuevaVentaClick: () -> Unit = {}
    var onSeleccionarClienteClick: (Cliente) -> Unit = { _ -> }
    var onAgregarProductoClick: (Producto, Int) -> Unit = { _, _ -> }
    var onEliminarItemCarritoClick: (Int) -> Unit = { _ -> }
    var onConfirmarVentaClick: (Venta, List<DetalleVenta>) -> Unit = { _, _ -> }
    var onBuscarClientesClick: (String) -> Unit = { _ -> }
    var onBuscarProductosClick: (String) -> Unit = { _ -> }
    var onIrAPagoClick: (Venta) -> Unit = { _ -> }
    var onVolverAListaClick: () -> Unit = {}
    var onSeleccionarMetodoPagoClick: (Venta, Int) -> Unit = { _, _ -> }
    var onMostrarPagoViewClick: (Venta) -> Unit = { _ -> } // 🆕 Callback para mostrar PagoView

    fun actualizarVentas(nuevasVentas: List<Venta>) {
        println("📋 VentaView: Actualizando ventas. Cantidad: ${nuevasVentas.size}")
        ventas = nuevasVentas
        nuevasVentas.forEach { venta ->
            println("  - Vista recibió venta #${venta.id}: ${venta.nombreCliente} - ${venta.total} - ${venta.metodoPago}")
        }
    }

    fun actualizarProductosDisponibles(productos: List<Producto>) {
        productosDisponibles = productos
    }

    fun actualizarClientesDisponibles(clientes: List<Cliente>) {
        clientesDisponibles = clientes
    }

    fun actualizarMetodosPago(metodos: List<Pago>) {
        metodosPago = metodos
        println("💳 VentaView: Métodos de pago actualizados: ${metodos.size}")
    }

    fun agregarAlCarrito(producto: Producto, cantidad: Int) {
        val carritoMutable = itemsCarrito.toMutableMap()

        val detalle = DetalleVenta(
            productoId = producto.id,
            cantidad = cantidad,
            precioUnitario = producto.precio,
            subtotal = producto.precio * cantidad,
            nombreProducto = producto.nombre,
            descripcionProducto = producto.descripcion,
            stockDisponible = producto.stock,
            nombreCategoria = producto.nombreCategoria,
            subcategoria = producto.subcategoria
        )

        if (carritoMutable.containsKey(producto.id)) {
            val existente = carritoMutable[producto.id]!!
            val nuevaCantidad = existente.cantidad + cantidad
            if (nuevaCantidad <= producto.stock) {
                val actualizado = existente.copy(
                    cantidad = nuevaCantidad,
                    subtotal = existente.precioUnitario * nuevaCantidad
                )
                carritoMutable[producto.id] = actualizado
            }
        } else {
            carritoMutable[producto.id] = detalle
        }
        itemsCarrito = carritoMutable.toMap()

        println("🛒 Producto agregado al carrito. Total items: ${itemsCarrito.size}")
    }

    fun eliminarDelCarrito(productoId: Int) {
        val carritoMutable = itemsCarrito.toMutableMap()
        carritoMutable.remove(productoId)
        itemsCarrito = carritoMutable.toMap()
        println("🗑️ Producto eliminado del carrito. Total items: ${itemsCarrito.size}")
    }

    fun calcularSubtotal(): Double {
        return itemsCarrito.values.sumOf { it.subtotal }
    }

    fun calcularDescuentoMonto(): Double {
        return descuento.toDoubleOrNull() ?: 0.0
    }

    fun calcularTotal(): Double {
        return calcularSubtotal() - calcularDescuentoMonto()
    }

    fun limpiarCarrito() {
        itemsCarrito = emptyMap()
        clienteSeleccionado = null
        descuento = ""
        notas = ""
        cantidadProducto = "1"
        busquedaCliente = ""
        busquedaProducto = ""
        ventaParaPago = null
        detallesParaPago = emptyList()
        pagoView = null
    }

    fun obtenerVentaActual(): Venta {
        return Venta(
            clienteId = clienteSeleccionado?.id ?: 0,
            total = calcularTotal(),
            descuento = calcularDescuentoMonto(),
            impuestos = 0.0,
            pagoId = 0,
            estado = "pendiente",
            notas = notas.trim()
        )
    }

    fun obtenerDetallesVentaActual(): List<DetalleVenta> {
        return itemsCarrito.values.toList()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Render() {
        println("🎨 VentaView.Render() - Vista actual: $vistaActual")
        when (vistaActual) {
            "lista" -> {
                println("📋 Renderizando lista de ventas. Total: ${ventas.size}")
                VistaListaVentas()
            }
            "nueva_venta" -> {
                println("🆕 Renderizando nueva venta")
                VistaNuevaVenta()
            }
            "seleccion_pago" -> {
                println("💳 Renderizando selección de método de pago")
                VistaSeleccionPago()
            }
            "pago_stripe" -> {
                println("💳 Renderizando PagoView para Stripe")
                // 🆕 Mostrar PagoView en lugar de vista propia
                pagoView?.Render() ?: run {
                    // Fallback si no hay PagoView configurada
                    VistaErrorPago()
                }
            }
            else -> {
                println("⚠️ Vista desconocida: $vistaActual")
                VistaListaVentas()
            }
        }
    }

    @Composable
    private fun VistaListaVentas() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🛒 Ventas",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Button(
                    onClick = {
                        println("Botón Nueva Venta clickeado")
                        limpiarCarrito()
                        vistaActual = "nueva_venta"
                        onNuevaVentaClick()
                    }
                ) {
                    Text("➕ Nueva Venta")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (ventas.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = "No hay ventas registradas",
                        modifier = Modifier.padding(16.dp),
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ventas) { venta ->
                        VentaCard(venta = venta)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun VistaNuevaVenta() {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header con botón volver
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🛒 Nueva Venta",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Button(
                        onClick = {
                            vistaActual = "lista"
                            onVolverAListaClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("⬅️ Volver")
                    }
                }
            }

            item {
                SelectorCliente()
            }

            // Solo mostrar productos si hay cliente seleccionado
            if (clienteSeleccionado != null) {
                item {
                    SelectorProducto()
                }

                item {
                    CarritoDeCompra()
                }
            }
        }
    }

    @Composable
    private fun VistaSeleccionPago() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💳 Seleccionar Método de Pago",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Button(
                    onClick = {
                        vistaActual = "nueva_venta"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("⬅️ Volver")
                }
            }

            // Resumen de la venta
            ventaParaPago?.let { venta ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📋 Resumen de Venta",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Cliente: ${clienteSeleccionado?.nombre} ${clienteSeleccionado?.apellido}")
                        Text("Total: ${DecimalFormat("#,##0.00").format(venta.total)}")
                        if (venta.descuento > 0) {
                            Text("Descuento: -${DecimalFormat("#,##0.00").format(venta.descuento)}")
                        }
                        Text("Productos: ${detallesParaPago.size} items")
                        if (venta.notas.isNotEmpty()) {
                            Text("Notas: ${venta.notas}")
                        }
                    }
                }
            }

            // Métodos de pago disponibles
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Selecciona el método de pago:",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    metodosPago.forEach { metodo ->
                        Button(
                            onClick = {
                                ventaParaPago?.let { venta ->
                                    onSeleccionarMetodoPagoClick(venta, metodo.id)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when (metodo.id) {
                                    1 -> MaterialTheme.colorScheme.tertiary // Efectivo
                                    2 -> MaterialTheme.colorScheme.primary // Tarjeta
                                    else -> MaterialTheme.colorScheme.secondary
                                }
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = when (metodo.id) {
                                        1 -> "💵 Pago en ${metodo.nombre.uppercase()}"
                                        2 -> "💳 Pago con ${metodo.nombre.uppercase()}"
                                        else -> "💰 ${metodo.nombre.uppercase()}"
                                    },
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Información adicional
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ℹ️ Información de métodos de pago",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "• Efectivo: La venta se completará inmediatamente\n• Tarjeta: Se procesará a través de Stripe de forma segura",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }

    // 🆕 Vista de error cuando no hay PagoView configurada
    @Composable
    private fun VistaErrorPago() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error de Configuración",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "No se pudo cargar la vista de pago. Intenta nuevamente.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            vistaActual = "lista"
                            onVolverAListaClick()
                        }
                    ) {
                        Text("🏠 Volver a Lista de Ventas")
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SelectorCliente() {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "👤 Seleccionar Cliente",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Cliente seleccionado o buscador
                if (clienteSeleccionado != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${clienteSeleccionado!!.nombre} ${clienteSeleccionado!!.apellido}",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "📱 ${clienteSeleccionado!!.telefono}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }

                            TextButton(onClick = {
                                clienteSeleccionado = null
                                busquedaCliente = ""
                            }) {
                                Text("Cambiar", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = busquedaCliente,
                            onValueChange = {
                                busquedaCliente = it
                                if (it.length >= 2) {
                                    onBuscarClientesClick(it)
                                }
                            },
                            label = { Text("Buscar cliente...") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Lista de clientes encontrados
                    if (busquedaCliente.isNotEmpty() && clientesDisponibles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 150.dp)
                        ) {
                            items(clientesDisponibles.take(5)) { cliente ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    onClick = { onSeleccionarClienteClick(cliente) }
                                ) {
                                    Text(
                                        text = "${cliente.nombre} ${cliente.apellido} - ${cliente.telefono}",
                                        modifier = Modifier.padding(8.dp),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SelectorProducto() {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🌬️ Agregar Productos",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Buscador de productos
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = busquedaProducto,
                        onValueChange = {
                            busquedaProducto = it
                            if (it.length >= 2) {
                                onBuscarProductosClick(it)
                            }
                        },
                        label = { Text("Buscar producto...") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Lista de productos encontrados
                if (busquedaProducto.isNotEmpty() && productosDisponibles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        items(productosDisponibles.take(5)) { producto ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = producto.nombre,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${DecimalFormat("#,##0.00").format(producto.precio)} - Stock: ${producto.stock}",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = "${producto.nombreCategoria} - ${producto.subcategoria}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = cantidadProducto,
                                            onValueChange = { cantidadProducto = it },
                                            label = { Text("Cant.") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.width(80.dp),
                                            singleLine = true
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Button(
                                            onClick = {
                                                val cantidad = cantidadProducto.toIntOrNull() ?: 1
                                                if (cantidad > 0 && cantidad <= producto.stock) {
                                                    agregarAlCarrito(producto, cantidad)
                                                    onAgregarProductoClick(producto, cantidad)
                                                    cantidadProducto = "1"
                                                    busquedaProducto = ""
                                                }
                                            },
                                            enabled = producto.stock > 0,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Text("➕", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun CarritoDeCompra() {
        val cantidadItems = itemsCarrito.size

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🛒 Carrito de Compra ($cantidadItems)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (cantidadItems > 0) {
                        TextButton(onClick = {
                            itemsCarrito = emptyMap()
                            println("🗑️ Carrito limpiado")
                        }) {
                            Text("🗑️ Limpiar", color = Color.Red)
                        }
                    }
                }

                if (cantidadItems == 0) {
                    Text(
                        text = "El carrito está vacío",
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(itemsCarrito.values.toList()) { item ->
                            ItemCarritoCard(item = item)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = descuento,
                        onValueChange = { descuento = it },
                        label = { Text("💰 Descuento") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = notas,
                        onValueChange = { notas = it },
                        label = { Text("📝 Notas (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Subtotal:")
                                Text("$${DecimalFormat("#,##0.00").format(calcularSubtotal())}")
                            }

                            if (calcularDescuentoMonto() > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Descuento:")
                                    Text("-$${DecimalFormat("#,##0.00").format(calcularDescuentoMonto())}", color = Color.Red)
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "TOTAL:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = "$${DecimalFormat("#,##0.00").format(calcularTotal())}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val venta = obtenerVentaActual()
                            val detalles = obtenerDetallesVentaActual()
                            onConfirmarVentaClick(venta, detalles)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = cantidadItems > 0 && clienteSeleccionado != null && calcularTotal() > 0
                    ) {
                        Text(
                            text = "✅ Continuar con el Pago - $${DecimalFormat("#,##0.00").format(calcularTotal())}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ItemCarritoCard(item: DetalleVenta) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.nombreProducto,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${item.nombreCategoria} - ${item.subcategoria}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${item.cantidad} x ${DecimalFormat("#,##0.00").format(item.precioUnitario)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${DecimalFormat("#,##0.00").format(item.subtotal)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    TextButton(onClick = {
                        eliminarDelCarrito(item.productoId)
                        onEliminarItemCarritoClick(item.productoId)
                    }) {
                        Text("🗑️", color = Color.Red, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    @Composable
    private fun VentaCard(venta: Venta) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Venta #${venta.id}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Cliente: ${venta.nombreCliente} ${venta.apellidoCliente}",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )

                        Text(
                            text = "📅 ${formatearFecha(venta.fechaVenta)}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        Row {
                            Text(
                                text = "💰 ${DecimalFormat("#,##0.00").format(venta.total)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = venta.estado.uppercase(),
                                fontSize = 12.sp,
                                color = when (venta.estado) {
                                    "completada" -> Color.Green
                                    "pendiente" -> Color.Red
                                    else -> Color.Red
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Mostrar método de pago
                        Text(
                            text = "💳 ${venta.metodoPago.uppercase()}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )

                        if (venta.notas.isNotEmpty()) {
                            Text(
                                text = "📝 ${venta.notas}",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    if (venta.estado == "pendiente") {
                        Button(
                            onClick = {
                                // 🆕 Usar el callback para mostrar PagoView
                                onMostrarPagoViewClick(venta)
                            }
                        ) {
                            Text("💳 Completar Pago")
                        }
                    }
                }
            }
        }
    }

    private fun formatearFecha(fechaTimestamp: String): String {
        return try {
            // Simplificado - puedes mejorar el formateo según necesites
            fechaTimestamp.substring(0, 16).replace("T", " ")
        } catch (e: Exception) {
            fechaTimestamp
        }
    }
}