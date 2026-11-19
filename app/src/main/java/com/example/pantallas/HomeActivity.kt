package com.example.pantallas

import android.content.Intent
import androidx.credentials.CredentialManager
import androidx.credentials.ClearCredentialStateRequest
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.pantallas.model.Libro
import com.example.pantallas.model.LibroResponse
import com.example.pantallas.network.RetrofitClient
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeActivity : AppCompatActivity() {

    private lateinit var tvResultado: TextView
    private lateinit var contenedorRecomendaciones: LinearLayout
    private lateinit var contenedorContinuar: LinearLayout
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var btnComedia: Button
    private lateinit var btnTerror: Button
    private lateinit var btnRomance: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        // Conectar vistas del XML
        bottomNavigation = findViewById(R.id.bottomNavigation)
        tvResultado = findViewById(R.id.tvResultado)
        contenedorRecomendaciones = findViewById(R.id.contenedorRecomendaciones)
        contenedorContinuar = findViewById(R.id.contenedorContinuar)

        // Conectar botones de géneros
        btnComedia = findViewById(R.id.btnComedia)
        btnTerror = findViewById(R.id.btnTerror)
        btnRomance = findViewById(R.id.btnRomance)

        // Inicializar autenticación y gestor de credenciales
        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        // Configurar el menú de navegación inferior
        menuNavegacion()

        // Cargar libros iniciales al abrir la actividad
        obtenerLibros("fiction")

        // Eventos de clic para los botones de categorías
        btnComedia.setOnClickListener { obtenerLibros("comedy") }
        btnTerror.setOnClickListener { obtenerLibros("horror") }
        btnRomance.setOnClickListener { obtenerLibros("romance") }

        // Configurar insets para diseño edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
    }

    // Método para obtener libros desde la API (Open Library)
    private fun obtenerLibros(busqueda: String) {
        RetrofitClient.api.obtenerLibros(busqueda = busqueda, limite = 10).enqueue(object : Callback<LibroResponse> {
            override fun onResponse(call: Call<LibroResponse>, response: Response<LibroResponse>) {
                val libros = response.body()?.docs ?: emptyList()

                if (response.isSuccessful && libros.isNotEmpty()) {
                    tvResultado.visibility = View.GONE
                    mostrarLibros(libros, contenedorRecomendaciones)
                    mostrarLibros(libros.take(3), contenedorContinuar)
                } else {
                    tvResultado.visibility = View.VISIBLE
                    tvResultado.text = "No se encontraron libros"
                    Toast.makeText(this@HomeActivity, "No se encontraron libros", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LibroResponse>, t: Throwable) {
                Toast.makeText(this@HomeActivity, "Error en el API: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    // Método para renderizar la lista de libros en el contenedor correspondiente
    private fun mostrarLibros(libros: List<Libro>, contenedor: LinearLayout) {
        contenedor.removeAllViews()

        for (libro in libros) {
            val vistaLibro = layoutInflater.inflate(R.layout.item_book, contenedor, false)

            val imgPortada = vistaLibro.findViewById<ImageView>(R.id.imgPortadaLibro)
            val tvNombreLibro = vistaLibro.findViewById<TextView>(R.id.tvNombreLibro)
            val tvNombreAutor = vistaLibro.findViewById<TextView>(R.id.tvNombreAutor)

            tvNombreLibro.text = libro.title ?: "Libro sin título"
            tvNombreAutor.text = libro.authorName?.joinToString(", ") ?: "Autor desconocido"

            Glide.with(this@HomeActivity)
                .load(libro.obtenerImagenGrande())
                .error(R.color.black)
                .into(imgPortada)

            vistaLibro.setOnClickListener {
                val intent = Intent(this@HomeActivity, DetailsActivity::class.java).apply {
                    putExtra("ID_LIBRO", libro.key ?: "")
                    putExtra("TITULO_LIBRO", libro.title ?: "Libro sin título")
                    putExtra("AUTOR_LIBRO", libro.authorName?.joinToString(", ") ?: "Desconocido")
                    putExtra("ANIO_LIBRO", libro.firstPublishYear ?: 0)
                    putExtra("IMAGEN_LIBRO", libro.obtenerImagenGrande())
                }
                startActivity(intent)
            }

            contenedor.addView(vistaLibro)
        }
    }

    // Método para la lógica del menú de navegación inferior
    private fun menuNavegacion() {
        bottomNavigation.selectedItemId = R.id.nav_inicio
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    true
                }
                R.id.navFavoritos -> {
                    val intent = Intent(this, FavoritesActivity::class.java)
                    startActivity(intent)
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

    // MÉTODO PARA CERRAR SESIÓN
    private fun cerrarSesionGoogle() {
        auth.signOut()

        lifecycleScope.launch {
            try {
                credentialManager.clearCredentialState(
                    ClearCredentialStateRequest()
                )
            } catch (e: Exception) {
                Toast.makeText(this@HomeActivity, "Error al cerrar sesión", Toast.LENGTH_SHORT).show()
            }

            val intent = Intent(this@HomeActivity, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }
    }
}