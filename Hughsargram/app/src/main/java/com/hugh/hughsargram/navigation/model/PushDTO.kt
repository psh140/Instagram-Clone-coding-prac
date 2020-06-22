package com.hugh.hughsargram.navigation.model

data class PushDTO(
    var to : String? = null,
    var notification : Notification = Notification()
) {
    data class Notification(
        var body : String? = null,
        var title : String? = null
    )
}

//to - push를 받는사람의 토큰아이디
//body - push 메세지의 주 내용