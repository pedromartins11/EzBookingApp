package ipca.utility.bookinghousesapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import ipca.utility.bookinghousesapp.MainActivity

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        Log.d(TAG, "From: ${remoteMessage.from}")

        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")

            if (needsToBeScheduled()) {

            } else {

                sendNotification(remoteMessage.data.toString())
            }
        }

        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            saveNotificationToStorage(remoteMessage)
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(baseContext, it.body, Toast.LENGTH_LONG).show()
            }
        }

    }

    private fun saveNotificationToStorage(remoteMessage: RemoteMessage) {

        val sharedPreferences = applicationContext.getSharedPreferences("notifications", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val notificationTitle = remoteMessage.notification?.title ?: ""
        val notificationBody = remoteMessage.notification?.body ?: ""
        val notification = "$notificationTitle - $notificationBody"
        val notificationCount = sharedPreferences.getInt("notification_count", 0) + 1

        editor.putString("notification_$notificationCount", notification)
        editor.putInt("notification_count", notificationCount)
        editor.apply()

    }

    private fun needsToBeScheduled() = true


    override fun onNewToken(token: String) {

        sendRegistrationToServer(token)
    }



    private fun sendRegistrationToServer(token: String?) {
        // TODO: Implement this method to send token to your app server.
        Log.d(TAG, "sendRegistrationTokenToServer($token)")
    }

    private fun sendNotification(messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val requestCode = 0
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE,
        )

        val channelId = "fcm_default_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ezhouselogo)
            .setContentTitle("FCM Message")
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = 0
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }

}