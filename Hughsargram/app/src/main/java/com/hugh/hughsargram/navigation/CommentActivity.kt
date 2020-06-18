package com.hugh.hughsargram.navigation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hugh.hughsargram.R
import com.hugh.hughsargram.navigation.model.AlarmDTO
import com.hugh.hughsargram.navigation.model.ContentDTO
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.item_comment.view.*

class CommentActivity : AppCompatActivity() {
    var contentUid : String? = null
    var destinationUid : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)
        contentUid = intent.getStringExtra("contentUid")
        destinationUid = intent.getStringExtra("destinationUid")

        comment_recyclerview.adapter = CommentRecyclerViewAdapter()
        comment_recyclerview.layoutManager = LinearLayoutManager(this)

        comment_btn_send?.setOnClickListener {
//            - comment라는 변수를 만들어서 댓글을 적은 사람의 정보를 데이터 베이스에 넣어주는 과정
            var comment = ContentDTO.Comment()
            comment.userId = FirebaseAuth.getInstance().currentUser?.email
            comment.comment = comment_edit_message.text.toString()
            comment.uid = FirebaseAuth.getInstance().currentUser!!.uid
            comment.timestamp = System.currentTimeMillis()

            FirebaseFirestore.getInstance().collection("images").document(contentUid!!)
                .collection("comments").document().set(comment)

            //destinationUid를 intent에서 받아옴
            commentAlarm(destinationUid!!, comment_edit_message.text.toString())

//            - 댓글을 쓴 후에 comment_edit_message를 초기화 시킴
            comment_edit_message.setText("")
        }
    }

    fun commentAlarm(destinationUid: String, message: String){
        var alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
        alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
        alarmDTO.kind = 1
        alarmDTO.message = message
        alarmDTO.timestamp = System.currentTimeMillis()

        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)
    }

    inner class CommentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        //comment를 담을 ArrayList
        var comments: ArrayList<ContentDTO.Comment> = arrayListOf()

//        - Firebase 데이터를 시간순으로 읽어오는 부분
//        - 값이 중복으로 쌓일 수 있기 때문에 comments.clear()를 해줌
//        - 코드 안정성을 위해서 querySnapshot이 널일 때 리턴해줌
//        - for문을 통해서 스냅샷을 하나씩 읽어옴. → commets안에 이 스냅샷을 ContentDTO.Comment로 캐스팅해서 넣어줌
//        - notifyDataSetChanged(): RecyclerView를 새로고침해줌
        init {
            FirebaseFirestore.getInstance().collection("images").document(contentUid!!).collection("comments").orderBy("timestamp")
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    comments.clear()

                    if (querySnapshot == null)
                        return@addSnapshotListener

                    for (snapshot in querySnapshot.documents!!) {
                        comments.add(snapshot.toObject(ContentDTO.Comment::class.java)!!)
                    }
                    notifyDataSetChanged()
                }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent,false)
            return CustomViewHolder(view)
        }
        private inner class CustomViewHolder(view: View): RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int {
            return comments.size
        }

//        - 서버에서 넘어온 아이디나 메세지, 프로필 이미지를 매핑해줌
//        - comments[position].uid를 하면 댓글을 단 프로필 사진의 주소가 넘어옴
//        - url변수에 이미지 주소를 받아옴
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var view = holder.itemView
            view.commentviewitem_textview_comment.text = comments[position].comment
            view.commentviewitem_textview_profile.text = comments[position].userId

            FirebaseFirestore.getInstance().collection("profileImages").document(comments[position].uid!!).get().addOnCompleteListener { task ->

                if(task.isSuccessful){
                    var url = task.result!!["image"]
                    Glide.with(holder.itemView.context).load(url).apply(RequestOptions().circleCrop()).into(view.commentviewitem_imageview_profile)
                }
            }
        }

    }
}