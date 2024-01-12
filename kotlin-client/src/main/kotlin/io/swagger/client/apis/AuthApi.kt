package io.swagger.client.apis

import io.swagger.client.infrastructure.*
import io.swagger.client.models.LoginResponse
import io.swagger.client.models.User

class AuthApi(basePath: String = "/") : ApiClient(basePath) {

    var authToken: String? = null
    var authUserId: Int? = 0
    var authUserType: Int? = 0
    var authStatus: Boolean? = false

    /**
     * Login
     *
     * @param email (required)
     * @param password (required)
     * @return void
     */

    fun apiAuthPost(email: String, password: String): Unit {
        val body = User(email = email, password = password)
        val localVariableBody: Any? = body
        val localVariableConfig = RequestConfig(
            RequestMethod.POST,
            "/api/Auth/login"
        )
        val headers = mutableMapOf(
            "Authorization" to "Bearer ${authToken}"
        )
        val response = request<Any?>(localVariableConfig.copy(headers = headers), localVariableBody)

        when (response.responseType) {
            ResponseType.Success -> {
                if (response is Success<*>) {

                    val loginResponse = response.data

                    if (loginResponse != null && loginResponse is Map<*, *>) {
                        authToken = loginResponse["token"]?.toString() ?: ""
                        authUserId = loginResponse["userId"].toString().toDoubleOrNull()?.toInt() ?: 0
                        authUserType = loginResponse["userType"].toString().toDoubleOrNull()?.toInt() ?: 0
                        authStatus = loginResponse["status"].toString().toBoolean()
                    }
                }
            }
            ResponseType.Informational -> TODO()
            ResponseType.Redirection -> TODO()
            ResponseType.ClientError -> {
                if (response is ClientError<*>) {
                    authToken = response.body?.toString()
                }

                throw ClientException(response.toString() ?: "Client error")
            }
            ResponseType.ServerError -> {
                if (response is ServerError<*>) {
                    throw ServerException(response.message ?: "Server error")
                }
            }
        }
    }



    /**
     * Logout
     *
     * @return void
     */
    fun apiAuthPostLogout(token: String?): Unit {


        val headers = mutableMapOf(
            "Authorization" to "Bearer ${token}"
        )

        val localVariableConfig = RequestConfig(
            RequestMethod.POST,
            "/api/Auth/logout",
            headers = headers
        )

        val response = request<Any?>(localVariableConfig)

        when (response.responseType) {
            ResponseType.Success -> Unit
            ResponseType.Informational -> TODO()
            ResponseType.Redirection -> TODO()
            ResponseType.ClientError -> throw ClientException((response as ClientError<*>).body as? String
                ?: "Client error")
            ResponseType.ServerError -> throw ServerException((response as ServerError<*>).message
                ?: "Server error")
        }
    }

}