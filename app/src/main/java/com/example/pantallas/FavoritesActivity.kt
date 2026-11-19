
package com.example.pantallas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.pantallas.model.LibroFavorito
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class FavoritesActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var credentialManager: CredentialManager
    private lateinit var auth: FirebaseAuth
    private lateinit var gridFavoritos: GridLayout
    private lateinit var tvEstadoFavoritos: TextView
    private lateinit var dbHelper: DataBaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_favorites)

        bottomNavigation = findViewById(R.id.bottomNavigation)
        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        gridFavoritos = findViewById(R.id.gridFavoritos)
        tvEstadoFavoritos = findViewById(R.id.tvEstadoFavoritos)
        dbHelper = DataBaseHelper(this)

        menuNavegacion()
        consultaFavoritos()
    }

    // ACTUALIZAR LOS FAVORITOS AL REGRESAR A LA PANTALLA
    override fun onResume() {
        super.onResume()
        if (::dbHelper.isInitialized) {
            consultaFavoritos()
        }
    }

    // METODO PARA CONFIGURAR EL MENU DE NAVEGACION
    private fun menuNavegacion() {

        bottomNavigation.selectedItemId = R.id.navFavoritos

        bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_inicio -> {
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    true
                }

                R.id.navFavoritos -> {
                    true
                }

                R.id.nav_Salir -> {
                    cerrarSesionGoogle()
                    true
                }

                else -> false
            }
        }
    }

    // METODO PARA CERRAR SESION
    private fun cerrarSesionGoogle() {

        auth.signOut()

        lifecycleScope.launch {

            try {
                credentialManager.clearCredentialState(
                    ClearCredentialStateRequest()
                )
            } catch (e: Exception) {
                Toast.makeText(
                    this@FavoritesActivity,
                    "Error al cerrar sesión",
                    Toast.LENGTH_SHORT
                ).show()
            }

            val intent = Intent(
                this@FavoritesActivity,
                MainActivity::class.java
            )

            // INICIAR LA APLICACION DESDE CERO
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
        }
    }

    // METODO PARA MOSTRAR LOS LIBROS FAVORITOS
    private fun mostrarLibrosFavoritos(favoritos: List<LibroFavorito>) {

        for (libro in favoritos) {

            val vistaLibro = layoutInflater.inflate(
                R.layout.item_favoritos,
                gridFavoritos,
                false
            )

            val imgLibro = vistaLibro.findViewById<ImageView>(
                R.id.imgFavorito
            )

            // CARGAR LA PORTADA DEL LIBRO
            Glide.with(this)
                .load(libro.imagen)
                .centerCrop()
                .into(imgLibro)

            // EVENTO PARA ABRIR LOS DETALLES DEL LIBRO
            vistaLibro.setOnClickListener {

                val intent = Intent(
                    this,
                    DetailsActivity::class.java
                )

                intent.putExtra("ID_LIBRO", libro.libroId)
                intent.putExtra("TITULO_LIBRO", libro.titulo)
                intent.putExtra("AUTOR_LIBRO", libro.autor)
                intent.putExtra("ANIO_LIBRO", libro.anio)
                intent.putExtra("IMAGEN_LIBRO", libro.imagen)

                startActivity(intent)
            }

            gridFavoritos.addView(vistaLibro)
        }
    }

    // METODO PARA CONSULTAR LOS LIBROS EN LA BASE DE DATOS
    private fun consultaFavoritos() {

        val favoritos = dbHelper.obtenerLibrosFavoritos()

        // LIMPIAR EL GRID ANTES DE MOSTRAR LOS LIBROS
        gridFavoritos.removeAllViews()

        // VERIFICAR SI EXISTEN LIBROS FAVORITOS
        if (favoritos.isEmpty()) {

            tvEstadoFavoritos.visibility = View.VISIBLE

            tvEstadoFavoritos.text =
                "No tienes ningún libro en favoritos :("

        } else {

            tvEstadoFavoritos.visibility = View.GONE

            mostrarLibrosFavoritos(favoritos)
        }
    }
}
