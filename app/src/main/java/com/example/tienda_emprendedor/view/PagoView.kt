package com.example.tienda_emprendedor.view

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tienda_emprendedor.model.Venta
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.rememberPaymentSheet
import java.text.DecimalFormat

class PagoView {

    var ventaParaPago by mutableStateOf<Venta?>(null)
    var estadoPago by mutableStateOf("inicial")
    var clientSecret by mutableStateOf("")
    var mensajeError by mutableStateOf("")
    var paymentIntentId by mutableStateOf("")

    // Callbacks
    var onProcesarPagoClick: (Venta) -> Unit = {}
    var onVolverAVentasClick: () -> Unit = {}
    var onPagoCompletadoClick: (Venta) -> Unit = {}
    var onReintentarPagoClick: (Venta) -> Unit = {}
    var onResultadoStripeCallback: ((Boolean, String?, String?) -> Unit)? = null

    @Composable
    fun Render() {
        val paymentSheet = rememberPaymentSheet { paymentResult ->
            when (paymentResult) {
                is PaymentSheetResult.Completed -> {
                    val paymentIntentId = clientSecret.split("_secret_").firstOrNull() ?: ""
                    onResultadoStripeCallback?.invoke(true, paymentIntentId, null)
                }
                is PaymentSheetResult.Canceled -> {

                    estadoPago = "cancelado"
                    onResultadoStripeCallback?.invoke(false, null, "Pago cancelado por el usuario")
                }
                is PaymentSheetResult.Failed -> {
                    val error = paymentResult.error.localizedMessage
                        ?: paymentResult.error.message
                        ?: "Error desconocido"
                    estadoPago = "error"
                    mensajeError = error
                    onResultadoStripeCallback?.invoke(false, null, error)
                }
            }
        }


        LaunchedEffect(estadoPago, clientSecret) {
            if (estadoPago == "listo_para_pagar" && clientSecret.isNotEmpty()) {
                try {

                    val configuration = PaymentSheet.Configuration.Builder("Aires Acondicionados")
                        .build()

                    paymentSheet.presentWithPaymentIntent(clientSecret, configuration)

                } catch (e: Exception) {
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
            HeaderPago()

            ventaParaPago?.let { venta ->
                InformacionVenta(venta)
            }

            ContenidoEstadoPago()
            InformacionStripe()
        }
    }

    @Composable
    private fun HeaderPago() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "💳 Pago con Tarjeta",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Button(
                onClick = onVolverAVentasClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("⬅️ Volver")
            }
        }
    }

    @Composable
    private fun InformacionVenta(venta: Venta) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📋 Resumen de Venta #${venta.id}",
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
                Text("Estado: ${venta.estado.uppercase()}")
            }
        }
    }

    @Composable
    private fun ContenidoEstadoPago() {
        when (estadoPago) {
            "inicial" -> EstadoInicial()
            "procesando" -> EstadoProcesando()
            "listo_para_pagar" -> EstadoListoPagar()
            "completado" -> EstadoCompletado()
            "error" -> EstadoError()
            "cancelado" -> EstadoCancelado()
        }
    }

    @Composable
    private fun EstadoInicial() {
        ventaParaPago?.let { venta ->
            Button(
                onClick = { onProcesarPagoClick(venta) },
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
    }

    @Composable
    private fun EstadoProcesando() {
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

    @Composable
    private fun EstadoListoPagar() {
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

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Si no aparece, verifica que no haya ventanas emergentes bloqueadas",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }

    @Composable
    private fun EstadoCompletado() {
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

                if (paymentIntentId.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ID de transacción: ${paymentIntentId.substring(0, 15)}...",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        ventaParaPago?.let { venta ->
                            onPagoCompletadoClick(venta)
                        }
                        onVolverAVentasClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("🏠 Volver a Ventas", color = Color(0xFF4CAF50))
                }
            }
        }
    }

    @Composable
    private fun EstadoError() {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Error en el Pago",
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

        Button(
            onClick = {
                ventaParaPago?.let { venta ->
                    onReintentarPagoClick(venta)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("🔄 Reintentar Pago")
        }
    }

    @Composable
    private fun EstadoCancelado() {
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
                ventaParaPago?.let { venta ->
                    onReintentarPagoClick(venta)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("🔄 Intentar Nuevamente")
        }
    }

    @Composable
    private fun InformacionStripe() {
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