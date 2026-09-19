package com.waveapp.tourcat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.waveapp.tourcat.common.ComConstant
import com.waveapp.tourcat.helper.LogoutHelper
import com.waveapp.tourcat.helper.MessageHelper
import com.waveapp.tourcat.helper.TokenManager
import com.waveapp.tourcat.util.EnvironmentUtil
import com.waveapp.tourcat.util.NetworkUtil
import kotlinx.coroutines.launch

class SignInActivity : AppCompatActivity() {

    private lateinit var credentialManager: CredentialManager
    private lateinit var auth: FirebaseAuth
    private var progressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signin)

        progressBar = findViewById(R.id.progressBar)
        hideProgress()

        credentialManager = CredentialManager.create(this)
        auth = Firebase.auth

        val btnGoogleLogin = findViewById<LinearLayout>(R.id.btn_google_login)
        val btnLogout = findViewById<Button>(R.id.btn_logout)
        val btnGuest = findViewById<Button>(R.id.btn_guest)

        setButtonVisibility(EnvironmentUtil.isUserLoggedIn())

        btnGoogleLogin.setOnClickListener { launchCredentialManager() }
        btnGuest.setOnClickListener { goMainOrReturn() }
        btnLogout.setOnClickListener { logoutAndNotify() }
    }

    private fun showProgress() { progressBar?.visibility = View.VISIBLE }
    private fun hideProgress() { progressBar?.visibility = View.GONE }

    private fun setButtonVisibility(isLoggedIn: Boolean) {
        findViewById<LinearLayout>(R.id.btn_google_login).visibility =
            if (isLoggedIn) View.GONE else View.VISIBLE
        findViewById<Button>(R.id.btn_guest).visibility =
            if (isLoggedIn) View.GONE else View.VISIBLE
        findViewById<Button>(R.id.btn_logout).visibility =
            if (isLoggedIn) View.VISIBLE else View.GONE
    }

    private fun launchCredentialManager() {
        if (!NetworkUtil.isNetworkConnected(this)) {
            MessageHelper.showToastLong(this,getString(R.string.msg_network_notconnect))
            return
        }
        showProgress()
        val signInWithGoogleOption = GetSignInWithGoogleOption
            .Builder(getString(R.string.default_web_client_id))
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()
        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = this@SignInActivity,
                    request = request
                )
                handleGoogleCredential(result.credential)
            } catch (e: Exception) {
                //MessageHelper.showToast(this@SignInActivity, getString(R.string.msg_error_signin_fail) + ": ${e.message}")
                hideProgress()
                // ** 사용자 취소 예외 분기 **
                if ((e is ApiException && e.statusCode == 16)
                    || (e.message?.contains("16") == true)        // 예외 메시지에 16 포함
                ) {
                    // 사용자 취소 → 아무 처리 안함 또는 안내만
                    // MessageHelper.showToast(this@SignInActivity, "로그인을 취소하셨습니다.")
                } else {
                    // 그 외 모든 예외는 실패 메시지 표시
                    MessageHelper.showToast(
                        this@SignInActivity,
                        getString(R.string.msg_error_signin_fail) + ": ${e.message}"
                    )
                }
            }
        }
    }

    private fun handleGoogleCredential(credential: Credential) {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
        } else {
            MessageHelper.showToast(this, getString(R.string.msg_error_invalid_google))
            hideProgress()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                hideProgress()
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        saveAuthToPref(
                            idToken = idToken,
                            uid = user.uid,
                            email = user.email ?: ""
                        )
                    }
                    // 여기서 goMainOrReturn() 호출하지 않음 (코인 fetch 후 이동!)
                } else {
                    MessageHelper.showToast(this, getString(R.string.msg_error_signin_fail))
                }
            }
    }

    private fun saveAuthToPref(idToken: String, uid: String, email: String) {
        val prefs = getSharedPreferences(ComConstant.PREF_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        prefs.edit().apply {
            putString(ComConstant.PREF_KEYS.ID_TOKEN, idToken)
            putString(ComConstant.PREF_KEYS.USER_UID, uid)
            putString(ComConstant.PREF_KEYS.USER_EMAIL, email)
            putBoolean(ComConstant.PREF_KEYS.IS_LOGGED_IN, true)
            putLong(ComConstant.PREF_KEYS.LAST_LOGIN_TIME, now)
            apply()
        }
        fetchAndSaveCredit(uid, email) // 코인 조회 및 저장 후 Main으로 이동!
    }

    // [수정] 코인 fetch 후 Pref에 저장, 저장 완료 후 MainActivity로 이동!
    private fun fetchAndSaveCredit(userUid: String, userEmail: String) {
        TokenManager.fetchTokenAndSavePref(
            context = this,
            userId = userEmail,
            onComplete = {
                goMainOrReturn() // 정상/오류 관계없이 무조건 1회 호출
            }
        )
    }

    /** ----------------------
     * 로그아웃 처리 - LogoutHelper 사용
     * ---------------------- */
    private fun logoutAndNotify() {
        showProgress()
        LogoutHelper.performLogout(this) {
            hideProgress()
            setButtonVisibility(false)
            MessageHelper.showToast(this, getString(R.string.msg_signout_complet))
            // 필요시 여기서 메인 이동 등 추가
        }
    }

    private fun goMainOrReturn() {
        hideProgress()
        val afterLoginIntent = intent.getParcelableExtra<Intent>("AFTER_LOGIN_INTENT")
        if (afterLoginIntent != null) {
            startActivity(afterLoginIntent)
        } else {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        finish()
    }

    override fun onStop() {
        super.onStop()
        hideProgress()
    }
}
