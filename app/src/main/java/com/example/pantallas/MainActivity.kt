package com.example.pantallas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // Declaración de los elementos de la pantalla
    private lateinit var etCorreo: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnIngresar: Button

    // Objeto para trabajar con la base de datos local (Asegurando el nombre correcto de la clase)
    private lateinit var dbHelper: DataBaseHelper

    // Objetos para el funcionamiento de Google y Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private lateinit var btnGoogle: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Enlazar el texto para crear una cuenta
        val tvCrearCuenta = findViewById<TextView>(R.id.tvCrearCuenta)

        // Enlazar los campos de texto y botones
        etCorreo = findViewById(R.id.etCorreo)
        etPassword = findViewById(R.id.etPass)
        btnIngresar = findViewById(R.id.btnIngresar)
        btnGoogle = findViewById(R.id.btnGoogle)

        // Inicializar la conexión con la base de datos local
        dbHelper = DataBaseHelper(this)

        // Inicializar Firebase Auth y Credential Manager
        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        // Evento para iniciar sesión con correo y contraseña
        btnIngresar.setOnClickListener {
            iniciarSesion()
        }

        // Evento para iniciar sesión con Google
        btnGoogle.setOnClickListener {
            iniciarSesionGoogle()
        }

        // Evento para ir a la pantalla de registro
        tvCrearCuenta.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }
    }

    // Método para validar e iniciar sesión con la base de datos local
    private fun iniciarSesion() {
        val correo = etCorreo.text.toString().trim()
        val pass = etPassword.text.toString().trim()

        if (correo.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Consultar credenciales en la BD local (método correcto de tu clase DataBaseHelper)
        val resultado = dbHelper.inisiarSesion(correo, pass)

        if (resultado) {
            Toast.makeText(this, "Bienvenido", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
        }
    }

    // Método para autenticar en Firebase con Google
    private fun autenticaFirebase(idToken: String) {
        val credencialFirebase = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credencialFirebase).addOnCompleteListener(this) { inicio ->
            if (inicio.isSuccessful) {
                val usuario = auth.currentUser
                Toast.makeText(this, "Bienvenido ${usuario?.displayName}", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Error al iniciar con Firebase: ${inicio.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Método para procesar la credencial obtenida de Google
    private fun procesarCredencial(credential: Credential) {
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val credencialGoogle = GoogleIdTokenCredential.createFrom(credential.data)
                autenticaFirebase(credencialGoogle.idToken)
            } catch (e: GoogleIdTokenParsingException) {
                Toast.makeText(this, "Credencial Inválida", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Método para iniciar sesión usando Google Identity / Credential Manager
    private fun iniciarSesionGoogle() {
        val inicioGoogle = GetSignInWithGoogleOption.Builder(
            getString(R.string.default_web_client_id)
        ).build()

        val solicitud = GetCredentialRequest.Builder()
            .addCredentialOption(inicioGoogle)
            .build()

        lifecycleScope.launch {
            try {
                val resultado = credentialManager.getCredential(
                    context = this@MainActivity,
                    request = solicitud
                )
                procesarCredencial(resultado.credential)
            } catch (e: GetCredentialException) {
                // Si te vuelve a salir este mensaje, recuerda revisar la huella SHA-1 en Firebase
                Toast.makeText(this@MainActivity, "Error al iniciar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Método para mantener la sesión abierta si el usuario ya se había logueado
    override fun onStart() {
        super.onStart()
        val usuarioActual = auth.currentUser
        if (usuarioActual != null) {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}