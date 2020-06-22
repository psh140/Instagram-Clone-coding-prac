package com.hugh.hughsargram.navigation

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hugh.hughsargram.R
import com.hugh.hughsargram.navigation.model.AlarmDTO
import com.hugh.hughsargram.navigation.model.ContentDTO
import com.hugh.hughsargram.navigation.util.FcmPush
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.item_detail.view.*

class DetailViewFragment :Fragment(){
    var firestore : FirebaseFirestore? = null
    var uid : String? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view = LayoutInflater.from(activity).inflate(R.layout.fragment_detail,container,false)
        firestore = FirebaseFirestore.getInstance()

//        - 트랜젝션을 해주기 위해서 uid값을 받아오는 부분
        uid = FirebaseAuth.getInstance().currentUser?.uid

        view.detailviewfragment_recyclerview.adapter = DetailViewRecyclerViewAdapter()
        view.detailviewfragment_recyclerview.layoutManager = LinearLayoutManager(activity)

        return view
    }

    inner class DetailViewRecyclerViewAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        var contentUidList : ArrayList<String> = arrayListOf()

        init {
            // Database에 접근해서 데이터를 받아올 수 있게 하는 query(orderBy("timestamp")를 이용해서 시간 순으로 받아오도록 함.
            firestore?.collection("images")?.orderBy("timestamp")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                contentDTOs.clear()
                contentUidList.clear()
                for(snapshot in querySnapshot!!.documents){
                    var item = snapshot.toObject(ContentDTO::class.java)
                    contentDTOs.add(item!!)
                    contentUidList.add(snapshot.id)
                }
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail, parent, false)

            return CustomViewHolder(view)
        }
        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)


        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var viewholder = (holder as CustomViewHolder).itemView

            // User Id
            viewholder.detailviewitem_profile_textview.text = contentDTOs[position].userId

            // Image
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).into(viewholder.detailviewitem_imageview_content)

            // Explain
            viewholder.detailviewitem_explain_textview.text = contentDTOs[position].explain

            // likes
            viewholder.detailviewitem_favoritecounter_textview.text = "Likes" + contentDTOs[position].favoriteCount

            // ProfileImage
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).into(viewholder.detailviewitem_profile_image)

            //This code is when the button is clicked
            viewholder.detailviewitem_favorite_imageview.setOnClickListener {
                favoriteEvent(position)
            }

            //This code is when the page is loaded
            if(contentDTOs!![position].favorites.containsKey(uid)) {
                viewholder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite)
            } else {
                viewholder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite_border)
            }

            // This code is when the profile image is clicked
            viewholder.detailviewitem_profile_image.setOnClickListener {

                val fragment = UserFragment()
                val bundle = Bundle()

                //선택된 Uid값
                bundle.putString("destinationUid", contentDTOs[position].uid)
                //선택된 email값
                bundle.putString("userId", contentDTOs[position].userId)

                fragment.arguments = bundle
                activity!!.supportFragmentManager.beginTransaction()
                    .replace(R.id.main_content, fragment)
                    .commit()
            }
            viewholder.detailviewitem_comment_imageview.setOnClickListener { v ->
                var intent = Intent(v.context, CommentActivity::class.java)
//                - contentUidList[position]: 내가 선택한 이미지의 uid값이 담김
                intent.putExtra("contentUid", contentUidList[position])
                intent.putExtra("destinationUid", contentDTOs[position].uid)
                startActivity(intent)
            }
        }

        fun favoriteEvent(position : Int) {
            //유저가 선택한 컨텐츠의 uid를 받아와서 '좋아요'해주는 이벤트를 넣을 변수
            //(contentUidList[position] ☞ 내가 선택한 컨텐츠의 uid 값을 넣어주는 코드)
            var tsDoc = firestore?.collection("images")?.document(contentUidList[position])

            //- 데이터를 입력하기 전에 트랜젝션을 불러와야 함
            //* 트랜젝션: 데이터베이스의 상태(추가, 삭제, 변경)를 변화시키기 해서 수행하는 작업
            firestore?.runTransaction { transaction ->

//                트랜젝션 데이터를 contentDTO로 캐스팅해주는 부분
                var contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

                if(contentDTO!!.favorites.containsKey(uid)) {
                    //When the button is clicked
                    contentDTO.favoriteCount = contentDTO?.favoriteCount - 1
                    contentDTO.favorites.remove(uid)
                }else{
                    // When the button is not clicked
                    contentDTO.favoriteCount = contentDTO?.favoriteCount + 1
                    contentDTO.favorites[uid!!] = true
                    favoriteAlarm(contentDTOs[position].uid!!)
                }
//                - 이 트랜젝션을 다시 서버로 돌려주는 코드
                transaction.set(tsDoc,contentDTO)
            }
        }

        fun favoriteAlarm(destinationUid: String){
            val alarmDTO = AlarmDTO() //메세지를 보낼 객체
            alarmDTO.destinationUid = destinationUid
            alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
            alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
            alarmDTO.kind = 0
            alarmDTO.timestamp = System.currentTimeMillis()

            //alarmDTO에 데이터베이스에 넣을 정보들을 담은 후 FirebaseFirestore에 이 객체를 넘김
            FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

            var message = FirebaseAuth.getInstance()?.currentUser?.email + getString(R.string.alarm_favorite)
            FcmPush.instance.sendMessage(destinationUid, "Hughstagram", message)
        }
    }


}
