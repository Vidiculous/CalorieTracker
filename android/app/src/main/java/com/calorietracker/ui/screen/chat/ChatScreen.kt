package com.calorietracker.ui.screen.chat

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorietracker.data.model.ChatMessage
import com.calorietracker.ui.component.*
import com.calorietracker.ui.navigation.Screen
import com.calorietracker.ui.theme.*
import com.calorietracker.util.ImageUtils
import java.io.File

private fun base64ToBitmap(base64: String): Bitmap? = try {
    val bytes = Base64.decode(base64, Base64.NO_WRAP)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
} catch (e: Exception) {
    null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    initialMode: String,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val micPermissionGranted by remember { mutableStateOf(false) }
    val micPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && initialMode == "voice") viewModel.startRecording(context)
    }
    val cameraPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showImageSourceDialog = true
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val base64 = ImageUtils.uriToBase64(context, it)
            base64?.let { b64 -> viewModel.setPendingImage(b64) }
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            photoUri?.let { uri ->
                val base64 = ImageUtils.uriToBase64(context, uri)
                base64?.let { b64 -> viewModel.setPendingImage(b64) }
            }
        }
    }

    LaunchedEffect(initialMode) {
        when (initialMode) {
            "voice" -> micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
            "camera" -> showImageSourceDialog = true
        }
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    // Handle transcript (non-autosubmit path)
    LaunchedEffect(state.messages) {
        val last = state.messages.lastOrNull()
        if (last?.role == "transcript") {
            inputText = last.content
        }
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            containerColor = Neutral800,
            title = { Text("Add Image", color = Color.White, fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            showImageSourceDialog = false
                            val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            photoUri = uri
                            takePictureLauncher.launch(uri)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Rose500, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Take Photo", color = Color.White)
                    }
                    TextButton(
                        onClick = {
                            showImageSourceDialog = false
                            imagePickerLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Photo, contentDescription = null, tint = Neutral400, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Choose from Gallery", color = Color.White)
                    }
                }
            },
            confirmButton = {}
        )
    }

    Scaffold(
        containerColor = Neutral900,
        topBar = {
            TopAppBar(
                title = { Text("Food Tracker", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::clearChat) {
                        Text("Clear Chat", color = Neutral400, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Neutral900,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            // Messages list
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(state.messages.filter { it.role != "transcript" }, key = { it.id }) { message ->
                    MessageBubble(message)
                }

                if (state.isLoading) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Neutral800),
                                shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
                            ) {
                                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    repeat(3) { i ->
                                        val infiniteTransition = rememberInfiniteTransition(label = "dot_$i")
                                        val alpha by infiniteTransition.animateFloat(
                                            0.3f, 1f,
                                            animationSpec = infiniteRepeatable(tween(500, delayMillis = i * 160), RepeatMode.Reverse),
                                            label = "dot_alpha"
                                        )
                                        Box(Modifier.size(6.dp).background(Neutral400.copy(alpha = alpha), androidx.compose.foundation.shape.CircleShape))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Pending image preview
            state.pendingImageBase64?.let { base64 ->
                val bitmap = remember(base64) { base64ToBitmap(base64) }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Attached image",
                            modifier = Modifier.size(48.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Attached Image", color = Neutral400, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = viewModel::clearPendingImage) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Neutral400)
                    }
                }
            }

            // Bottom input area
            ChatInputBar(
                inputText = inputText,
                onInputChange = { inputText = it },
                isRecording = state.isRecording,
                isTranscribing = state.isTranscribing,
                audioLevel = state.audioLevel,
                partialTranscript = state.partialTranscript,
                autoSubmit = state.settings.autoSubmit,
                hasPendingImage = state.pendingImageBase64 != null,
                onSend = {
                    if (inputText.isNotBlank() || state.pendingImageBase64 != null) {
                        viewModel.sendText(inputText, state.pendingImageBase64)
                        inputText = ""
                    }
                },
                onCameraClick = { showImageSourceDialog = true },
                onRecipesClick = { onNavigate(Screen.RecipeList.route) },
                onBarcodeClick = { onNavigate(Screen.BarcodeScanner.route) },
                onMicClick = {
                    if (state.isRecording) {
                        viewModel.stopRecording()
                    } else {
                        micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        viewModel.startRecording(context)
                    }
                },
                onAutoSubmitToggle = { /* viewModel.toggleAutoSubmit() */ }
            )
        }
    }

    // Confirm log bottom sheet
    state.pendingConfirmItems?.let { items ->
        ConfirmLogSheet(
            items = items,
            mealType = state.pendingConfirmMealType,
            onLogAll = viewModel::confirmLog,
            onDiscard = viewModel::discardConfirm
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            colors = CardDefaults.cardColors(containerColor = if (isUser) Rose500 else Neutral800),
            shape = if (isUser)
                RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
            else
                RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                message.imageBase64?.let { b64 ->
                    val bitmap = remember(b64) { base64ToBitmap(b64) }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }
                if (message.content.isNotBlank()) {
                    Text(
                        text = message.content,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    isRecording: Boolean,
    isTranscribing: Boolean,
    audioLevel: Float,
    partialTranscript: String,
    autoSubmit: Boolean,
    hasPendingImage: Boolean,
    onSend: () -> Unit,
    onCameraClick: () -> Unit,
    onRecipesClick: () -> Unit,
    onBarcodeClick: () -> Unit,
    onMicClick: () -> Unit,
    onAutoSubmitToggle: () -> Unit
) {
    Surface(
        color = Neutral800,
        tonalElevation = 4.dp
    ) {
        Column {
            if (isRecording) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AudioLevelIndicator(audioLevel = audioLevel)
                    Spacer(Modifier.width(12.dp))
                    Text("Listening... (tap to stop)", color = Neutral400, fontSize = 13.sp)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onCameraClick, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = Neutral400)
                }
                IconButton(onClick = onRecipesClick, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Recipes", tint = Neutral400)
                }
                IconButton(onClick = onBarcodeClick, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Barcode", tint = Neutral400)
                }

                OutlinedTextField(
                    value = when {
                        isRecording -> partialTranscript.ifEmpty { "Listening..." }
                        isTranscribing -> "Processing..."
                        else -> inputText
                    },
                    onValueChange = if (!isRecording && !isTranscribing) onInputChange else { _ -> },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = if (hasPendingImage) "Ask about this image..." else "Type, paste URL, or speak...",
                            color = Neutral500, fontSize = 13.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Rose500,
                        unfocusedBorderColor = Neutral700,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Rose500,
                        focusedContainerColor = Neutral900,
                        unfocusedContainerColor = Neutral900
                    ),
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    singleLine = true,
                    enabled = !isRecording && !isTranscribing
                )

                if (inputText.isNotBlank() || hasPendingImage) {
                    IconButton(
                        onClick = onSend,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Rose500, androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                } else {
                    IconButton(onClick = onMicClick, modifier = Modifier.size(40.dp)) {
                        Icon(
                            if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecording) "Stop" else "Record",
                            tint = if (isRecording) Red500 else Rose500
                        )
                    }
                }
            }
        }
    }
}
