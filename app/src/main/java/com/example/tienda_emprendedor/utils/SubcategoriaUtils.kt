package com.example.tienda_emprendedor.utils

object SubcategoriaUtils {


    val subcategoriasPorCategoria = mapOf(
        "Split" to listOf("Residencial", "Comercial", "Industrial"),
        "Ventana" to listOf("Compacto", "Estándar"),
        "Central" to listOf("Ductooooo", "Cassette", "Piso-Techo"),
        "Portátil" to listOf("Doméstico", "Oficina")
    )

    val categoriasPrincipales = listOf("Split", "Ventana", "Central", "Portátil")


    fun obtenerSubcategorias(categoria: String): List<String> {
        return subcategoriasPorCategoria[categoria] ?: emptyList()
    }

    fun obtenerTodasLasSubcategorias(): List<String> {
        return subcategoriasPorCategoria.values.flatten()
    }

    fun esSubcategoriaValida(categoria: String, subcategoria: String): Boolean {
        return subcategoriasPorCategoria[categoria]?.contains(subcategoria) ?: false
    }
}