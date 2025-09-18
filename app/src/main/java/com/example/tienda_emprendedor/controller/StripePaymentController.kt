package com.example.tienda_emprendedor.controller

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.rememberPaymentSheet
import com.example.tienda_emprendedor.service.StripeService
import com.example.tienda_emprendedor.model.Venta
import com.example.tienda_emprendedor.model.Pago
import com.example.tienda_emprendedor.model.PagoDao

class StripePaymentController(private val activity: ComponentActivity) {

    private val stripeService = StripeService()
    private val pagoDao = PagoDao()
    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Procesa un pago con Stripe PaymentSheet
     */
    @Composable
    fun ProcesarPago(
        venta: Venta,
        onPaymentSuccess: (String) -> Unit, // paymentIntentId
        onPaymentError: (String) -> Unit,
        onPaymentCanceled: () -> Unit
    ) {
        var paymentIntentClientSecret by remember { mutableStateOf<String?>(null) }
        var isLoading by remember { mutableStateOf(false) }

        // Configurar PaymentSheet
        val paymentSheet = rememberPaymentSheet { paymentResult ->
            when (paymentResult) {
                is PaymentSheetResult.Completed -> {
                    println("✅ Pago completado exitosamente")
                    // Guardar el pago en la base de datos
                    scope.launch {
                        guardarPagoExitoso(venta, paymentIntentClientSecret ?: "")
                    }
                    onPaymentSuccess(paymentIntentClientSecret ?: "")
                }
                is PaymentSheetResult.Canceled -> {
                    println("❌ Pago cancelado por el usuario")
                    onPaymentCanceled()
                }
                is PaymentSheetResult.Failed -> {
                    val error = paymentResult.error.localizedMessage
                        ?: paymentResult.error.message
                        ?: "Error desconocido"
                    println("❌ Pago falló: $error")
                    onPaymentError(error)
                }
            }
        }

        // Crear PaymentIntent cuando se monta el composable
        LaunchedEffect(venta.id) {
            if (paymentIntentClientSecret == null && !isLoading) {
                isLoading = true
                crearPaymentIntent(venta) { result ->
                    isLoading = false
                    result.onSuccess { clientSecret ->
                        paymentIntentClientSecret = clientSecret
                        // Mostrar automáticamente el PaymentSheet
                        mostrarPaymentSheet(paymentSheet, clientSecret)
                    }.onFailure { error ->
                        onPaymentError(error.localizedMessage ?: error.message ?: "Error al crear PaymentIntent")
                    }
                }
            }
        }
    }

    private fun crearPaymentIntent(venta: Venta, callback: (Result<String>) -> Unit) {
        scope.launch {
            try {
                println("💳 Creando PaymentIntent para venta #${venta.id}")
                println("💰 Monto: $${venta.total}")

                val amountInCents = stripeService.dollarsToCents(venta.total)
                val description = "Venta #${venta.id} - ${venta.nombreCliente} ${venta.apellidoCliente}"

                val result = stripeService.createPaymentIntent(
                    amount = amountInCents,
                    currency = "usd",
                    description = description
                )

                callback(result)
            } catch (e: Exception) {
                println("❌ Error creando PaymentIntent: ${e.message}")
                callback(Result.failure(e))
            }
        }
    }

    private fun mostrarPaymentSheet(
        paymentSheet: PaymentSheet,
        clientSecret: String
    ) {
        try {
            val configuration = PaymentSheet.Configuration.Builder("Aires Acondicionados")
                .build()

            println("🎨 Mostrando PaymentSheet...")
            paymentSheet.presentWithPaymentIntent(clientSecret, configuration)
        } catch (e: Exception) {
            println("❌ Error mostrando PaymentSheet: ${e.message}")
        }
    }

    private suspend fun guardarPagoExitoso(venta: Venta, paymentIntentId: String) {
        try {
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
            } else {
                println("❌ Error al registrar pago en BD")
            }
        } catch (e: Exception) {
            println("❌ Error guardando pago: ${e.message}")
            e.printStackTrace()
        }
    }
}