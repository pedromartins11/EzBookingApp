package ipca.utility.bookinghousesapp

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.util.Log
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.ImageView
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import io.jsonwebtoken.io.IOException
import io.swagger.client.apis.AuthApi
import io.swagger.client.apis.FeedbackApi
import io.swagger.client.apis.HouseApi
import io.swagger.client.apis.PaymentApi
import io.swagger.client.apis.ReservationApi
import io.swagger.client.apis.UserApi
import java.time.LocalDateTime
import java.util.Date
import io.swagger.client.infrastructure.ClientException
import io.swagger.client.infrastructure.ServerException
import io.swagger.client.infrastructure.ApiClient
import io.swagger.client.infrastructure.RequestConfig
import io.swagger.client.infrastructure.RequestMethod
import io.swagger.client.models.EditProfile
import io.swagger.client.models.Feedback
import io.swagger.client.models.Payment
import io.swagger.client.models.Reservation
import ipca.utility.bookinghousesapp.Backend.AUTHENTICATION_API
import ipca.utility.bookinghousesapp.Backend.BASE_API
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.File

import java.util.Objects

sealed class ResultWrapper<out T> {
    data class Success<out T>(val value: T) : ResultWrapper<T>()
    data class Error(val code: Int? = null, val error: String? = null) :
        ResultWrapper<Nothing>()

    object NetworkError : ResultWrapper<Nothing>()

    inline fun onSuccess(action: (value: T) -> Unit): ResultWrapper<T> {
        if (this is Success) action(value)
        return this
    }

    inline fun onError(action: (error: Error) -> Unit): ResultWrapper<T> {
        if (this is Error) action(this)
        return this
    }

    inline fun onNetworkError(action: () -> Unit): ResultWrapper<T> {
        if (this is NetworkError) action()
        return this
    }
}

object Backend {

    internal const val BASE_API = "https://bookapih.azurewebsites.net/"
    internal const val AUTHENTICATION_API = "https://authenticationezbooking20231222152833.azurewebsites.net/"
    internal const val NOTIFICATION_API = "https://fcm.googleapis.com/fcm/send"
    private val client = OkHttpClient()
    //private const val PATH_HOUSES = "House"

    suspend fun <T> wrap(apiCall: suspend () -> T): ResultWrapper<T> {
        return try {
            ResultWrapper.Success(apiCall())
        } catch (throwable: Throwable) {
            Log.e("Repository", throwable.toString())
            when (throwable) {
                is IOException -> ResultWrapper.NetworkError
                else -> {
                    ResultWrapper.Error(0, throwable.message)
                }
            }
        }
    }

    fun fetchHouseDetail(idhouse: Int): LiveData<ResultWrapper<io.swagger.client.models.House>> =
    liveData(Dispatchers.IO) {
        emit( wrap { HouseApi(BASE_API).apiHouseIdGet(idhouse) })
    }

    fun fetchAllHouses(): LiveData<ResultWrapper<Array<io.swagger.client.models.House>>> =
        liveData(Dispatchers.IO) {
            emit( wrap { HouseApi(BASE_API).apiHouseGet() })
        }

    fun filterHouses(location: String?,guestsNumber: Int?,checkedV: Boolean?, startDate: LocalDateTime?,endDate: LocalDateTime?):
            LiveData<ResultWrapper<Array<io.swagger.client.models.House>>> =
        liveData(Dispatchers.IO) {
            emit( wrap { HouseApi(BASE_API).apiHouseFilteredGet(location,guestsNumber,checkedV,startDate,endDate) })
        }

    fun CreateReservation(reservation: io.swagger.client.models.Reservation?,houseId: Int,userId: Int,context: Context,):
            LiveData<ResultWrapper<Unit>> =
        liveData(Dispatchers.IO) {
            val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val authToken = sharedPreferences.getString("access_token", null)
            emit( wrap { ReservationApi(BASE_API).apiReservationPost(reservation, houseId,userId,authToken) })
        }
    @SuppressLint("SuspiciousIndentation")
    fun CreateUser(user: io.swagger.client.models.User?,context: Context):
        LiveData<ResultWrapper<Unit>> =
            liveData(Dispatchers.IO) {
                emit( wrap { UserApi(BASE_API).apiUserPost(user?.name.toString(), user?.email.toString(), user?.password.toString(), user?.phone!! ) })
    }

