package presentation

/** Initial count bounds; navigation passes only the selected count, not Parcelable storage models. */
data class SelectTokensNumberAlertData(
    val minTokensNum: Int,
    val maxTokensNum: Int,
    val currentTokensNum: Int,
)
