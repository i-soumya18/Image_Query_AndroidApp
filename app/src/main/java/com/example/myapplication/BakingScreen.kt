package com.example.myapplication

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

val images = arrayOf(
    // Image generated using Gemini from the prompt "cupcake image"
    R.drawable.baked_goods_1,
    // Image generated using Gemini from the prompt "cookies images"
    R.drawable.baked_goods_2,
    // Image generated using Gemini from the prompt "cake images"
    R.drawable.baked_goods_3,
)
val imageDescriptions = arrayOf(
    R.string.image1_description,
    R.string.image2_description,
    R.string.image3_description,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BakingScreen(
    bakingViewModel: BakingViewModel = viewModel()
) {
    val selectedImage = remember { mutableIntStateOf(0) }
    val placeholderPrompt = stringResource(R.string.prompt_placeholder)
    val placeholderResult = stringResource(R.string.results_placeholder)
    var prompt by rememberSaveable { mutableStateOf(placeholderPrompt) }
    var result by rememberSaveable { mutableStateOf(placeholderResult) }
    val uiState by bakingViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Launchers for taking a picture and selecting an image from the gallery
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        selectedBitmap = bitmap
    }
    val selectImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            selectedBitmap = BitmapFactory.decodeStream(inputStream)
        }
    }

    // Animation for the selected image
    val imageRotation by animateFloatAsState(
        targetValue = if (selectedBitmap != null) 15f else 0f,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF8E2DE2),
                        Color(0xFF4A00E0)
                    )
                )
            )
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.baking_title),
            style = MaterialTheme.typography.titleLarge.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            ),
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.CenterHorizontally)
        )

        // Show the selected image or the currently chosen image from LazyRow
        selectedBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Selected Image",
                modifier = Modifier
                    .size(250.dp)
                    .padding(16.dp)
                    .graphicsLayer {
                        rotationX = imageRotation
                        rotationY = imageRotation
                        scaleX = 1.1f
                        scaleY = 1.1f
                    }
                    .shadow(8.dp, RoundedCornerShape(16.dp))
            )
        } ?: run {
            // Show the image currently selected from LazyRow (default images)
            Image(
                painter = painterResource(id = images[selectedImage.intValue]),
                contentDescription = stringResource(imageDescriptions[selectedImage.intValue]),
                modifier = Modifier
                    .size(250.dp)
                    .padding(16.dp)
                    .graphicsLayer {
                        rotationX = imageRotation
                        rotationY = imageRotation
                        scaleX = 1.1f
                        scaleY = 1.1f
                    }
                    .shadow(8.dp, RoundedCornerShape(16.dp))
            )
        }

        Row(
            modifier = Modifier.padding(all = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimatedButton(onClick = { takePictureLauncher.launch(null) }, text = "Take Picture")
            AnimatedButton(onClick = { selectImageLauncher.launch("image/*") }, text = "Select from Gallery")
        }

        Row(
            modifier = Modifier.padding(all = 16.dp)
        ) {
            TextField(
                value = prompt,
                label = { Text(stringResource(R.string.label_prompt)) },
                onValueChange = { prompt = it },
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color(0xFF42099E),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,  // Text color when focused
                    unfocusedTextColor = Color.White, // Text color when unfocused
                    focusedLabelColor = Color.White
                ),
                modifier = Modifier
                    .weight(0.8f)
                    .padding(end = 16.dp)
                    .align(Alignment.CenterVertically)
            )

            AnimatedButton(
                onClick = {
                    val bitmap = selectedBitmap ?: BitmapFactory.decodeResource(
                        context.resources,
                        images[selectedImage.intValue]
                    )
                    bakingViewModel.sendPrompt(bitmap, prompt)
                },
                text = stringResource(R.string.action_go),
                enabled = prompt.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
            )
        }

        if (uiState is UiState.Loading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 6.dp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp)
            )
        } else {
            val textColor = when (uiState) {
                is UiState.Error -> Color.Red
                is UiState.Success -> Color.White
                else -> Color.White
            }
            val resultText = when (uiState) {
                is UiState.Error -> (uiState as UiState.Error).errorMessage
                is UiState.Success -> (uiState as UiState.Success).outputText
                else -> placeholderResult
            }
            Text(
                text = resultText,
                textAlign = TextAlign.Start,
                color = textColor,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .background(Color(0x4A000000), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            )
        }
    }
}


@Composable
fun AnimatedButton(onClick: () -> Unit, text: String, enabled: Boolean = true, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1.0f else 0.95f,
        animationSpec = tween(durationMillis = 300)
    )
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = 8.dp.toPx()
            }
            .background(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFFF415DA),
                        Color(0xFF1CC4ED)
                    )
                ),
                shape = RoundedCornerShape(50)
            )
            .padding(8.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}
