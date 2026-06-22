package data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class GenerateData(
    val images: List<ApiImage>
)

@Serializable
data class GridSplitTextAssetsData(
    val assets: List<ApiTextAsset>
)

@Serializable
data class ApiTextAsset(
    val id: String,
    val textAssetDecoration: ApiTextAssetDecoration? = null,
    val textOutsideForeground: ApiTextOutsideForeground? = null
)

@Serializable
data class ApiTextAssetDecoration(
    val text: String? = null,
    val style: ApiTextOutsideForegroundStyle? = null,
    val source: String? = null
)
