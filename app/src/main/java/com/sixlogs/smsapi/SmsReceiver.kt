package com.sixlogs.smsapi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

import android.R
import android.app.Notification
import android.telephony.SmsMessage
import java.lang.Exception


class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent) {
        try {
            val smsExtra = intent.extras!!["pdus"] as Array<Any>?
            var body = ""
            for (i in smsExtra!!.indices) {
                val sms: SmsMessage = SmsMessage.createFromPdu(smsExtra[i] as ByteArray)
                body += sms.getMessageBody()
            }
            val notification: Notification = Notification.Builder(context)
                .setContentText(body)
                .setContentTitle("New Message")
                .setStyle(Notification.BigTextStyle().bigText(body))
                .build()
            val notificationManagerCompat = NotificationManagerCompat.from(context!!)
            notificationManagerCompat.notify(1, notification)
        }catch (ex : Exception){
            ex.printStackTrace()
        }

    }
}