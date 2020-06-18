package com.hugh.hughsargram

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.hugh.hughsargram.navigation.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.jar.Manifest

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {
    override fun onNavigationItemSelected(p0: MenuItem): Boolean {
        setToolbarDefault()
        when (p0.itemId) {
            R.id.action_home -> {
                var detailViewFragment = DetailViewFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_content, detailViewFragment).commit()
            }
            R.id.action_search -> {
                var gridFragment = GridFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, gridFragment)
                    .commit()
            }
            R.id.action_add_photo -> {
                if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    startActivity(Intent(this, AddPhotoActivity::class.java))
                }
            }
            R.id.action_favorite_alarm -> {
                var alarmFragment = AlarmFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, alarmFragment)
                    .commit()
            }
            R.id.action_account -> {
                var userFragment = UserFragment()
                var bundle = Bundle()

                //uid 값을 받아옴
                var uid = FirebaseAuth.getInstance().currentUser?.uid

//                - UserFragment에 uid를 넘겨주는 부분
//                * Bundle 객체: 안드로이드에서 Activity 간에 데이터를 주고 받을 때 사용하는 클래스.
//                  여러가지 데이터를 전송할 수 있음 (문자열로 된 키와 여러 타입의 값을 저장하는 일종의 Map 클래스임)
                bundle.putString("destinationUid", uid)
                userFragment.arguments = bundle
                supportFragmentManager.beginTransaction().replace(R.id.main_content, userFragment)
                    .commit()
            }
        }
        return false
    }
    //    * supportFragmentManager.beginTransaction().replace(R.id.xxxxxx, xxxFragment).commit() 부분 설명
//
//    프래그먼트를 Transaction(삭제,추가,대체)을 하기 위해서 supportFragmentManager가 필요함
//    replace 함수를 통해 프래그먼트를 교체할 수 있음
//    R.id.xxxxxx: 엑티비티 레이아웃에서 교체되는 프래그먼트가 위치할 최상위 뷰 그룹 (여기서는 프래그먼트 레이아웃 리소스 ID)
//    xxxFragment: 교체할 프래그먼트 객체
//    commit()을 작성해야 변경내용이 저장됨

    fun setToolbarDefault(){
        toolbar_username.visibility = View.GONE
        toolbar_btn_back.visibility = View.GONE
        toolbar_title_image.visibility = View.VISIBLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottom_navigation.setOnNavigationItemSelectedListener(this)
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
            1
        )

        //메인 화면으로 DetailViewFragment가 보이도록 하는 부분
        bottom_navigation.selectedItemId = R.id.action_home
    }
//    - 위와 같이 MainActivity 클래스가 BottomNavigationView.OnNavigationItemSelectedListener를 상속하도록 하고
//      onNavigationItemSelected 메소드를 오버라이드 해줌
//    - when을 이용해서 각 아이콘을 눌렀을 때 배경색이 바뀌도록 코드를 작성함

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 앨범에서 Profile Image 사진 선택시 호출 되는 부분
        if (requestCode == UserFragment.PICK_PROFILE_FROM_ALBUM && resultCode == Activity.RESULT_OK) {

            var imageUri = data?.data
            var uid = FirebaseAuth.getInstance().currentUser!!.uid //파일 업로드
            //userProfileImages : 이미지 저장 폴더  uid : 파일명
            var storageRef = FirebaseStorage.getInstance().reference.child("userProfileImages").child(uid!!)

            //사진을 업로드 하는 부분  userProfileImages 폴더에 uid에 파일을 업로드함

//            - 이미지 다운로드 주소를 받아오기 위해서 continueWithTask를 사용
//            - continueWithTask의 리턴 값이 addOnSuccessListener로 넘어옴
//            - 이 값을 HashMap에 담아줌
//                    - map["image"]부분: 이미지 주소값을 담음
            storageRef.putFile(imageUri!!).continueWithTask { task: com.google.android.gms.tasks.Task<UploadTask.TaskSnapshot> ->
                return@continueWithTask storageRef.downloadUrl
            }.addOnSuccessListener { uri ->
                var map = HashMap<String, Any>()
                map["image"] = uri.toString()
                FirebaseFirestore.getInstance().collection("profileImages").document(uid).set(map)
            }

        }
    }

}