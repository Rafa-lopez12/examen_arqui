package com.example.tienda_emprendedor.controller

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.tienda_emprendedor.model.Cliente
import com.example.tienda_emprendedor.model.ClienteDao
import com.example.tienda_emprendedor.view.ClienteView

class ClienteController {

    private val modelo: ClienteDao = ClienteDao()
    private val vista: ClienteView = ClienteView()
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        configurarEventosVista()
        cargarClientesDesdeModelo()
    }

    private fun configurarEventosVista() {
        vista.onAgregarClick = {
            agregarCliente()
        }

        vista.onEditarClick = { cliente ->
            editarCliente(cliente)
        }

        vista.onGuardarEdicionClick = {
            guardarEdicionCliente()
        }

        vista.onEliminarClick = { cliente ->
            eliminarCliente(cliente)
        }

        vista.onConfirmarEliminacionClick = { cliente ->
            confirmarEliminacionCliente(cliente)
        }

        vista.onBuscarClick = { busqueda ->
            buscarClientes(busqueda)
        }

        vista.onLimpiarBusquedaClick = {
            cargarClientesDesdeModelo()
        }
    }

    private fun agregarCliente() {
        if (vista.validarFormulario()) {
            val cliente = vista.obtenerClienteActual()

            scope.launch {
                // Verificar que el DNI no exista
                val dniExiste = modelo.verificarDniExistente(cliente.dni)
                if (dniExiste) {
                    vista.errorDni = "Este DNI ya está registrado"
                    println("❌ Error: DNI duplicado")
                    return@launch
                }

                val exito = modelo.insertarCliente(cliente)
                if (exito) {
                    vista.limpiarFormulario()
                    vista.mostrarFormulario = false
                    cargarClientesDesdeModelo()
                    println("✅ Cliente registrado: ${cliente.nombre} ${cliente.apellido}")
                } else {
                    println("❌ Error al registrar cliente")
                }
            }
        } else {
            println("❌ Formulario con errores de validación")
        }
    }

    private fun editarCliente(cliente: Cliente) {
        vista.cargarClienteParaEdicion(cliente)
        println("✏️ Editando cliente: ${cliente.nombre} ${cliente.apellido}")
    }

    private fun guardarEdicionCliente() {
        if (vista.validarFormulario()) {
            val cliente = vista.obtenerClienteActual()

            scope.launch {
                // Verificar que el DNI no exista (excepto para el cliente actual)
                val dniExiste = modelo.verificarDniExistente(cliente.dni, cliente.id)
                if (dniExiste) {
                    vista.errorDni = "Este DNI ya está registrado por otro cliente"
                    println("❌ Error: DNI duplicado")
                    return@launch
                }

                val exito = modelo.actualizarCliente(cliente)
                if (exito) {
                    vista.limpiarFormulario()
                    vista.mostrarFormulario = false
                    cargarClientesDesdeModelo()
                    println("✅ Cliente actualizado: ${cliente.nombre} ${cliente.apellido}")
                } else {
                    println("❌ Error al actualizar cliente")
                }
            }
        } else {
            println("❌ Formulario con errores de validación")
        }
    }

    private fun eliminarCliente(cliente: Cliente) {
        // Esto solo activa el diálogo de confirmación en la vista
        println("⚠️ Solicitando confirmación para eliminar cliente: ${cliente.nombre} ${cliente.apellido}")
    }

    private fun confirmarEliminacionCliente(cliente: Cliente) {
        scope.launch {
            // Verificar si el cliente tiene ventas asociadas
            val ventasCliente = modelo.contarVentasCliente(cliente.id)

            if (ventasCliente > 0) {
                println("⚠️ No se puede eliminar. El cliente ${cliente.nombre} ${cliente.apellido} tiene $ventasCliente ventas asociadas")
                // TODO: Mostrar mensaje al usuario
            } else {
                val exito = modelo.eliminarCliente(cliente.id)
                if (exito) {
                    cargarClientesDesdeModelo()
                    println("✅ Cliente eliminado (desactivado): ${cliente.nombre} ${cliente.apellido}")
                } else {
                    println("❌ Error al eliminar cliente")
                }
            }
        }
    }

    private fun buscarClientes(busqueda: String) {
        if (busqueda.trim().isEmpty()) {
            cargarClientesDesdeModelo()
            return
        }

        scope.launch {
            val clientesEncontrados = modelo.buscarClientesPorNombre(busqueda.trim())
            vista.actualizarClientes(clientesEncontrados)
            println("🔍 Búsqueda '$busqueda': ${clientesEncontrados.size} resultados")
        }
    }

    private fun cargarClientesDesdeModelo() {
        scope.launch {
            val clientes = modelo.obtenerTodosLosClientes()
            vista.actualizarClientes(clientes)
            println("👥 Clientes cargados: ${clientes.size}")
        }
    }

    // Método para obtener un cliente específico (útil para ventas)
    fun obtenerClientePorId(id: Int, callback: (Cliente?) -> Unit) {
        scope.launch {
            val cliente = modelo.obtenerClientePorId(id)
            callback(cliente)
        }
    }

    // Método para buscar clientes (útil para autocompletar en ventas)
    fun buscarClientesParaSeleccion(busqueda: String, callback: (List<Cliente>) -> Unit) {
        scope.launch {
            val clientes = if (busqueda.trim().isEmpty()) {
                modelo.obtenerTodosLosClientes()
            } else {
                modelo.buscarClientesPorNombre(busqueda.trim())
            }
            callback(clientes)
        }
    }

    // Método para validar si un DNI está disponible
    fun validarDniDisponible(dni: String, idActual: Int = 0, callback: (Boolean) -> Unit) {
        scope.launch {
            val existe = modelo.verificarDniExistente(dni, idActual)
            callback(!existe) // Retorna true si está disponible
        }
    }

    fun obtenerVista(): ClienteView {
        return vista
    }

    fun obtenerModelo(): ClienteDao {
        return modelo
    }

    // Método útil para obtener la lista actual de clientes
    fun obtenerClientesDisponibles(): List<Cliente> {
        return vista.clientes
    }

    // Método para recargar los datos
    fun recargarDatos() {
        cargarClientesDesdeModelo()
    }
}