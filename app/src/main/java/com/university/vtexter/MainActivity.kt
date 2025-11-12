package com.university.vtexter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.university.vtexter.navigation.NavGraph
import com.university.vtexter.ui.theme.VTexterTheme
import com.university.vtexter.utils.UserSyncManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Start user sync if logged in
        if (FirebaseAuth.getInstance().currentUser != null) {
            UserSyncManager.startSync(this)
            UserSyncManager.syncCurrentUser(this)
        }

        setContent {
            VTexterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph()
                }
            }
        }
    }
}