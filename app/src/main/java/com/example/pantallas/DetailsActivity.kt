package com.example.pantallas

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class DetailsActivity : AppCompatActivity() {

    private lateinit var imgPortadaDetalle: ImageView
    private lateinit var btnFavorito: ImageButton
    private lateinit var tvTituloLibroDetalle: TextView
    private lateinit var tvAutorLibroDetalle: TextView
    private lateinit var tvDetallesLibro: TextView

    private lateinit var db: DataBaseHelper

    private var libroFavorito = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_details)

        // Enlazar vistas del XML
        imgPortadaDetalle = findViewById(R.id.imgPortadaDetalle)
        btnFavorito = findViewById(R.id.btnFavorito)
        tvTituloLibroDetalle = findViewById(R.id.tvTituloLibroDetalle)
        tvAutorLibroDetalle = findViewById(R.id.tvAutorLibroDetalle)
        tvDetallesLibro = findViewById(R.id.tvDetallesLibro)

        db = DataBaseHelper(this)

        mostrarInformacionLibro()
    }

    // Método para mostrar la información del libro y manejar favoritos
    private fun mostrarInformacionLibro() {

        val idLibro = intent.getStringExtra("ID_LIBRO") ?: ""
        val titulo = intent.getStringExtra("TITULO_LIBRO") ?: "Libro sin título"
        val autor = intent.getStringExtra("AUTOR_LIBRO") ?: "Autor desconocido"
        val anio = intent.getIntExtra("ANIO_LIBRO", 0)
        val imagen = intent.getStringExtra("IMAGEN_LIBRO") ?: ""

        tvTituloLibroDetalle.text = titulo
        tvAutorLibroDetalle.text = autor
        tvDetallesLibro.text = if (anio != 0) {
            "Año de publicación: $anio"
        } else {
            "Año de publicación desconocido"
        }

        Glide.with(this)
            .load(imagen)
            .into(imgPortadaDetalle)

        // Verificar si el libro ya existe en la base de datos de favoritos
        if (idLibro.isNotEmpty()) {
            libroFavorito = db.existeLibroFavorito(idLibro)
        }

        // Establecer el ícono correcto al iniciar la pantalla
        actualizarIconoFavorito()

        // Evento de clic en el botón de favoritos
        btnFavorito.setOnClickListener {

            if (idLibro.isEmpty()) {
                Toast.makeText(this, "No se puede guardar este libro", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (libroFavorito) {
                // Si ya es favorito, lo eliminamos
                val eliminado = db.eliminarLibroFavorito(idLibro)
                if (eliminado) {
                    libroFavorito = false
                    Toast.makeText(this, "Libro $titulo eliminado de favoritos", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error al eliminar de favoritos", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Si no es favorito, lo guardamos
                val guardado = db.guardarLibroFavorito(
                    libroId = idLibro,
                    titulo = titulo,
                    autor = autor,
                    anio = anio,
                    imagen = imagen
                )

                if (guardado) {
                    libroFavorito = true
                    Toast.makeText(this, "Libro $titulo agregado a favoritos", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "El libro ya se encuentra en favoritos", Toast.LENGTH_SHORT).show()
                }
            }

            // Actualizar visualmente el ícono del corazón
            actualizarIconoFavorito()
        }
    }

    // Método para actualizar dinámicamente el icono del botón de favorito
    private fun actualizarIconoFavorito() {
        if (libroFavorito) {
            btnFavorito.setImageResource(R.drawable.baseline_favorite_24)
        } else {
            btnFavorito.setImageResource(R.drawable.outline_favorite_24)
        }
    }
}