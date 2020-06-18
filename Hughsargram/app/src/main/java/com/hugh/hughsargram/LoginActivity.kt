package com.hugh.hughsargram

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*


//1. 로그인
//firebase에 연결해서 사용자를 추가할 수 있게 함
//계정 생성 및 로그인은 이메일과 패스워드를 통해 수행되도록 함
//로그인이 완료되면 다음 엑티비티인 MainActivity로 넘어가게 함

//2. 구글 로그인
//구글 로그인 후 firebase를 거쳐 로그인 확인을 응답받음

class LoginActivity : AppCompatActivity() {
    var auth : FirebaseAuth? = null
    var googleSignInClient : GoogleSignInClient? = null
    var GOOGLE_LOGIN_CODE = 9001
//  callbackManager 객체: 페이스북 로그인 결과를 가져오는 객체
    var callBackManager : CallbackManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        email_login_button.setOnClickListener {
            signinAndSignup()
        }

        google_sign_in_button.setOnClickListener {
            googleLogin()
        }

        facebook_login_button.setOnClickListener {
            facebookLogin()
        }

        //사용자 ID와 프로필 정보를 요청하기 위해 GoogleSignInOptions 객체를 DEFAULT_SIGN_IN 인자와 함께 생성함
        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        //gso객체를 인자로 전달하여 GoogleSignInClient 객체를 생성
        googleSignInClient = GoogleSignIn.getClient(this, gso)
//        printHashKey()

//      로그인을 응답을 처리할 콜백 관리자를 만듦
        callBackManager = CallbackManager.Factory.create()
    }
//    onStart는 생명주기상 onCreate 다음에 호출된다.
//    보통 회원가입 등이 필요한 기능에서 리스너 객체 등을 onCreate에서 선언하고 onStart에서 선언된 리스너를 등록한 후,
//    이미 로그인 된 사용자인지를 구분하여 로그인 화면으로 넘어가지 않고 바로 메인으로 넘어가게 할 때 사용한다.
//    브로드캐스트리시버를 사용할 때도 보통 여기다가 등록한다. onStop과 짝을 이루고 다루게 된다.
    override fun onStart() {
        super.onStart()
        //자동 로그인 설정
        moveMainPage(auth?.currentUser)
    }

//    getting hashkey fun
//        fun printHashKey() {
//        try {
//            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
//            for (signature in info.signatures) {
//                val md = MessageDigest.getInstance("SHA")
//                md.update(signature.toByteArray())
//                val hashKey = String(Base64.encode(md.digest(), 0))
//                Log.i("TAG", "printHashKey() Hash Key: $hashKey")
//            }
//        } catch (e: NoSuchAlgorithmException) {
//            Log.e("TAG", "printHashKey()", e)
//        } catch (e: Exception) {
//            Log.e("TAG", "printHashKey()", e)
//        }
//    }

    //gso객체를 인자로 넘겨서 생성한 googleSignInClient 객체의 signInIntent Method를 사용하여 Intent 생성
    //만들어진 Intent를 startActivityForResult 객체에 전달함(여기서 GOOGLE_LOGIN_CODE는 request코드임) → 사용자에게 인증을 요청하는 엑티비티(화면)가 실행됨
    fun googleLogin(){
        var signInIntent = googleSignInClient?.signInIntent
        startActivityForResult(signInIntent, GOOGLE_LOGIN_CODE)
    }

//    LoginManager를 이용해서 페이스북에서 받을 권한을 요청함. 여기에서는 public_profile과 email을 요청함
//    그 다음 object: FacebookCallback<LoginResult>는 최종적으로 로그인에 성공했을 때 나오는 부분임
//    onSuccess메소드: 로그인에 성공했을 때 실행되는 메소드
    fun facebookLogin(){
        LoginManager.getInstance()
            .logInWithReadPermissions(this, Arrays.asList("public_profile","email"))

        LoginManager.getInstance()
            .registerCallback(callBackManager, object: FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult?) {
                    handleFackbookAccessToken(result?.accessToken)
                }

                override fun onCancel() {

                }

                override fun onError(error: FacebookException?) {

                }

            })
    }

//    handleFackbookAccessToken 메소드: 페이스북 데이터를 firebase에 넘기는 기능
//    구글 로그인과 동일하게 firebase에 인증 확인을 받으면 됨.
    fun handleFackbookAccessToken(token: AccessToken?){
        var credental = FacebookAuthProvider.getCredential(token?.token!!)
        auth?.signInWithCredential(credental)
            ?.addOnCompleteListener {
                    task ->
                if(task.isSuccessful){
                    // Login
                    moveMainPage(task.result?.user)
                }else{
                    // Show the error message
                    Toast.makeText(this, task.exception!!.message, Toast.LENGTH_LONG).show()
                }
            }
    }

//    - 사용자가 SignIn에 성공하면 onActivityResult가 실행됨
//    * 여기서 requestCode는 앞서 작성한 startActivityForResult의 두 번째 인자값임
//    사용자가 엑티비티에서 실행한 결과를 onActivityResult에서 data로 받을 수 있음
//    result에 구글에 로그인(실행한 것) 했을 때 구글에서 넘겨주는 결과값(실행한 결과)를 받아와서 저장해줌
//    result가 성공했을 때 이 값을 firebase에 넘겨주기 위해 account에 result의 signInAccount값을 저장한 후
//    firebaseAuthWithGoogle에 account값을 전달함
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

//    onActivityResult 메소드(콜백 함수)에 callbackManager.onActivityResult를 호출해서 로그인의 결과를 callbackManager를 통해 LoginManager에게 전달함
        callBackManager?.onActivityResult(requestCode, resultCode, data)

        if(requestCode==GOOGLE_LOGIN_CODE){
            var result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result != null) {
                if(result.isSuccess){
                    var account = result.signInAccount
                    firebaseAuthWithGoogle(account)
                }
            }
        }
    }

//    사용자가 성공적으로 로그인 했을 때 account(GoogleSignInAccount 객체)에서 ID토큰을 가져와서 firebase 사용자 인증정보로 교환함
//    signInWithCredential 메소드를 통해서 firebase에 인증함
//    auth 결과값을 확인하는 부분은 앞서 이메일을 확인하는 부분과 같음
    fun firebaseAuthWithGoogle(account: GoogleSignInAccount?){
        var credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener {
                    task ->
                if(task.isSuccessful){
                    // Login
                    moveMainPage(task.result?.user)
                }else{
                    // Show the error message
                    Toast.makeText(this, task.exception!!.message, Toast.LENGTH_LONG).show()
                }
            }
    }

    //Sign in or sign up method
    fun signinAndSignup() {
        auth?.createUserWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())
            ?.addOnCompleteListener {
                    task ->
                if(task.isSuccessful) {
                    //계정 생성 성공 시 main activity로 이동
                    moveMainPage(task.result?.user)
                } else if(task.exception?.message.isNullOrEmpty()) {
                    Toast.makeText(this, task.exception!!.message, Toast.LENGTH_LONG).show()
                } else {
                    signinEmail()
                }
            }
    }

    fun signinEmail(){
        auth?.signInWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())
            ?.addOnCompleteListener {
                    task ->
                if(task.isSuccessful){
                    // Login
                    moveMainPage(task.result?.user)
                }else{
                    // Show the error message
                    Toast.makeText(this, task.exception!!.message, Toast.LENGTH_LONG).show()
                }
            }
    }

//    * Intent 객체를 선언할 때 필요한 파라미터
//
//    현재 액티비티(this)
//    전환할 액티비티(MainActivity::class.java)
    fun moveMainPage(user : FirebaseUser?) {
        if(user != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}