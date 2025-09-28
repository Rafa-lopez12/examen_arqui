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
import com.example.tienda_emprendedor.model.Cliente
import java.text.SimpleDateFormat
import java.util.*

class ClienteView {

    // Estados del formulario
    var nombre by mutableStateOf("")
    var apellido by mutableStateOf("")
    var telefono by mutableStateOf("")
    var email by mutableStateOf("")
    var direccion by mutableStateOf("")
    var dni by mutableStateOf("")
    var busqueda by mutableStateOf("")

    // Estados de la UI
    var clientes by mutableStateOf(listOf<Cliente>())
    var mostrarFormulario by mutableStateOf(false)
    var clienteEnEdicion by mutableStateOf<Cliente?>(null)
    var mostrarDialogoConfirmacion by mutableStateOf(false)
    var clienteAEliminar by mutableStateOf<Cliente?>(null)

    // Estados de validación
    var errorNombre by mutableStateOf("")
    var errorApellido by mutableStateOf("")
    var errorDni by mutableStateOf("")
    var errorTelefono by mutableStateOf("")
    var errorEmail by mutableStateOf("")

    // Callbacks
    var onAgregarClick: () -> Unit = {}
    var onEditarClick: (Cliente) -> Unit = {}
    var onGuardarEdicionClick: () -> Unit = {}
    var onEliminarClick: (Cliente) -> Unit = {}
    var onConfirmarEliminacionClick: (Cliente) -> Unit = {}
    var onBuscarClick: (String) -> Unit = {}
    var onLimpiarBusquedaClick: () -> Unit = {}

    fun actualizarClientes(nuevosClientes: List<Cliente>) {
        clientes = nuevosClientes
    }

    fun limpiarFormulario() {
        nombre = ""
        apellido = ""
        telefono = ""
        email = ""
        direccion = ""
        dni = ""
        limpiarErrores()
        clienteEnEdicion = null
    }

    fun cargarClienteParaEdicion(cliente: Cliente) {
        clienteEnEdicion = cliente
        nombre = cliente.nombre
        apellido = cliente.apellido
        telefono = cliente.telefono
        email = cliente.email
        direccion = cliente.direccion
        dni = cliente.dni
        mostrarFormulario = true
        limpiarErrores()
    }

    private fun limpiarErrores() {
        errorNombre = ""
        errorApellido = ""
        errorDni = ""
        errorTelefono = ""
        errorEmail = ""
    }

    fun validarFormulario(): Boolean {
        limpiarErrores()
        var esValido = true

        if (nombre.trim().isEmpty()) {
            errorNombre = "El nombre es obligatorio"
            esValido = false
        }

        if (apellido.trim().isEmpty()) {
            errorApellido = "El apellido es obligatorio"
            esValido = false
        }

        if (dni.trim().isEmpty()) {
            errorDni = "El DNI/CI es obligatorio"
            esValido = false
        } else if (dni.trim().length < 6) {
            errorDni = "El DNI/CI debe tener al menos 6 caracteres"
            esValido = false
        }

        if (telefono.isNotEmpty() && telefono.length < 8) {
            errorTelefono = "El teléfono debe tener al menos 8 dígitos"
            esValido = false
        }

        if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorEmail = "Formato de email inválido"
            esValido = false
        }

