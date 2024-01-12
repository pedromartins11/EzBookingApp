package ipca.utility.bookinghousesapp

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import android.util.Log
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.Manifest
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import io.swagger.client.apis.AuthApi
import ipca.utility.bookinghousesapp.databinding.ActivityHousedetailBinding
import ipca.utility.bookinghousesapp.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding : ActivityLoginBinding
    private var isPasswordVisible = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.imageViewShowPassword.setOnClickListener{
            isPasswordVisible = !isPasswordVisible

            val editTextPassword: EditText = binding.editTextPassword

            if (isPasswordVisible) {
                editTextPassword.transformationMethod = null
                binding.imageViewShowPassword.setImageResource(R.drawable.icons8_blind_52)
            } else {
                editTextPassword.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
                binding.imageViewShowPassword.setImageResource(R.drawable.icons8_eye_52)
            }

            editTextPassword.setSelection(editTextPassword.text.length)
        }

        binding.buttonLogin.setOnClickListener{
            val email = binding.editTextEmail.text.toString()
            val password = binding.editTextPassword.text.toString()

            askNotificationPermission()
            Backend.login(this, lifecycleScope, email, password) { loginSuccessful ->
                if (loginSuccessful) {

                    val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                    val userId = sharedPreferences.getInt("user_id", 0)
                    val userStatus = sharedPreferences.getBoolean("user_status", false)

                    if(!userStatus){
                        binding.textViewError.text = "O utilizador está desativado, contacte o suporte"
                        return@login
                    }
                    val intent = Intent(this, MainActivity::class.java )
                    startActivity(intent)


                    FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            Log.w("Teste", "Fetching FCM registration token failed", task.exception)
                            return@OnCompleteListener
                        }
                        val token = task.result

                        val db = FirebaseFirestore.getInstance()
                        val tokenData = hashMapOf("token" to token)
                        db.collection("tokens").document(userId.toString())
                            .set(tokenData)
                            .addOnSuccessListener {
                                Log.d("Teste", "Token salvo no Firestore com ID: ${userId}")
                            }
                            .addOnFailureListener { e ->
                                Log.w("Teste", "Erro ao salvar token no Firestore", e)
                            }
                    })
                } else {
                    binding.textViewError.text = "Credenciais inválidas. Tente novamente."
                }
            }
        }

        binding.textViewRegister.setOnClickListener{
            val intent = Intent(this, RegisterActivity::class.java )
            startActivity(intent)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
        } else {

        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {


            }
        }
    }
}