package com.tasteindia.app.ui.details

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tasteindia.app.domain.model.MealDetail
import com.tasteindia.app.ui.components.ErrorState
import com.tasteindia.app.ui.components.LoadingState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailsScreen(
    viewModel: DetailsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState is DetailsUiState.Success) {
                            (uiState as DetailsUiState.Success).mealDetail.name
                        } else {
                            "Recipe Details"
                        },
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("details_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (uiState is DetailsUiState.Success) {
                        val meal = (uiState as DetailsUiState.Success).mealDetail
                        IconButton(
                            onClick = { viewModel.toggleFavourite() },
                            modifier = Modifier.testTag("details_favourite_toggle")
                        ) {
                            Icon(
                                imageVector = if (meal.isFavourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (meal.isFavourite) "Remove from favourites" else "Add to favourites",
                                tint = if (meal.isFavourite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.testTag("details_top_bar")
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is DetailsUiState.Loading -> {
                    LoadingState(
                        contentDescriptionText = "Loading recipe details",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is DetailsUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.retry() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is DetailsUiState.Success -> {
                    val meal = state.mealDetail
                    val scrollState = rememberSaveable(saver = ScrollState.Saver) {
                        ScrollState(0)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .testTag("details_scroll_container")
                    ) {
                        // 1. Hero Image with Coil and fallback
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(meal.thumbUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "${meal.name} hero photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 10f)
                                .testTag("details_hero_image"),
                            loading = {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                                }
                            },
                            error = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Photo not available",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // 2. Name
                            Text(
                                text = meal.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("details_meal_name")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 3. Category/Area metadata row (only if non-empty)
                            val hasMetadata = !meal.category.isNullOrBlank() || !meal.area.isNullOrBlank()
                            if (hasMetadata) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("details_metadata_row"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (!meal.category.isNullOrBlank()) {
                                        SuggestionChip(
                                            onClick = { },
                                            label = { Text("Category: ${meal.category}") },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            ),
                                            modifier = Modifier.testTag("details_meta_category")
                                        )
                                    }
                                    if (!meal.area.isNullOrBlank()) {
                                        SuggestionChip(
                                            onClick = { },
                                            label = { Text("Cuisine: ${meal.area}") },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            ),
                                            modifier = Modifier.testTag("details_meta_area")
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // 4. Tag chips (only if non-empty)
                            val validTags = meal.tags.filter { it.isNotBlank() }
                            if (validTags.isNotEmpty()) {
                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .testTag("details_tags_container"),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    validTags.forEach { tag ->
                                        SuggestionChip(
                                            onClick = { },
                                            label = { Text("#$tag") },
                                            modifier = Modifier.testTag("details_tag_$tag")
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            // 5. Ingredient list (skipping blank rows)
                            val validIngredients = meal.ingredients.filter {
                                it.name.isNotBlank()
                            }
                            if (validIngredients.isNotEmpty()) {
                                Text(
                                    text = "Ingredients",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("details_ingredients_header")
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("details_ingredients_card")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        validIngredients.forEachIndexed { index, ingredient ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("details_ingredient_row_$index"),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "• ${ingredient.name}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (ingredient.measure.isNotBlank()) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = ingredient.measure,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // 6. Instructions with preserved paragraph breaks
                            if (meal.instructions.isNotBlank()) {
                                Text(
                                    text = "Instructions",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("details_instructions_header")
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Normalize newline paragraphs
                                val paragraphs = meal.instructions
                                    .replace("\r\n", "\n")
                                    .split("\n\n", "\n")
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("details_instructions_container"),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    paragraphs.forEachIndexed { pIdx, paragraph ->
                                        Text(
                                            text = paragraph,
                                            style = MaterialTheme.typography.bodyLarge,
                                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.testTag("details_instruction_p_$pIdx")
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                            }

                            // 7. Action buttons (Source link, Video link) validated with Patterns
                            val isSourceValid = isValidWebUrl(meal.sourceUrl)
                            val isVideoValid = isValidWebUrl(meal.videoUrl)

                            if (isSourceValid || isVideoValid) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Text(
                                    text = "External Links",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("details_links_row"),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (isVideoValid && !meal.videoUrl.isNullOrBlank()) {
                                        Button(
                                            onClick = {
                                                openExternalUrl(context, meal.videoUrl)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("details_video_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Watch Video")
                                        }
                                    }

                                    if (isSourceValid && !meal.sourceUrl.isNullOrBlank()) {
                                        OutlinedButton(
                                            onClick = {
                                                openExternalUrl(context, meal.sourceUrl)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("details_source_button")
                                        ) {
                                            Text("View Recipe Source")
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Details optional fields", showBackground = true)
@Composable
fun DetailsScreenMissingOptionalFieldsPreview() {
    DetailsScreenPreviewContent(
        meal = MealDetail(
            id = "preview",
            name = "Dal Tadka",
            thumbUrl = "",
            ingredients = emptyList(),
            instructions = "Heat the pan.\n\nAdd the spices and lentils."
        )
    )
}

@Preview(name = "Details long instructions", showBackground = true)
@Composable
fun DetailsScreenLongInstructionsPreview() {
    DetailsScreenPreviewContent(
        meal = MealDetail(
            id = "preview",
            name = "Hyderabadi Biryani",
            thumbUrl = "",
            category = "Rice",
            area = "Indian",
            instructions = List(8) { index ->
                "Step ${index + 1}: combine the ingredients carefully and cook until the flavours are developed."
            }.joinToString("\n\n")
        )
    )
}

@Composable
private fun DetailsScreenPreviewContent(meal: MealDetail) {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = meal.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (!meal.category.isNullOrBlank() || !meal.area.isNullOrBlank()) {
                Text(
                    text = listOfNotNull(meal.category, meal.area).joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Instructions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = meal.instructions,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

private fun isValidWebUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    val trimmed = url.trim()
    return (trimmed.startsWith("http://") || trimmed.startsWith("https://")) &&
            Patterns.WEB_URL.matcher(trimmed).matches()
}

private fun openExternalUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url.trim()))
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
    }
}
