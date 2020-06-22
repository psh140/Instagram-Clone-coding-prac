package com.hugh.hughsargram.navigation.util

import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.hugh.hughsargram.navigation.model.PushDTO
import okhttp3.*
import java.io.IOException


class FcmPush {
    var JSON = MediaType.parse("application/json; charset=utf-8")
    var url = "https://fcm.googleapis.com/fcm/send"
    var serverKey = "AAAANKszKXc:APA91bEaqJvBfQQqegfjCqMCwKcr4q4Jw7pcQrO0Z5-I9d0sw5zuwmsumO0QAWVx02bwZnZknVcGEPY8wCi_1DLsj_uDUv1nzcHfjcjKdeCl-YfX5455HiX0Tvw9CEQZVuAw-SxnX8_l"
    var gson : Gson? = null
    var okHttpClient : OkHttpClient? = null

    companion object {
        var instance = FcmPush()
    }

    init {
        gson = Gson()
        okHttpClient = OkHttpClient()
    }
    fun sendMessage(destinationUid: String, title: String, message: String) {
        FirebaseFirestore.getInstance().collection("pushtokens").document(destinationUid).get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                var token = task?.result?.get("pushToken")?.toString()

                var pushDTO = PushDTO()
                pushDTO.to = token
                pushDTO.notification.title = title
                pushDTO.notification.body = message

                var body = RequestBody.create(JSON, gson?.toJson(pushDTO))
                var request = Request.Builder()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "key=" + serverKey)
                    .url(url)
                    .post(body)
                    .build()

                okHttpClient?.newCall(request)?.enqueue(object : Callback {
                    override fun onFailure(call: Call?, e: IOException?) {

                    }

                    override fun onResponse(call: Call?, response: Response?) {
                        println(response?.body()?.string())
                    }

                })
            }
        }
    }
}


//push를 전송하는 클래스
//안드로이드 폰에 서버를 개발하는 것
//push를 보낼 값을