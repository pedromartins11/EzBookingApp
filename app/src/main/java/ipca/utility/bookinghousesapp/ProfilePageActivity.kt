package ipca.utility.bookinghousesapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.android.material.bottomnavigation.BottomNavigationView
import ipca.utility.bookinghousesapp.databinding.ActivityLoginBinding
import ipca.utility.bookinghousesapp.databinding.ActivityProfilePageBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

class ProfilePageActivity : AppCompatActivity() {

    private lateinit var binding : ActivityProfilePageBinding
    private lateinit var bottomNavigationView: BottomNavigationView
    private val GALLERY_REQUEST_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfilePageBinding.inflate(layoutInflater)
        setContentView(binding.root)


        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.menu.findItem(R.id.profile).isChecked = true
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> startActivity(Intent(this, MainActivity::class.java))
                R.id.notifications -> startActivity(Intent(this, NotificationsActivity::class.java))
                R.id.profile -> startActivity(Intent(this, ProfilePageActivity::class.java))
            }
            true
        }
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val userType = sharedPreferences.getInt("user_type", 0)
        if (userType == 3) {
            binding.constraintLayoutAdminProfile.visibility = View.VISIBLE
        } else {
            binding.constraintLayoutAdminProfile.visibility = View.GONE
        }
        Backend.fetchUserDetail(this, lifecycleScope) { user ->
            user?.let {
                binding.textViewUserName.text = it.name
                binding.textViewUserEmail.text = it.email

                val imageUrl = "${Backend.BASE_API}/Users/${user.image}${user.imageFormat}"
                Glide.with(this@ProfilePageActivity)
                    .asBitmap()
                    .load(imageUrl)
                    .transition(BitmapTransitionOptions.withCrossFade())
                    .transform(CircleCrop())
                    .into(binding.imageView11)
            }
        }

        binding.imageViewBack.setOnClickListener {
            onBackPressed()
        }

        binding.constraintLayoutEditProfile.setOnClickListener{
            val intent = Intent(this,EditProfileActivity::class.java )
            startActivity(intent)
        }

        binding.constraintLayoutLogout.setOnClickListener {
            Backend.logout(this, lifecycleScope) {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
                Log.d("LogoutActivity", "Logout bem-sucedido!")
            }
        }

        binding.constraintLayoutUserHouses.setOnClickListener{
            val intent = Intent(this,UserHousesList::class.java )
            startActivity(intent)
        }

        binding.constraintLayoutCreateHouse.setOnClickListener {
            val intent = Intent(this, CreateHouse::class.java)
            startActivity(intent)
        }


        binding.constraintLayoutUserReservations.setOnClickListener{
            val intent = Intent(this,UserReservationsList::class.java )
            startActivity(intent)
        }

        binding.constraintLayoutAdminProfile.setOnClickListener{
            val intent = Intent(this,AdminProfilePage::class.java )
            startActivity(intent)
        }

        binding.imageView11.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
        }
    }




    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val selectedImageUri: Uri? = data.data

            if (selectedImageUri != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val imageBytes = getBytesFromUri(selectedImageUri)
                    val sharedPreferences =
                        getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                    val authToken = sharedPreferences.getString("access_token", null)
                    val tempImageFile = createTempImageFile(imageBytes)

                    runOnUiThread {
                        Glide.with(this@ProfilePageActivity).clear(binding.imageView11)
                    }
                    Backend.UpdateUserAvatar(authToken, tempImageFile, getFileExtensionFromUri(selectedImageUri)) { success ->
                        if (success) {


                            val intent = Intent(this@ProfilePageActivity, ProfilePageActivity::class.java)
                            startActivity(intent)
                        } else {
                            println("Erro na escolha de imagem")
                        }
                    }
                }
            }
        }
    }


    private suspend fun getBytesFromUri(uri: Uri): ByteArray {
        return withContext(Dispatchers.IO) {
            val inputStream = contentResolver.openInputStream(uri)
            val outputStream = ByteArrayOutputStream()

            inputStream?.use { input ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
            }

            outputStream.toByteArray()
        }
    }

    private fun getFileExtensionFromUri(uri: Uri): String {
        val contentResolver = contentResolver ?: return "png"
        val mimeTypeMap = MimeTypeMap.getSingleton()
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri)) ?: "png"
    }

    private fun createTempImageFile(imageBytes: ByteArray): File {
        val tempFile = File.createTempFile("temp_avatar", null, cacheDir)
        tempFile.writeBytes(imageBytes)
        return tempFile
    }
}