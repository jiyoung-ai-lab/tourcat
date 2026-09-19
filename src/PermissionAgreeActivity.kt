package com.waveapp.tourcat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class PermissionAgreeActivity : AppCompatActivity() {

    // 1. SDK 버전별 권한 목록
    private val requiredPermissions: Array<String> by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.ACCESS_COARSE_LOCATION // 대략적 위치
            )
        } else {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE ,
                Manifest.permission.ACCESS_COARSE_LOCATION // 대략적 위치
            )
        }
    }

    // 2. 권한 요청 런처 (ActivityResult API)
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val denied = results.filter { !it.value }.keys
//        if (denied.isEmpty()) {
            goSignIn()
//        } else {
//            showDeniedDialog(denied)
//        }
    }

    // 3. 아직 미동의 권한만 추출
    private fun getUngrantedPermissions(): List<String> {
        return requiredPermissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_agree)

        findViewById<Button>(R.id.btn_permission_agree).setOnClickListener {
            requestAllPermissions()
        }
    }

    private fun requestAllPermissions() {
        val toRequest = getUngrantedPermissions()
        if (toRequest.isEmpty()) {
            goSignIn()
        } else {
            permissionLauncher.launch(toRequest.toTypedArray())
        }
    }

    private fun goSignIn() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }

}
