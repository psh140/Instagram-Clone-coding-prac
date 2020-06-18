package com.hugh.hughsargram.navigation

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.hugh.hughsargram.R
import com.hugh.hughsargram.navigation.model.ContentDTO
import kotlinx.android.synthetic.main.fragment_grid.view.*

class GridFragment : Fragment() {
    var firestore: FirebaseFirestore? = null
    var fragmentView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentView =
            LayoutInflater.from(activity).inflate(R.layout.fragment_grid, container, false)
        firestore = FirebaseFirestore.getInstance()
        fragmentView?.gridfragment_recyclerview?.adapter = UserFragmentRecyclerViewAdapter()
        fragmentView?.gridfragment_recyclerview?.layoutManager = GridLayoutManager(activity, 3)
        return fragmentView
    }

    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs: ArrayList<ContentDTO> = arrayListOf()

        //        - 생성자를 만들어서 데이터 값들을 가져오는 부분
//        - 쿼리 부분: 사용자가 올린 이미지만 갖고올 수 있도록 쿼리를 만들어줌 (whereEqualTo("uid", uid) 부분)
//        - if 식 부분: 프로그램의 안정성을 위해서 querySnapshot이 null일 경우 바로 종료시키는 부분
//        - for 문: 데이터 값을 받아오는 부분 ( 데이터를 받아와서 contentDTOs에 넣어주는 부분)
//        - notifyDataSetChanged 부분: RecyclerView가 새로고침될 수 있도록 만들어주는 부분
        init {

            firestore?.collection("images")
                ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    contentDTOs.clear()

                    // Sometimes, This code return null of querySnapshot when it signout
                    if (querySnapshot == null)
                        return@addSnapshotListener

                    // Get data
                    for (snapshot in querySnapshot?.documents!!) {
                        contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                    }
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
