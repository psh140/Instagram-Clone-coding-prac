package com.hugh.hughsargram.navigation

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hugh.hughsargram.LoginActivity
import com.hugh.hughsargram.MainActivity
import com.hugh.hughsargram.R
import com.hugh.hughsargram.navigation.model.AlarmDTO
import com.hugh.hughsargram.navigation.model.ContentDTO
import com.hugh.hughsargram.navigation.model.FollowDTO
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.view.*


class UserFragment : Fragment() {
    var fragmentView: View? = null
    var firestore: FirebaseFirestore? = null
    var uid: String? = null
    var auth: FirebaseAuth? = null
    var currentUserUid: String? = null

    companion object {
        var PICK_PROFILE_FROM_ALBUM = 10
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentView =
            LayoutInflater.from(activity).inflate(R.layout.fragment_user, container, false)

//        - uid: uid값을 세팅해주는 부분, 이전 화면에서 넘어온 값을 받아오는 부분.
        uid = arguments?.getString("destinationUid")
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserUid = auth?.currentUser?.uid

//        로그아웃 버튼을 활성화 해주고, 로그아웃 버튼을 누르면 엑티비티를 종료한 후 로그인 화면으로 돌아가게 함.
//        그리고 firebase에 auth값을 signout해줌
        if (uid == currentUserUid) {
            // MyPage
            fragmentView!!.account_btn_follow_signout.text = getString(R.string.signout)
            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity, LoginActivity::class.java))
                auth?.signOut()
            }
        } else {
//            누구의 유저 페이지인지 보여주는 text와 back버튼을 활성화 시켜줌
            fragmentView!!.account_btn_follow_signout.text = getString(R.string.follow)

            var mainActivity = (activity as MainActivity)
            mainActivity.toolbar_username.text = arguments!!.getString("userId")

            mainActivity.toolbar_btn_back.setOnClickListener {
                mainActivity.bottom_navigation.selectedItemId = R.id.action_home
            }
            mainActivity.toolbar_title_image.visibility = View.GONE
            mainActivity.toolbar_btn_back.visibility = View.VISIBLE
            mainActivity.toolbar_username.visibility = View.VISIBLE
            fragmentView!!.account_btn_follow_signout.setOnClickListener {
                requestFollow()
            }
        }

        fragmentView?.account_iv_profile?.setOnClickListener {

            //앨범 오픈
            var photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            activity?.startActivityForResult(photoPickerIntent, PICK_PROFILE_FROM_ALBUM)
        }

//        - account_recyclerview어뎁터에 어뎁터를 달아주는 부분
//        - spanCount: 3 부분: 한 라인에 3개씩 사진이 뜨도록 하는 부분
        fragmentView?.account_recyclerview?.adapter = UserFragmentRecyclerViewAdapter()
        fragmentView?.account_recyclerview?.layoutManager = GridLayoutManager(activity, 3)

        getProfileImage()
        getFollowerAndFollowing()
        return fragmentView
    }

