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
            
            // Use a safer approach to get the Google account
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.await()
            
            if (account == null) {
                Log.e("GoogleSignInHelper", "Failed to get Google account")
                return Pair(null, null)
            }
            
            Log.d("GoogleSignInHelper", "Google account obtained: ${account.email}")

            // Get the Google ID token to authenticate with Firebase
            val googleIdToken = account.idToken
            if (googleIdToken == null) {
                Log.e("GoogleSignInHelper", "Google ID token is null")
                return Pair(null, null)
            }

            // Authenticate with Firebase using the Google ID token
            val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
            Log.d("GoogleSignInHelper", "Authenticating with Firebase")
            
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
            
            if (firebaseUser == null) {
                Log.e("GoogleSignInHelper", "Firebase user is null after authentication")
                return Pair(null, null)
            }

            Log.d("GoogleSignInHelper", "Firebase auth successful for: ${firebaseUser.email}")

            // IMPORTANT: We MUST fetch the Firebase ID token for the backend.
            // Our ProGuard rules (-dontoptimize, Signature, etc.) will prevent the ParameterizedType crash here.
            val firebaseIdTokenTask = firebaseUser.getIdToken(true)
            val tokenResult = firebaseIdTokenTask.await()
            val token = tokenResult.token
            
            Log.d("GoogleSignInHelper", "Firebase ID token obtained successfully")

            Pair(token, account)
        } catch (e: ApiException) {
            Log.e("GoogleSignInHelper", "Google sign in failed with ApiException: ${e.statusCode}", e)
            Pair(null, null)
        } catch (e: Exception) {
            Log.e("GoogleSignInHelper", "Sign in failed with exception: ${e.javaClass.simpleName} - ${e.message}", e)
            Pair(null, null)
        }
    }
}
