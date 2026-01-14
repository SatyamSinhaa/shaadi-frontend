package com.example.myapplication.data.api

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class GoogleSignInHelper(private val context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var googleSignInClient: GoogleSignInClient

    init {
        configureGoogleSignIn()
    }

    private fun configureGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("305204559366-avns077q8ck2ab5ddsapdrs48g3mk467.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    fun signOut() {
        googleSignInClient.signOut()
        auth.signOut()
    }

    suspend fun handleSignInResult(data: Intent?): Pair<String?, GoogleSignInAccount?> {
        return try {
            Log.d("GoogleSignInHelper", "Processing sign-in result, data: ${data != null}")
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            Log.d("GoogleSignInHelper", "Google account obtained: ${account.email}")

            // Google Sign In was successful, authenticate with Firebase
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            Log.d("GoogleSignInHelper", "Authenticating with Firebase")
            val authResult = auth.signInWithCredential(credential).await()
            Log.d("GoogleSignInHelper", "Firebase auth successful for: ${authResult.user?.email}")

            // Return the ID token and account info
            val idToken = authResult.user?.getIdToken(false)?.await()?.token
            Log.d("GoogleSignInHelper", "ID token obtained: ${idToken != null}")
            Pair(idToken, account)
        } catch (e: ApiException) {
            Log.e("GoogleSignInHelper", "Google sign in failed", e)
            Pair(null, null)
        } catch (e: Exception) {
            Log.e("GoogleSignInHelper", "Firebase authentication failed", e)
            Pair(null, null)
        }
    }
}
