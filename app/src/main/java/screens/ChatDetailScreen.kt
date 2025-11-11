package com.university.vtexter.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.university.vtexter.components.MessageItem
import com.university.vtexter.models.MessageType
import com.university.vtexter.utils.PermissionsUtil
import com.university.vtexter.viewmodels.ChatDetailViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chatId: String,
    onNavigateBack: () -> Unit,
    viewModel: ChatDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val otherUserName by viewModel.otherUserName.collectAsState()
    val otherUserProfilePic by viewModel.otherUserProfilePic.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()

    var messageText by remember { mutableStateOf("") }
    var showAttachMenu by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.sendImageMessage(chatId, it, context)
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.tempCameraUri?.let { uri ->
                viewModel.sendImageMessage(chatId, uri, context)
            }
        }
    }

    // Video picker
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.sendVideoMessage(chatId, it, context)
        }
    }

    // Document picker
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.sendDocumentMessage(chatId, it, context)
        }
    }

    // Permission launchers
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = viewModel.createCameraImageUri(context)
            cameraLauncher.launch(uri)
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            imagePickerLauncher.launch("image/*")
        }
    }

    LaunchedEffect(chatId) {
        viewModel.loadMessages(chatId)
        viewModel.listenToTypingStatus(chatId)
        viewModel.listenToOnlineStatus(chatId)
    }

    // Auto-scroll to bottom when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    // Update typing status
    LaunchedEffect(messageText) {
        viewModel.updateTypingStatus(chatId, messageText.isNotEmpty())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(otherUserName)
                        Text(
                            text = when {
                                isTyping -> "typing..."
                                isOnline -> "online"
                                else -> "offline"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOnline) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.initiateCall(chatId, false)
                    }) {
                        Icon(Icons.Default.VideoCall, contentDescription = "Video Call")
                    }
                    IconButton(onClick = {
                        viewModel.initiateCall(chatId, true)
                    }) {
                        Icon(Icons.Default.Call, contentDescription = "Voice Call")
                    }
                    IconButton(onClick = { /* More options */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                }
            )
        },
        bottomBar = {
            Column {
                // Upload progress
                if (isUploading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                Surface(shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Attachment button
                        IconButton(onClick = { showAttachMenu = !showAttachMenu }) {
                            Icon(
                                if (showAttachMenu) Icons.Default.Close else Icons.Default.AttachFile,
                                contentDescription = "Attach"
                            )
                        }

                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type a message...") },
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Send or voice message button
                        FloatingActionButton(
                            onClick = {
                                if (messageText.isNotBlank()) {
                                    viewModel.sendMessage(chatId, messageText.trim())
                                    messageText = ""
                                } else {
                                    // TODO: Record voice message
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                if (messageText.isNotBlank())
                                    Icons.AutoMirrored.Filled.Send
                                else
                                    Icons.Default.Mic,
                                contentDescription = if (messageText.isNotBlank()) "Send" else "Voice"
                            )
                        }
                    }
                }

                // Attachment menu
                if (showAttachMenu) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            AttachmentOption(
                                icon = Icons.Default.PhotoLibrary,
                                label = "Gallery",
                                onClick = {
                                    if (PermissionsUtil.hasStoragePermission(context)) {
                                        imagePickerLauncher.launch("image/*")
                                    } else {
                                        storagePermissionLauncher.launch(PermissionsUtil.STORAGE_PERMISSIONS)
                                    }
                                    showAttachMenu = false
                                }
                            )
                            AttachmentOption(
                                icon = Icons.Default.CameraAlt,
                                label = "Camera",
                                onClick = {
                                    if (PermissionsUtil.hasCameraPermission(context)) {
                                        val uri = viewModel.createCameraImageUri(context)
                                        cameraLauncher.launch(uri)
                                    } else {
                                        cameraPermissionLauncher.launch(PermissionsUtil.CAMERA_PERMISSION)
                                    }
                                    showAttachMenu = false
                                }
                            )
                            AttachmentOption(
                                icon = Icons.Default.Videocam,
                                label = "Video",
                                onClick = {
                                    videoPickerLauncher.launch("video/*")
                                    showAttachMenu = false
                                }
                            )
                            AttachmentOption(
                                icon = Icons.Default.InsertDriveFile,
                                label = "Document",
                                onClick = {
                                    documentPickerLauncher.launch("*/*")
                                    showAttachMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Message,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No messages yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Start the conversation!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.surface),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(messages) { message ->
                    MessageItem(message = message)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun AttachmentOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(icon, contentDescription = label)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}