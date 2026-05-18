package com.example.safe_route_project.settings

import android.content.Intent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.safe_route_project.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class AccountSectionController(
    private val activity: AppCompatActivity,
    private val accountCard: View,
    private val accountNameView: TextView,
    private val accountEmailView: TextView,
) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val googleSignInClient: GoogleSignInClient by lazy {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(activity.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(activity, options)
    }

    private val signInLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleSignInResult(result.data)
    }

    fun bind() {
        renderCurrentUser()

        accountCard.setOnClickListener {
            if (auth.currentUser == null) {
                launchSignIn()
            } else {
                showAccountDialog()
            }
        }
    }

    fun refresh() {
        renderCurrentUser()
    }

    private fun showAccountDialog() {
        AlertDialog.Builder(activity)
            .setTitle("계정 관리")
            .setItems(arrayOf("계정 변경", "로그아웃")) { _, which ->
                when (which) {
                    0 -> changeAccount()
                    1 -> logout()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun changeAccount() {
        auth.signOut()
        googleSignInClient.signOut().addOnCompleteListener {
            renderCurrentUser()
            launchSignIn()
        }
    }

    private fun logout() {
        auth.signOut()
        googleSignInClient.signOut().addOnCompleteListener {
            renderCurrentUser()
            Toast.makeText(activity, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchSignIn() {
        signInLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun handleSignInResult(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)

        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken

            if (idToken.isNullOrBlank()) {
                Toast.makeText(activity, "구글 계정 정보를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
                renderCurrentUser()
                return
            }

            firebaseAuthWithGoogle(idToken)
        } catch (e: ApiException) {
            Toast.makeText(activity, "구글 로그인 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            renderCurrentUser()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    renderCurrentUser()
                    Toast.makeText(activity, "계정이 변경되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    renderCurrentUser()
                    Toast.makeText(
                        activity,
                        "계정 변경 실패: ${task.exception?.message ?: "알 수 없는 오류"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun renderCurrentUser() {
        val firebaseUser = auth.currentUser
        val googleAccount = GoogleSignIn.getLastSignedInAccount(activity)

        val displayName = firebaseUser?.displayName
            ?: googleAccount?.displayName
            ?: activity.getString(R.string.login_required)

        val email = firebaseUser?.email
            ?: googleAccount?.email
            ?: activity.getString(R.string.connect_account)

        accountNameView.text = displayName
        accountEmailView.text = email
    }
}