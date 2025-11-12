package com.university.vtexter.utils

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.university.vtexter.data.repository.VTexterRepository
import com.university.vtexter.models.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Syncs users from Firebase Realtime Database to Room
 * This allows us to see all registered users as contacts
 */
object UserSyncManager {

    private val database = FirebaseDatabase.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    /**
     * Start syncing users from Firebase to Room
     */
    fun startSync(context: Context) {
        val repository = VTexterRepository(context)

        // Listen to users node in Firebase Realtime Database
        database.getReference("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                CoroutineScope(Dispatchers.IO).launch {
                    snapshot.children.forEach { userSnapshot ->
                        val userId = userSnapshot.key ?: return@forEach
                        val name = userSnapshot.child("name").getValue(String::class.java) ?: ""
                        val email = userSnapshot.child("email").getValue(String::class.java) ?: ""
                        val status = userSnapshot.child("status").getValue(String::class.java) ?: ""
                        val profilePic = userSnapshot.child("profilePicture").getValue(String::class.java) ?: ""

                        // Skip current user
                        if (userId == currentUserId) return@forEach

                        val user = User(
                            userId = userId,
                            name = name,
                            email = email,
                            profilePicture = profilePic,
                            status = status
                        )

                        // Save to Room database
                        repository.saveUser(user)
                        repository.saveContact(user)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    /**
     * Sync current user to Firebase so others can see them
     */
    fun syncCurrentUser(context: Context) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        val userData = mapOf(
            "userId" to currentUser.uid,
            "name" to (currentUser.displayName ?: "User"),
            "email" to (currentUser.email ?: ""),
            "profilePicture" to "",
            "status" to "Hey there! I'm using VTexter",
            "isOnline" to true,
            "lastSeen" to System.currentTimeMillis()
        )

        database.getReference("users")
            .child(currentUser.uid)
            .setValue(userData)
    }
}