        return esValido
    }

    fun obtenerClienteActual(): Cliente {
        val fechaActual = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        return Cliente(
            id = clienteEnEdicion?.id ?: 0,
            nombre = nombre.trim(),
            apellido = apellido.trim(),
            telefono = telefono.trim(),
            email = email.trim(),
            direccion = direccion.trim(),
            dni = dni.trim(),
            fechaRegistro = clienteEnEdicion?.fechaRegistro ?: fechaActual,
            activo = true
        )
    }

    @Composable
    fun Render() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "👥 Gestión de Clientes",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Barra de búsqueda y botones de acción
            BarraBusquedaYAcciones()

            Spacer(modifier = Modifier.height(16.dp))

            // Formulario (si está visible)
            if (mostrarFormulario) {
                FormularioCliente()
                Spacer(modifier = Modifier.height(16.dp))
            }

            ListaClientes()
        }

        if (mostrarDialogoConfirmacion && clienteAEliminar != null) {
            ConfirmacionEliminarDialog(
                cliente = clienteAEliminar!!,
                onConfirmar = {
                    onConfirmarEliminacionClick(clienteAEliminar!!)
                    mostrarDialogoConfirmacion = false
                    clienteAEliminar = null
                },
                onCancelar = {
                    mostrarDialogoConfirmacion = false
                    clienteAEliminar = null
                }
            )
        }
    }

    @Composable
    private fun BarraBusquedaYAcciones() {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Barra de búsqueda
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = busqueda,
                        onValueChange = { busqueda = it },
                        label = { Text("🔍 Buscar cliente...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(onClick = { onBuscarClick(busqueda) }) {
                        Text("Buscar")
                    }

                    if (busqueda.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(onClick = {
                            busqueda = ""
                            onLimpiarBusquedaClick()
                        }) {
                            Text("Limpiar")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botón para agregar cliente
                Button(
                    onClick = {
                        if (mostrarFormulario) {
                            mostrarFormulario = false
                            limpiarFormulario()
                        } else {
                            limpiarFormulario()
                            mostrarFormulario = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (mostrarFormulario) "❌ Cancelar" else "➕ Nuevo Cliente")
                }
            }
        }
    }

    @Composable
    private fun FormularioCliente() {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (clienteEnEdicion != null) "✏️ Editar Cliente" else "👤 Nuevo Cliente",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Primera fila: Nombre y Apellido
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = nombre,
                            onValueChange = {
                                nombre = it
                                if (errorNombre.isNotEmpty()) errorNombre = ""
                            },
                            label = { Text("* Nombre") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = errorNombre.isNotEmpty(),
                            singleLine = true
                        )
                        if (errorNombre.isNotEmpty()) {
                            Text(
                                text = errorNombre,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = apellido,
                            onValueChange = {
                                apellido = it
                                if (errorApellido.isNotEmpty()) errorApellido = ""
                            },
                            label = { Text("* Apellido") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = errorApellido.isNotEmpty(),
                            singleLine = true
                        )
                        if (errorApellido.isNotEmpty()) {
                            Text(
                                text = errorApellido,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Segunda fila: DNI y Teléfono
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = dni,
                            onValueChange = {
                                dni = it
                                if (errorDni.isNotEmpty()) errorDni = ""
                            },
                            label = { Text("* DNI/CI") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = errorDni.isNotEmpty(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        if (errorDni.isNotEmpty()) {
                            Text(
                                text = errorDni,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = telefono,
                            onValueChange = {
                                telefono = it
                                if (errorTelefono.isNotEmpty()) errorTelefono = ""
                            },
                            label = { Text("📱 Teléfono") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = errorTelefono.isNotEmpty(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                        if (errorTelefono.isNotEmpty()) {
                            Text(
                                text = errorTelefono,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Email
                Column {
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            if (errorEmail.isNotEmpty()) errorEmail = ""
                        },
                        label = { Text("📧 Email") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = errorEmail.isNotEmpty(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    if (errorEmail.isNotEmpty()) {
                        Text(
                            text = errorEmail,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dirección
                OutlinedTextField(
                    value = direccion,
                    onValueChange = { direccion = it },
                    label = { Text("🏠 Dirección") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Botón de guardar
                Button(
                    onClick = {
                        if (clienteEnEdicion != null) {
                            onGuardarEdicionClick()
                        } else {
                            onAgregarClick()
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    enabled = nombre.isNotEmpty() && apellido.isNotEmpty() && dni.isNotEmpty()
                ) {
                    Text(
                        if (clienteEnEdicion != null) "💾 Guardar Cambios" else "👤 Registrar Cliente"
                    )
                }

                // Nota sobre campos obligatorios
                Text(
                    text = "* Campos obligatorios",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp)
                )
            }
        }
    }

    @Composable
    private fun ListaClientes() {
        Text(
            text = "Lista de Clientes (${clientes.size})",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (clientes.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = "No hay clientes registrados",
                    modifier = Modifier.padding(16.dp),
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(clientes) { cliente ->
                    ClienteCard(cliente = cliente)
                }
            }
        }
    }

    @Composable
    private fun ClienteCard(cliente: Cliente) {
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
                            text = "${cliente.apellido}, ${cliente.nombre}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row {
                            Text(
                                text = "📱 ${cliente.telefono}",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            if (cliente.email.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "📧 ${cliente.email}",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Text(
                            text = "🆔 DNI: ${cliente.dni}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (cliente.direccion.isNotEmpty()) {
                            Text(
                                text = "🏠 ${cliente.direccion}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                        // Fecha de registro
                        if (cliente.fechaRegistro.isNotEmpty()) {
                            Text(
                                text = "📅 Registrado: ${formatearFecha(cliente.fechaRegistro)}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Column {
                        TextButton(onClick = { onEditarClick(cliente) }) {
                            Text("✏️ Editar")
                        }
                        TextButton(onClick = {
                            clienteAEliminar = cliente
                            mostrarDialogoConfirmacion = true
                        }) {
                            Text("🗑️ Eliminar", color = Color.Red)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ConfirmacionEliminarDialog(
        cliente: Cliente,
        onConfirmar: () -> Unit,
        onCancelar: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onCancelar,
            title = { Text("⚠️ Confirmar Eliminación") },
            text = {
                Text("¿Estás seguro de que quieres eliminar al cliente \"${cliente.nombre} ${cliente.apellido}\"?\n\nEsta acción desactivará el cliente en el sistema.")
            },
            confirmButton = {
                Button(
                    onClick = onConfirmar,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelar) {
                    Text("Cancelar")
                }
            }
        )
    }

    private fun formatearFecha(fecha: String): String {
        return try {
            // Intentar parsear la fecha en formato yyyy-MM-dd
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = parser.parse(fecha)
            date?.let { formatter.format(it) } ?: fecha
        } catch (e: Exception) {
            fecha
        }
    }
}