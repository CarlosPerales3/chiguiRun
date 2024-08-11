package com.perales.chiguirun

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.properties.Delegates
import com.facebook.FacebookSdk;




class LoginActivity : AppCompatActivity() {

        companion object{
            lateinit var userEmail: String
            lateinit var providerSession: String
        }

        private var email by Delegates.notNull<String>()
        private var password by Delegates.notNull<String>()
        private lateinit var etEmail: EditText
        private lateinit var etPassword: EditText
        private lateinit var lyTerms: LinearLayout
        private lateinit var etVerifyPassword : EditText

        private lateinit var mAuth: FirebaseAuth

        private var RESULT_CODE_GOOGLE_SIGN_IN = 100

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_login)

            lyTerms = findViewById(R.id.lyTerms)
            lyTerms.visibility = View.INVISIBLE

            etEmail = findViewById(R.id.etEmail)
            etPassword = findViewById(R.id.etPassword)
            etVerifyPassword = findViewById(R.id.etVerifyPassword)
            etVerifyPassword.visibility = View.GONE
            mAuth = FirebaseAuth.getInstance()

            manageButtonLogin()

            etEmail.doOnTextChanged { text, start, before, count -> manageButtonLogin() }
            etPassword.doOnTextChanged { text, start, before, count -> manageButtonLogin() }


        }

        public override fun onStart() {
            super.onStart()
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) goHome(currentUser.email.toString(), currentUser.providerId)
        }

        override fun onBackPressed() {
        super.onBackPressed()
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(startMain)
         }

        private fun manageButtonLogin(){
            var tvLogin = findViewById<TextView>(R.id.tvLogin)
            email = etEmail.text.toString()
            password = etPassword.text.toString()

            if (!ValidatePassword.isPassword(password) || ValidateEmail.isEmail(email) == false){

                tvLogin.setBackgroundColor(ContextCompat.getColor(this, R.color.gray))
                tvLogin.isEnabled = false
            }
            else {
                tvLogin.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
                tvLogin.isEnabled = true
            }
        }

        fun login(view: View){
            loginUser()
        }

        private fun verifyPassword(a:String,b:String):Boolean {

            return if (!TextUtils.isEmpty(a) || !TextUtils.isEmpty(b)){
                if (a == b) true
                else
                {
                    Toast.makeText(this,"La contraseña no coincide",Toast.LENGTH_SHORT).show()
                    false
                }
            }
            else {
                Toast.makeText(this,"Rellena todos los campos",Toast.LENGTH_SHORT).show()
                false
            }

        }

        private fun loginUser() {
            email = etEmail.text.toString()
            password = etPassword.text.toString()
            var verifyPassword = etVerifyPassword.text.toString()
            var verificacion by Delegates.notNull<Boolean>()


            mAuth.signInWithEmailAndPassword(email,password)
                .addOnCompleteListener(this){task->
                    if (task.isSuccessful) goHome(email,"email")
                    else {
                        if ( etVerifyPassword.visibility == View.GONE) etVerifyPassword.visibility = View.VISIBLE
                        verificacion = verifyPassword(password,verifyPassword)

                        if (lyTerms.visibility == View.INVISIBLE) lyTerms.visibility = View.VISIBLE
                        else{
                            var cbAcept = findViewById<CheckBox>(R.id.cbAcept)
                            if (cbAcept.isChecked && verificacion) register()
                        }
                    }
                }

        }

        private fun goHome(email: String, provider: String){
            userEmail = email
            providerSession = provider

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        private fun register(){
            email = etEmail.text.toString()
            password = etPassword.text.toString()

            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    if (it.isSuccessful){

                        var dateRegister = SimpleDateFormat("dd/MM/yyyy").format(Date())
                        var dbRegister = FirebaseFirestore.getInstance()
                        dbRegister.collection("users").document(email).set(hashMapOf(
                            "user" to email,
                            "dateRegister" to dateRegister
                        ))

                        goHome(email, "email")
                    }
                    else Toast.makeText(this, "Error, algo ha salido mal :(", Toast.LENGTH_SHORT).show()
                }

        }

        fun goTerms(v: View){
            val intent = Intent(this, TermsActivity::class.java)
            startActivity(intent)
        }

        fun forgotPassWord(view: View){
            //startActivity(Intent(this,ForgotPasswordActivity::class.java))
            resetPassword()
        }

        private fun resetPassword(){
            var e = etEmail.text.toString()
            if (!TextUtils.isEmpty(e)){
                mAuth.sendPasswordResetEmail(e)
                    .addOnCompleteListener{ task ->
                        if (task.isSuccessful) Toast.makeText(this, "Email enviado a $e", Toast.LENGTH_SHORT).show()
                        else Toast.makeText(this, "No se encontró el usuario con este correo", Toast.LENGTH_SHORT).show()
                    }
            }
            else Toast.makeText(this, "Indica un email", Toast.LENGTH_SHORT).show()
        }

        fun callSignInGoogle(view: View) {
            if (lyTerms.visibility == View.INVISIBLE) {
                lyTerms.visibility = View.VISIBLE
                //showDialogInLogIn()
                Toast.makeText(this, "Aceptar los términos y reintentar", Toast.LENGTH_LONG).show()
            } else {
                var cbAcept = findViewById<CheckBox>(R.id.cbAcept)
                if (cbAcept.isChecked)
                    signInGoogle()
            }
        }


        fun signInGoogle(){
            // Configure Google Sign In
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("345379380854-ad6pjgij09t87glc66kf2epejoc0bjtg.apps.googleusercontent.com")
                .requestEmail()
                .build()

            var googleSignInClient = GoogleSignIn.getClient(this, gso)
            googleSignInClient.signOut()

            startActivityForResult(googleSignInClient.signInIntent, RESULT_CODE_GOOGLE_SIGN_IN)
        }

        fun showError() {
            val errorMessage = "El registro en Google tuvo un error... Vuelve a intentar o inicia sesión ingresando tus datos."
            // Aquí puedes mostrar el mensaje en la interfaz de usuario (por ejemplo, en un TextView o un Snackbar).
            println(errorMessage) // Para este ejemplo, lo imprimo en la consola.
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)

            // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
            if (requestCode == RESULT_CODE_GOOGLE_SIGN_IN) {

                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                    // Google Sign In was successful, authenticate with Firebase
                    val account = task.getResult(ApiException::class.java)!!

                    if (account != null){
                        email = account.email!!
                        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                        mAuth.signInWithCredential(credential).addOnCompleteListener{
                            if (it.isSuccessful) {
                                var dateRegister = SimpleDateFormat("dd/MM/yyyy").format(Date())
                                var dbRegister = FirebaseFirestore.getInstance()
                                dbRegister.collection("users").document(email).set(hashMapOf(
                                    "user" to email,
                                    "dateRegister" to dateRegister,
                                    "provider" to "Google"
                                ))
                                goHome(email, "Google")
                            }
                            else showError()
                        }
                    }


                } catch (e: ApiException) {
                    Toast.makeText(this, "Error en la conexión con Google", Toast.LENGTH_SHORT)
                }
            }

        }

    }
