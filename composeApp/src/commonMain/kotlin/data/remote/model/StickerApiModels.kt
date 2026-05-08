package data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class BackgroundRemoveData(
    val image: ApiImage
)

@Serializable
data class GenerateData(
    val images: List<ApiImage>
)

@Serializable
data class GridSplitData(
    val images: List<ApiImage>
)