//    - 여기서 uid는 내 페이지를 클릭했을 때는 내 uid값이고, 상대방 페이지를 클릭했을 때는 상대방 uid값임.
//    - snapshot을 이용해서 값을 실시간으로 불러옴
    fun getFollowerAndFollowing() {
        firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if (documentSnapshot==null) return@addSnapshotListener

            var followDTO = documentSnapshot.toObject(FollowDTO::class.java)

            if(followDTO?.followingCount != null){
                fragmentView?.account_tv_following_count?.text = followDTO?.followingCount?.toString()
            }
            if(followDTO?.followerCount != null){
                fragmentView?.account_tv_follower_count?.text = followDTO?.followerCount?.toString()

                if(followDTO?.followers?.containsKey(currentUserUid!!)) {
                    fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow_cancel)
                    fragmentView?.account_btn_follow_signout?.background?.setColorFilter(
                        ContextCompat.getColor(activity!!,R.color.colorLightGray), PorterDuff.Mode.MULTIPLY)
                }else{
                    if (uid != currentUserUid) {
                        fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)
                        fragmentView?.account_btn_follow_signout?.background?.colorFilter = null
                    }
                }
            }
        }
    }

    fun requestFollow() {
        // Save data to my account

        var tsDocFollowing = firestore!!.collection("users").document(currentUserUid!!)
        firestore?.runTransaction { transaction ->

            var followDTO = transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)
//            - 내 계정이 누군가를 팔로우하는 과정이 담긴 트랜젝션을 만듦
//            - followDTO에 아무 값이 없을 때 처리하는 부분
//            - 여기서 uid는 상대방의 uid. (중복 팔로윙을 방지하기 위함)
//            - transaction.set(tsDocFollowing, followDTO): 데이터가 데이터 베이스에 담기게 됨.
            if (followDTO == null) {

                followDTO = FollowDTO()
                followDTO.followingCount = 1
                followDTO.followings[uid!!] = true

                transaction.set(tsDocFollowing, followDTO)
                return@runTransaction
            }

            if (followDTO.followings.containsKey(uid)) {
                // It remove following third person when a third person follow me
                followDTO?.followingCount = followDTO?.followingCount - 1
                followDTO?.followings.remove(uid)
            } else {
                // It remove following third person when a third person not follow me
                followDTO?.followingCount = followDTO?.followingCount + 1
                followDTO?.followings[uid!!] = true
            }
            transaction.set(tsDocFollowing, followDTO)
            return@runTransaction
        }

        // Save data to third person
        var tsDocFollower = firestore!!.collection("users").document(uid!!)
        firestore?.runTransaction { transaction ->

            var followDTO = transaction.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            if (followDTO == null) {

                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUserUid!!] = true
                followerAlarm(uid!!)
                transaction.set(tsDocFollower, followDTO!!)
                return@runTransaction
            }

            if (followDTO!!.followers.containsKey(currentUserUid!!)) {
                // It cancel my follower when I follow a third person
                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.followers.remove(currentUserUid!!)
            } else {
                // It cancel my follower when I don't follow a third person
                followDTO!!.followerCount = followDTO!!.followerCount + 1
                followDTO!!.followers[currentUserUid!!] = true
                followerAlarm(uid!!)

            }
            transaction.set(tsDocFollower, followDTO!!)
            return@runTransaction
        }

    }

    fun followerAlarm(destinationUid: String){
        var alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = auth?.currentUser?.email
        alarmDTO.uid = auth?.currentUser?.uid
        alarmDTO.kind = 2
        alarmDTO.timestamp = System.currentTimeMillis()
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)
    }

    //    - firestore에서 profileImages라는 컬렉션으로부터 내 uid의 document를 읽어옴
//    - 프로필 사진이 실시간으로 변하는 것을 받아오기 위해서 Snapshot이용
//    - Glide로 이미지를 다운로드 받아옴
//    - circleCrop: 이미지를 원형(circle)으로 받아오기 위함
    fun getProfileImage() {
        firestore?.collection("profileImages")?.document(uid!!)
            ?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if (documentSnapshot == null) return@addSnapshotListener

                if (documentSnapshot?.data != null) {
                    val url = documentSnapshot?.data!!["image"]
                    Glide.with(activity!!).load(url).apply(RequestOptions().circleCrop())
                        .into(fragmentView!!.account_iv_profile)
                }
            }
    }

    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs: ArrayList<ContentDTO> = arrayListOf()

        //        - 생성자를 만들어서 데이터 값들을 가져오는 부분
//        - 쿼리 부분: 사용자가 올린 이미지만 갖고올 수 있도록 쿼리를 만들어줌 (whereEqualTo("uid", uid) 부분)
//        - if 식 부분: 프로그램의 안정성을 위해서 querySnapshot이 null일 경우 바로 종료시키는 부분
//        - for 문: 데이터 값을 받아오는 부분 ( 데이터를 받아와서 contentDTOs에 넣어주는 부분)
//        - notifyDataSetChanged 부분: RecyclerView가 새로고침될 수 있도록 만들어주는 부분
        init {

            firestore?.collection("images")?.whereEqualTo("uid", uid)
                ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    contentDTOs.clear()

                    // Sometimes, This code return null of querySnapshot when it signout
                    if (querySnapshot == null)
                        return@addSnapshotListener

                    // Get data
                    for (snapshot in querySnapshot?.documents!!) {
                        contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                    }

                    fragmentView?.account_tv_post_count?.text = contentDTOs.size.toString()
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            //1/3크기의 정사각형
            val width = resources.displayMetrics.widthPixels / 3

            val imageView = ImageView(parent.context)
            imageView.layoutParams = LinearLayoutCompat.LayoutParams(width, width)

            return CustomViewHolder(imageView)
        }

        //        - oncreatViewHolder 메소드에서 리턴해준 imageView를 RecyclerView.ViewHolder로 넘겨주는 부분
        inner class CustomViewHolder(var imageView: ImageView) :
            RecyclerView.ViewHolder(imageView)

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        //        - 데이터를 매핑해주는 부분에 imageView를 불러옴
//        - holder값을 CustomViewHolder로 캐스팅해줌.  그 후 imageView를 받아옴
//        - Gilde부분: 이미지를 다운로드해오는 부분
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageView = (holder as CustomViewHolder).imageView
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl)
                .apply(RequestOptions().centerCrop()).into(imageView)
        }
    }



}