    fun fetchReservationPayment(userId: Int,context: Context): LiveData<ResultWrapper<io.swagger.client.models.Reservation>> =
        liveData(Dispatchers.IO) {
            val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val authToken = sharedPreferences.getString("access_token", null)
            emit( wrap { ReservationApi(BASE_API).paymentGet(userId,authToken) })
        }

    fun sendNotitication(token: String, notificationTitle: String, notificationBody: String,lifecycleScope: LifecycleCoroutineScope,  callback: (Boolean) -> Unit
    ){
    lifecycleScope.launch(Dispatchers.IO) {
            val mediaTypeJson = "application/json; charset=utf-8".toMediaType()
            val okHttpClient = OkHttpClient()

            val body = """
            {
                "to": "$token",
                "notification": {
                    "body": "$notificationBody",
                    "title": "$notificationTitle"
                }
            }
        """.trimIndent()

            val requestBody = body.toRequestBody(mediaTypeJson)
            val request = Request.Builder()
                .post(requestBody)
                .url("${NOTIFICATION_API}")
                .addHeader("Authorization", "key=AAAAAaa-bNg:APA91bEoqBLabyCti11X_Wf9AfQy6Im3QsoH98W2cnNx0DT_ucS83HqZi5q1YQW5bbZQXqhSwJt39aHtwsf1B_lcAHz5QxaAoLA1tZ1dnvLVZo63hOYuOS9HRJxCcPyjw-rEopT-ka8t")
                .build()

            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: java.io.IOException) {
                    // Handle this
                    Log.d("shoppinglist", e.message.toString())
                }

                override fun onResponse(call: Call, response: Response) {
                    // Handle this
                    Log.d("shoppinglist", response.toString())
                }
            })
        }}

    @SuppressLint("SuspiciousIndentation")
    fun UpdateUserAvatar(token: String?, imageFile: File, fileExtension: String, callback: (Boolean) -> Unit) {
        try {
            val mediaType = "multipart/form-data".toMediaTypeOrNull()
            val imageName = "avatar_${System.currentTimeMillis()}"

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "ImageFile",
                    "$imageName.$fileExtension",
                    imageFile.asRequestBody(mediaType)
                )
                .build()

            val request = Request.Builder()
                .url(BASE_API + "/api/User/avatar")
                .header("Authorization", "Bearer $token")
                .put(requestBody)
                .build()

            val client = OkHttpClient()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                callback(true)
            } else {
                callback(false)
            }
        } catch (e: IOException) {
            callback(false)
        }
    }

    private fun getFileExtensionFromFileName(fileName: String): String {
        val lastDotIndex = fileName.lastIndexOf(".")
        return if (lastDotIndex != -1) {
            fileName.substring(lastDotIndex + 1)
        } else {
            "png"
        }
    }

    @SuppressLint("SuspiciousIndentation")
    fun CreateHouseImage(token: String?, idHouse : Int, imageFiles: ArrayList<File>, callback: (Boolean) -> Unit) {
        try {
            val mediaType = "multipart/form-data".toMediaTypeOrNull()

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("id_house", idHouse.toString())

            for (imageFile in imageFiles) {
                val extension = getFileExtensionFromFileName(imageFile.name)
                val fileName = imageFile.nameWithoutExtension

                requestBody.addFormDataPart(
                    "imageFiles",
                    "$fileName.$extension",
                    imageFile.asRequestBody(mediaType)
                )
            }
            val finalRequestBody = requestBody.build()

            val request = Request.Builder()
                .url(BASE_API + "/api/Image/$idHouse")
                .header("Authorization", "Bearer $token")
                .post(finalRequestBody)
                .build()

            val client = OkHttpClient()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                callback(true)
            } else {
                callback(false)
            }
        } catch (e: IOException) {
            callback(false)
        }
    }

    @SuppressLint("SuspiciousIndentation")
    fun EditHouseImage(token: String?, idHouse : Int, imageFiles: ArrayList<File>, callback: (Boolean) -> Unit) {
        try {
            val mediaType = "multipart/form-data".toMediaTypeOrNull()

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("update", idHouse.toString())

            for ( imageFile in imageFiles) {
                val extension = getFileExtensionFromFileName(imageFile.name)
                val fileName = imageFile.nameWithoutExtension

                requestBody.addFormDataPart(
                    "imageFiles",
                    "$fileName.$extension",
                    imageFile.asRequestBody(mediaType)
                )
            }
            val finalRequestBody = requestBody.build()

            val request = Request.Builder()
                .url(BASE_API + "/api/Image/update/$idHouse")
                .header("Authorization", "Bearer $token")
                .put(finalRequestBody)
                .build()

            val client = OkHttpClient()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                callback(true)
            } else {
                Log.e("Erro na escolha de imagem", "Código de status: ${response.code}")
                callback(false)
            }
        } catch (e: IOException) {
            Log.e("Erro na escolha de imagem", "Exceção durante a chamada à API", e)
            callback(false)
        }
    }



    @SuppressLint("SuspiciousIndentation")
    fun login(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        email: String,
        password: String,
        callback: (Boolean) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val authApi = AuthApi("${AUTHENTICATION_API}")
                authApi.apiAuthPost(email, password)
                var token = authApi.authToken
                var userId = authApi.authUserId
                var userType = authApi.authUserType
                var userStatus = authApi.authStatus

                val sharedPreferences =
                    context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putString("access_token", token)
                editor.putInt("user_id", userId ?: -1)
                editor.putInt("user_type", userType ?: -1)
                editor.putString("password", password)

                editor.putBoolean("user_status", userStatus ?: false)
                editor.apply()

                lifecycleScope.launch(Dispatchers.Main) {
                    callback(true)
                }
            } catch (e: ClientException) {
                Log.e("LoginActivity", "Client error on login: ${e.message}")
                lifecycleScope.launch(Dispatchers.Main) {
                    callback(false)
                }
            } catch (e: ServerException) {
                Log.e("LoginActivity", "Server error on login: ${e.message}")
                lifecycleScope.launch(Dispatchers.Main) {
                    callback(false)
                }
            }
        }
    }

    @SuppressLint("SuspiciousIndentation")
    fun logout(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        callback: () -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                val authToken = sharedPreferences.getString("access_token", null)
                if (authToken != null) {
                    AuthApi("${AUTHENTICATION_API}").apiAuthPostLogout(authToken)
                }

                lifecycleScope.launch(Dispatchers.Main) {
                    callback()
                }
            } catch (e: Exception) {
                Log.e("LogoutActivity", "Error during logout: ${e.message}")
                lifecycleScope.launch(Dispatchers.Main) {
                    callback()
                }
            }
        }
    }



    @SuppressLint("SuspiciousIndentation")
    fun CreateFeedback(
        lifecycleScope:
        LifecycleCoroutineScope,
        newClassification: Int,
        newComment: String,
        newReservation : Int,
        callback: (Boolean) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {


            FeedbackApi("${BASE_API}").apiFeedbackPost(Feedback(comment = newComment, classification = newClassification),newReservation)
            lifecycleScope.launch(Dispatchers.Main) {
                callback(true)
            }


        }
    }


    @SuppressLint("SuspiciousIndentation")
    fun UpdateUser(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        name: String,
        email: String,
        phone: Int,
        password: String,
        token: String,
        staus: Boolean,
        callback: (Boolean) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val userId = sharedPreferences.getInt("user_id", 0)
            val authToken = sharedPreferences.getString("access_token", null)
            UserApi("${BASE_API}").apiUserUserIdPut(authToken,userId)

            lifecycleScope.launch(Dispatchers.Main) {
                callback(true)
            }

        }
    }

    @SuppressLint("SuspiciousIndentation")
    fun UpdateUserProfile(context: Context,newUserName: String,newUserEmail: String,newUserPhone: Int):
            LiveData<ResultWrapper<Unit>> =
        liveData(Dispatchers.IO) {
            val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val authToken = sharedPreferences.getString("access_token", "")
            val userId = sharedPreferences.getInt("user_id", 0)
            emit( wrap { UserApi("${BASE_API}").apiUserUserIdProfilePut(authToken, userId, EditProfile(newUserName, newUserEmail, newUserPhone)) })
        }

    @SuppressLint("SuspiciousIndentation")
    fun CancelReservation(
        context : Context,
        lifecycleScope: LifecycleCoroutineScope,
        reservationId: Int,
        callback: (Boolean) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val authToken = sharedPreferences.getString("access_token", "")
            ReservationApi("${BASE_API}").apiReservationReservationIdDeactivatePut(authToken, reservationId)

            lifecycleScope.launch(Dispatchers.Main) {
                callback(true)
            }
        }
    }


    @SuppressLint("SuspiciousIndentation")
    fun GetAllUsers(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        callback: (Array<io.swagger.client.models.User>) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val authToken = sharedPreferences.getString("access_token", "")
            val userApi = UserApi("${BASE_API}").apiUserGet(authToken)
            lifecycleScope.launch(Dispatchers.Main) {
                callback(userApi)
            }

        }
    }

    @SuppressLint("SuspiciousIndentation")
    fun GetLastPayment(
        lifecycleScope: LifecycleCoroutineScope,
        userId: Int,
        callback: (Payment) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val paymentApi = PaymentApi("${BASE_API}").paymentConfirmGet(userId)
            lifecycleScope.launch(Dispatchers.Main) {
                callback(paymentApi)
            }

        }
    }

    @SuppressLint("SuspiciousIndentation")
    suspend fun GetLastHouse(userId: Int): io.swagger.client.models.House {
        return withContext(Dispatchers.IO) {
            HouseApi("${BASE_API}").GetLastHouse(userId)
        }
    }

    @SuppressLint("SuspiciousIndentation")
    fun UpdatePayment(
        paymentId : Int,
        lifecycleScope: LifecycleCoroutineScope,

        callback: (Boolean) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            PaymentApi("${BASE_API}").apiPaymentPaymentIdStatePut(paymentId)

            lifecycleScope.launch(Dispatchers.Main) {
                callback(true)
            }

        }
    }

    @SuppressLint("SuspiciousIndentation")
    fun GetAllPayments(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        callback: (Array<io.swagger.client.models.Payment>) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val authToken = sharedPreferences.getString("access_token", "")
            val paymentApi = PaymentApi("${BASE_API}").apiPaymentGet(authToken)
            lifecycleScope.launch(Dispatchers.Main) {
                callback(paymentApi)
            }

        }
    }

    @SuppressLint("SuspiciousIndentation")
    fun GetHouseReservations(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        houseId: Int,
        callback: (Array<io.swagger.client.models.Reservation>) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val sharedPreferences =
                context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val authToken = sharedPreferences.getString("access_token", null)

            val houseApi = HouseApi("${BASE_API}").apiHouseReservationsIdGet(authToken, houseId)


            lifecycleScope.launch(Dispatchers.Main) {
                callback(houseApi)
            }

        }
    }

    @SuppressLint("SuspiciousIndentation")
    fun fetchUserDetail(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        callback: (io.swagger.client.models.User) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val userId = sharedPreferences.getInt("user_id", 0)
            val authToken = sharedPreferences.getString("access_token", null)
            val userApi = UserApi("${BASE_API}").apiUserUserIdGet(authToken, userId)
            lifecycleScope.launch(Dispatchers.Main) {
                callback(userApi)
            }

        }
    }

    @SuppressLint("SuspiciousIndentation")
    fun GetReservationFeedbacks(
        lifecycleScope: LifecycleCoroutineScope,
        reservationId: Int,
        callback: (Boolean) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val feedbackApi = FeedbackApi("${BASE_API}").apiFeedbackReservationIdFeedbacksGet(reservationId)
            lifecycleScope.launch(Dispatchers.Main) {
                callback(feedbackApi)
            }

        }
    }


    fun fetchAllHousesSusp(lifecycleScope: LifecycleCoroutineScope, callback: (Array<io.swagger.client.models.House>) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val housesApi: Array<io.swagger.client.models.House> = HouseApi("${BASE_API}").apiHouseSuspGet()

                lifecycleScope.launch(Dispatchers.Main) {
                    if (housesApi.isEmpty()) {
                        println("Nenhuma casa encontrada")
                    } else {
                        callback(housesApi)

                        housesApi.forEach { house ->
                            println("House ID: ${house.id_house}")
                        }
                    }
                }
            } catch (e: io.swagger.client.infrastructure.ClientException) {
                println("Erro ao buscar casas: ${e.message}")
            }
        }
    }


    fun updateHouseStateApproved(id: Int, lifecycleScope: LifecycleCoroutineScope, callback: () -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            HouseApi("${BASE_API}").apiHouseStateIdPut(id)
            lifecycleScope.launch(Dispatchers.Main) {
                callback()
            }

        }
    }


    fun deleteHouseById(id: Int, lifecycleScope: LifecycleCoroutineScope, callback: () -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                HouseApi("${BASE_API}").apiHouseIdDelete(id)
                lifecycleScope.launch(Dispatchers.Main) {
                    callback()
                }
            } catch (e: Exception) {

                println("Erro ao apagar a casa: ${e.message}")
            }
        }
    }

    fun fetchUserDetail(
        context : Context,
        lifecycleScope: LifecycleCoroutineScope,
        id_user: Int,
        callback: (io.swagger.client.models.User) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val authToken = sharedPreferences.getString("access_token", "")
            val userApi = UserApi("${BASE_API}").apiUserUserIdGet(authToken, id_user)
            lifecycleScope.launch(Dispatchers.Main) {
                println("USER Backend:")
                println(id_user)
                println(userApi)
                println(userApi.id_user)
                callback(userApi)
            }
        }
    }

    @SuppressLint("SuspiciousIndentation")
    fun GetAllReservations(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        callback: (Array<io.swagger.client.models.Reservation>) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val sharedPreferences =
                context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val authToken = sharedPreferences.getString("access_token", null)
            val reservationsApi = ReservationApi("${BASE_API}").apiReservationGet(authToken)


            lifecycleScope.launch(Dispatchers.Main) {
                callback(reservationsApi)
            }

        }
    }

    @SuppressLint("SuspiciousIndentation")
    fun GetAllHouses(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        callback: (Array<io.swagger.client.models.House>) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val sharedPreferences =
                context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val authToken = sharedPreferences.getString("access_token", null)
            val housesApi = HouseApi("${BASE_API}").apiHouseAllGet(authToken)


            lifecycleScope.launch(Dispatchers.Main) {
                callback(housesApi)
            }

        }
    }

    @SuppressLint("SuspiciousIndentation")
    fun GetUserReservations(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        callback: (Array<io.swagger.client.models.Reservation>) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val sharedPreferences =
                context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val authId = sharedPreferences.getInt("user_id", 0)
            val authToken = sharedPreferences.getString("access_token", null)

            val reservationApi = UserApi("${BASE_API}").apiUserReservationsIdGet(authToken, authId)


            lifecycleScope.launch(Dispatchers.Main) {
                callback(reservationApi)
            }

        }
    }

    @SuppressLint("SuspiciousIndentation")
    fun GetUserHouses(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        callback: (Array<io.swagger.client.models.House>) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val sharedPreferences =
                context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val authId = sharedPreferences.getInt("user_id", 0)
            val authToken = sharedPreferences.getString("access_token", null)
            println(authId)
            val houseApi = UserApi("${BASE_API}").apiUserHousesIdGet(authToken, authId)


            lifecycleScope.launch(Dispatchers.Main) {
                callback(houseApi)
            }

        }
    }

    @SuppressLint("SuspiciousIndentation")
    fun DeleteHouse(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        houseId: Int,
        callback: (Boolean) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val sharedPreferences =
                context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val authToken = sharedPreferences.getString("access_token", null)
            HouseApi("${BASE_API}").apiHouseStateDeleteIdPut(houseId)


            lifecycleScope.launch(Dispatchers.Main) {
                callback(true)
            }

        }
    }

    @SuppressLint("SuspiciousIndentation")
    fun DeleteUser(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        userId: Int,
        callback: (Boolean) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val sharedPreferences =
                context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val authToken = sharedPreferences.getString("access_token", null)
            UserApi("${BASE_API}").apiUserUserIdDelete(userId)


            lifecycleScope.launch(Dispatchers.Main) {
                callback(true)
            }

        }
    }

    @SuppressLint("SuspiciousIndentation")
    fun DeactivateUser(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        userId: Int,
        callback: (Boolean) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val sharedPreferences =
                context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val authToken = sharedPreferences.getString("access_token", null)
            UserApi("${BASE_API}").apiUserUserIdDeactivatePut(authToken, userId)


            lifecycleScope.launch(Dispatchers.Main) {
                callback(true)
            }

        }
    }

    fun editHouse(id: Int, house: io.swagger.client.models.House): ResultWrapper<Unit> {
        return try {

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    HouseApi(BASE_API).apiHouseIdPut(id, house)
                } catch (e: Exception) {

                    println("Erro ao Editar a casa: $e")
                }
            }
            ResultWrapper.Success(Unit)
        } catch (e: Exception) {

            println("Erro ao Editar a casa: $e")
            ResultWrapper.Error()
        }
    }


    @SuppressLint("SuspiciousIndentation")
    fun CreateHouse(body: io.swagger.client.models.House,context: Context):
            LiveData<ResultWrapper<Unit>> =
        liveData(Dispatchers.IO) {
            val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val token = sharedPreferences.getString("access_token", "")

            emit( wrap {
                HouseApi(BASE_API).apiHousePost(token, body)
            })
        }


}




