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
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.rememberPaymentSheet
import java.text.DecimalFormat

class VentaView {

    // Estados para nueva venta
    var clienteSeleccionado by mutableStateOf<Cliente?>(null)
    var productosDisponibles by mutableStateOf(listOf<Producto>())
    var clientesDisponibles by mutableStateOf(listOf<Cliente>())

    // Carrito de compra
    var itemsCarrito by mutableStateOf(mutableMapOf<Int, DetalleVenta>())
    var descuento by mutableStateOf("")
    var notas by mutableStateOf("")

    // Estados de UI
    var mostrarFormularioVenta by mutableStateOf(false)
    var mostrarSelectorCliente by mutableStateOf(false)
    var mostrarSelectorProducto by mutableStateOf(false)
    var busquedaCliente by mutableStateOf("")
    var busquedaProducto by mutableStateOf("")
    var vistaActual by mutableStateOf("lista") // "lista", "nueva_venta", "pago"

    // Estados de pago
    var estadoPago by mutableStateOf("inicial") // "inicial", "procesando", "listo_para_pagar", "completado", "error", "cancelado"
    var clientSecret by mutableStateOf("")
    var mensajeError by mutableStateOf("")

    var ventas by mutableStateOf(listOf<Venta>())
    var ventaParaPago by mutableStateOf<Venta?>(null)

    // Estados para productos
    var productoSeleccionado by mutableStateOf<Producto?>(null)
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
    var onProcesarPagoStripeClick: ((Venta) -> Unit)? = null
    var onPaymentResultCallback: ((Boolean, String?, String?) -> Unit)? = null

    fun actualizarVentas(nuevasVentas: List<Venta>) {
        println("📋 VentaView: Actualizando ventas. Cantidad: ${nuevasVentas.size}")
        ventas = nuevasVentas
        nuevasVentas.forEach { venta ->
            println("  - Vista recibió venta #${venta.id}: ${venta.nombreCliente} - ${venta.total}")
        }
    }

    fun actualizarProductosDisponibles(productos: List<Producto>) {
        productosDisponibles = productos
    }

    fun actualizarClientesDisponibles(clientes: List<Cliente>) {
        clientesDisponibles = clientes
    }

    fun agregarAlCarrito(producto: Producto, cantidad: Int) {
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

        // Si ya existe el producto, sumar la cantidad
        if (itemsCarrito.containsKey(producto.id)) {
            val existente = itemsCarrito[producto.id]!!
            val nuevaCantidad = existente.cantidad + cantidad
            if (nuevaCantidad <= producto.stock) {
                existente.cantidad = nuevaCantidad
                existente.subtotal = existente.precioUnitario * nuevaCantidad
            }
        } else {
            itemsCarrito[producto.id] = detalle
        }

        // Refrescar el estado
        itemsCarrito = itemsCarrito.toMutableMap()
    }

    fun eliminarDelCarrito(productoId: Int) {
        itemsCarrito.remove(productoId)
        itemsCarrito = itemsCarrito.toMutableMap()
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
        itemsCarrito.clear()
        clienteSeleccionado = null
        descuento = ""
        notas = ""
        cantidadProducto = "1"
        busquedaCliente = ""
        busquedaProducto = ""
        productoSeleccionado = null
        estadoPago = "inicial"
        clientSecret = ""
        mensajeError = ""
    }

    fun obtenerVentaActual(): Venta {
        return Venta(
            clienteId = clienteSeleccionado?.id ?: 0,
            total = calcularTotal(),
            descuento = calcularDescuentoMonto(),
            impuestos = 0.0, // Puedes agregar lógica de impuestos aquí
            metodoPago = "pendiente",
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
            "pago" -> {
                println("💳 Renderizando vista de pago")
                VistaPago()
            }
            else -> {
                println("⚠️ Vista desconocida: $vistaActual")
                VistaListaVentas()
            }
        }
    }

