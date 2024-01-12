package ipca.utility.bookinghousesapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.lifecycle.lifecycleScope
import ipca.utility.bookinghousesapp.databinding.ActivityHouseImagesBinding
import ipca.utility.bookinghousesapp.databinding.ActivityLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.ArrayList

class HouseImagesActivity : AppCompatActivity() {

    private lateinit var binding : ActivityHouseImagesBinding
    private val GALLERY_REQUEST_CODE = 123
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHouseImagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonChooseImages.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
        }

        val houseName = intent.extras?.getString("HOUSE_NAME") ?: ""
        binding.textViewHouseName.text = houseName
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                val authToken = sharedPreferences.getString("access_token", null)
                val userId = sharedPreferences.getInt("user_id", 0)

                try {
                    val house = Backend.GetLastHouse(userId)
                    val houseId = house.id_house

                    val clipData = data.clipData
                    if (clipData != null) {
                        val tempImageFilesList: ArrayList<File> = ArrayList()

                        for (i in 0 until clipData.itemCount) {
                            val selectedImageUri: Uri? = clipData.getItemAt(i).uri
                            if (selectedImageUri != null) {
                                val imageBytes = getBytesFromUri(selectedImageUri)
                                val tempImageFile = createTempImageFile(imageBytes, selectedImageUri)
                                tempImageFilesList.add(tempImageFile)
                            }
                        }

                        Backend.CreateHouseImage(authToken, houseId!!, tempImageFilesList) { success ->
                            if (success) {
                                val intent = Intent(this@HouseImagesActivity, UserHousesList::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                println("Erro na escolha de imagem")
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
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
        val extension = mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri))
        return extension ?: "png"
    }


    private fun createTempImageFile(imageBytes: ByteArray, uri: Uri): File {
        val fileExtension = getFileExtensionFromUri(uri)
        val originalFileName = getOriginalFileNameWithoutExtension(uri)

        val tempDir = File(cacheDir, "temp_images")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }

        val tempFile = File(tempDir, "$originalFileName.$fileExtension")
        tempFile.writeBytes(imageBytes)

        return tempFile
    }

    private fun getOriginalFileNameWithoutExtension(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)

        if (cursor != null && nameIndex != null) {
            cursor.moveToFirst()
            val originalFileName = cursor.getString(nameIndex)
            cursor.close()

            return originalFileName?.substringBeforeLast('.') ?: "temp_image"
        }

        return "temp_image"
    }
}