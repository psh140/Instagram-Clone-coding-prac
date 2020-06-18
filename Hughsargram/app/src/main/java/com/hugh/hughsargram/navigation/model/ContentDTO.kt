package com.hugh.hughsargram.navigation.model

import java.util.HashMap

data class ContentDTO(var explain: String? = null,
                      var imageUrl: String? = null,
                      var uid: String? = null,
                      var userId: String? = null,
                      var timestamp: Long? = null,
                      var favoriteCount: Int = 0,
                      var favorites: MutableMap<String, Boolean> = HashMap()) {

    data class Comment(var uid: String? = null,
                       var userId: String? = null,
                       var comment: String? = null,
                       var timestamp: Long? = null)
}
//- explain: 컨텐츠의 설명을 관리하는 변수
//
//- imageUrl: 이미지 주소를 관리하는 변수
//
//- uid: 어느 유저가 이미지를 올렸는지 관리하는 변수
//
//- userId: 컨텐츠를 올린 유저의 이미지를 관리해주는 변수
//
//- timestamp: 컨텐츠를 올린 시간을 관리하는 변수
//
//- favoriteCount: '좋아요' 갯수를 관리하는 변수
//
//- favorites: '중복 좋아요를 방지'하기 위해 좋아요를 누른 유저를 관리하는 변수 ( Map → MutableMap 으로 변경)
//
//
//
//- Comment: 댓글 관리해주는 데이터 클래스
//
//uid: 위와 동일
//userId: 유저의 이메일을 관리해주는 변수
//comment: 커멘트를 관리해주는 변수
//timestamp: 커멘트를 올린 시간을 관리해주는 변수