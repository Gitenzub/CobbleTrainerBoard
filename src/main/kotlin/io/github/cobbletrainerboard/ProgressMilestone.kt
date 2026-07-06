package io.github.cobbletrainerboard

data class ResolvedProgress(
    val key: String,
    val region: String,
    val completedGyms: Int,
    val requiredGyms: Int,
    val longText: String,
    val shortText: String,
    val index: Int,
    val source: String,
    val debugText: String
) {
    companion object {
        val UNKNOWN = ResolvedProgress(
            key = "unknown",
            region = "Kanto",
            completedGyms = 0,
            requiredGyms = 8,
            longText = "Kanto 0/8",
            shortText = "Kanto 0/8",
            index = 0,
            source = "advancements-only",
            debugText = "No matching gym advancements found"
        )
    }
}
