package ipca.utility.bookinghousesapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.swagger.client.models.User
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import ipca.utility.bookinghousesapp.databinding.ActivityLoginBinding
import ipca.utility.bookinghousesapp.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding : ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonRegister.setOnClickListener{
            val editTextName = binding.editTextName.text.toString()
            val editTextEmail = binding.editTextEmail.text.toString()
            val editTextPassword = binding.editTextPassword.text.toString()
            val editTextPhone = binding.editTextPhone.text.toString().toInt()

            var user = User(name = editTextName, email = editTextEmail, password = editTextPassword, phone = editTextPhone)

            Backend.CreateUser(user, this).observe(this){
                it.onError {error ->
                    Toast.makeText(
                        this@RegisterActivity,
                        "${error.error}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                it.onNetworkError {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Sem Ligação à Internet",
                        Toast.LENGTH_LONG
                    ).show()
                }
                it.onSuccess {
                    val intent = Intent(this,LoginActivity::class.java)
                    startActivity(intent)
                }
            }
        }

        binding.textViewLogin.setOnClickListener{
            val intent = Intent(this,LoginActivity::class.java )
            startActivity(intent)
        }
    }
}