    @Composable
    private fun VistaListaVentas() {
        println("📋 VistaListaVentas iniciando...")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            println("📋 Creando Row con header...")
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
                        println("🆕 Botón Nueva Venta clickeado")
                        limpiarCarrito()
                        vistaActual = "nueva_venta"
                        onNuevaVentaClick()
                    }
                ) {
                    Text("➕ Nueva Venta")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            println("📋 Verificando ventas. Total: ${ventas.size}")
            if (ventas.isEmpty()) {
                println("📋 No hay ventas, mostrando mensaje vacío")
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
                println("📋 Mostrando ${ventas.size} ventas")
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ventas) { venta ->
                        VentaCard(venta = venta)
                    }
                }
            }
        }

        println("📋 VistaListaVentas completada")
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

            // Selección de cliente
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
    private fun VistaPago() {
        // ✅ Configurar PaymentSheet de Stripe
        val paymentSheet = rememberPaymentSheet { paymentResult ->
            when (paymentResult) {
                is PaymentSheetResult.Completed -> {
                    println("✅ Pago completado exitosamente con Stripe")
                    // Extraer PaymentIntent ID del client secret
                    val paymentIntentId = clientSecret.split("_secret_").firstOrNull() ?: ""
                    onPaymentResultCallback?.invoke(true, paymentIntentId, null)
                }
                is PaymentSheetResult.Canceled -> {
                    println("❌ Pago cancelado por el usuario")
                    estadoPago = "cancelado"
                    onPaymentResultCallback?.invoke(false, null, "Pago cancelado por el usuario")
                }
                is PaymentSheetResult.Failed -> {
                    val error = paymentResult.error.localizedMessage ?: paymentResult.error.message ?: "Error desconocido"
                    println("❌ Pago falló: $error")
                    estadoPago = "error"
                    mensajeError = error
                    onPaymentResultCallback?.invoke(false, null, error)
                }
            }
        }

        // ✅ Efecto para mostrar PaymentSheet automáticamente cuando esté listo
        LaunchedEffect(estadoPago, clientSecret) {
            if (estadoPago == "listo_para_pagar" && clientSecret.isNotEmpty()) {
                try {
                    println("🎨 Mostrando PaymentSheet con clientSecret: ${clientSecret.substring(0, 20)}...")

                    val configuration = PaymentSheet.Configuration.Builder("Aires Acondicionados")
                        .build()

                    paymentSheet.presentWithPaymentIntent(clientSecret, configuration)

                } catch (e: Exception) {
                    println("❌ Error mostrando PaymentSheet: ${e.message}")
                    estadoPago = "error"
                    mensajeError = "Error mostrando el formulario de pago: ${e.message}"
                }
            }
        }

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
                    text = "💳 Procesar Pago",
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

            // Información de la venta
            ventaParaPago?.let { venta ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Resumen de Venta #${venta.id}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Cliente: ${venta.nombreCliente} ${venta.apellidoCliente}")
                        Text("Total: $${DecimalFormat("#,##0.00").format(venta.total)}")
                        if (venta.descuento > 0) {
                            Text("Descuento: -$${DecimalFormat("#,##0.00").format(venta.descuento)}")
                        }
                        if (venta.notas.isNotEmpty()) {
                            Text("Notas: ${venta.notas}")
                        }
                    }
                }

                // ✅ ESTADOS DEL PAGO
                when (estadoPago) {
                    "inicial" -> {
                        // Botón para iniciar el pago
                        Button(
                            onClick = {
                                ventaParaPago?.let { venta ->
                                    onProcesarPagoStripeClick?.invoke(venta)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = "💳 PAGAR CON STRIPE - $${DecimalFormat("#,##0.00").format(venta.total)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    "procesando" -> {
                        // Indicador de carga
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "⏳ Procesando pago...",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Preparando el formulario de pago seguro",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    "listo_para_pagar" -> {
                        // El PaymentSheet se muestra automáticamente
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🔒 Formulario de Pago Listo",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "El formulario de pago seguro debería aparecer automáticamente",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }

                    "completado" -> {
                        // Pago exitoso
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "✅ ¡Pago Completado Exitosamente!",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "La venta ha sido procesada correctamente",
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }

                    "error" -> {
                        // Error en el pago
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "❌ Error en el Pago",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                if (mensajeError.isNotEmpty()) {
                                    Text(
                                        text = mensajeError,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }

                        // Botón para reintentar
                        Button(
                            onClick = {
                                estadoPago = "inicial"
                                mensajeError = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("🔄 Reintentar Pago")
                        }
                    }

                    "cancelado" -> {
                        // Pago cancelado
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "⚠️ Pago Cancelado",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "El pago fue cancelado. Puedes intentar nuevamente.",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }

                        // Botón para reintentar
                        Button(
                            onClick = {
                                estadoPago = "inicial"
                                mensajeError = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("🔄 Intentar Nuevamente")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Información adicional sobre Stripe
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🔒 Pago Seguro con Stripe",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "• Acepta tarjetas de crédito y débito\n• Procesamiento seguro y encriptado\n• Cumple con estándares PCI DSS\n• Soporte para múltiples métodos de pago",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
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
                    // Buscador de clientes
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
                        text = "🛒 Carrito de Compra (${itemsCarrito.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (itemsCarrito.isNotEmpty()) {
                        TextButton(onClick = { itemsCarrito.clear() }) {
                            Text("🗑️ Limpiar", color = Color.Red)
                        }
                    }
                }

                if (itemsCarrito.isEmpty()) {
                    Text(
                        text = "El carrito está vacío",
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Items del carrito
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(itemsCarrito.values.toList()) { item ->
                            ItemCarritoCard(item = item)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Campo de descuento
                    OutlinedTextField(
                        value = descuento,
                        onValueChange = { descuento = it },
                        label = { Text("💰 Descuento") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Campo de notas
                    OutlinedTextField(
                        value = notas,
                        onValueChange = { notas = it },
                        label = { Text("📝 Notas (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Resumen de totales
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Subtotal:")
                                Text("${DecimalFormat("#,##0.00").format(calcularSubtotal())}")
                            }

                            if (calcularDescuentoMonto() > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Descuento:")
                                    Text("-${DecimalFormat("#,##0.00").format(calcularDescuentoMonto())}", color = Color.Red)
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
                                    text = "${DecimalFormat("#,##0.00").format(calcularTotal())}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón confirmar venta
                    Button(
                        onClick = {
                            val venta = obtenerVentaActual()
                            val detalles = obtenerDetallesVentaActual()
                            onConfirmarVentaClick(venta, detalles)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = itemsCarrito.isNotEmpty() && clienteSeleccionado != null && calcularTotal() > 0
                    ) {
                        Text(
                            text = "✅ Confirmar Venta - ${DecimalFormat("#,##0.00").format(calcularTotal())}",
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
                            onClick = { onIrAPagoClick(venta) }
                        ) {
                            Text("💳 Pagar")
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