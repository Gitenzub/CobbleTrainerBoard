package io.github.cobbletrainerboard

import net.minecraft.server.network.ServerPlayerEntity
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object ProgressResolver {
    private const val CACHE_MS: Long = 2_000L

    private data class CacheEntry(
        val timestamp: Long,
        val value: ResolvedProgress
    )

    data class RegionDebug(
        val display: String,
        val completed: Int,
        val required: Int,
        val completedGymNames: List<String>
    )

    data class LastBadge(
        val region: String,
        val badge: String,
        val completedGyms: Int,
        val requiredGyms: Int
    )

    private val cache = ConcurrentHashMap<UUID, CacheEntry>()

    fun clearCache() {
        cache.clear()
    }

    fun resolve(player: ServerPlayerEntity): ResolvedProgress {
        val now = System.currentTimeMillis()
        val cached = cache[player.uuid]
        if (cached != null && now - cached.timestamp <= CACHE_MS) {
            return cached.value
        }

        val resolved = resolveUncached(player)
        cache[player.uuid] = CacheEntry(now, resolved)
        return resolved
    }

    fun debugRegions(player: ServerPlayerEntity): List<RegionDebug> {
        val config = ProgressConfig.get()
        val completedIds = completedAdvancementIds(player)
        val normalizedCompletedIds = completedIds.map { it.normalizeForMatch() }

        return config.regions.map { region ->
            val completedGymNames = region.gyms
                .filter { gym -> isGymCompleted(config, region, gym, completedIds, normalizedCompletedIds) }
                .map { it.name }

            RegionDebug(
                display = region.display,
                completed = completedGymNames.size.coerceAtMost(region.requiredGyms),
                required = region.requiredGyms,
                completedGymNames = completedGymNames
            )
        }
    }


    fun lastBadge(player: ServerPlayerEntity): LastBadge? {
        val config = ProgressConfig.get()
        val completedIds = completedAdvancementIds(player)
        val normalizedCompletedIds = completedIds.map { it.normalizeForMatch() }

        var last: LastBadge? = null
        for (region in config.regions) {
            var completedInRegion = 0
            for (gym in region.gyms) {
                if (isGymCompleted(config, region, gym, completedIds, normalizedCompletedIds)) {
                    completedInRegion++
                    last = LastBadge(
                        region = region.display,
                        badge = gym.name,
                        completedGyms = completedInRegion.coerceAtMost(region.requiredGyms),
                        requiredGyms = region.requiredGyms
                    )
                }
            }
            if (completedInRegion < region.requiredGyms) {
                break
            }
        }
        return last
    }

    fun completedRelevantAdvancementIds(player: ServerPlayerEntity): List<String> {
        val config = ProgressConfig.get()
        val regionKeys = config.regions.map { it.key }
        return completedAdvancementIds(player)
            .filter { id ->
                val normalized = id.normalizeForMatch()
                looksLikeGymBadgeOrTrial(normalized) || regionKeys.any { normalized.contains(it) }
            }
            .sorted()
    }

    private fun resolveUncached(player: ServerPlayerEntity): ResolvedProgress {
        val config = ProgressConfig.get()
        val completedIds = completedAdvancementIds(player)
        val normalizedCompletedIds = completedIds.map { it.normalizeForMatch() }

        val regionCounts = config.regions.map { region ->
            val completed = region.gyms.count { gym ->
                isGymCompleted(config, region, gym, completedIds, normalizedCompletedIds)
            }.coerceAtMost(region.requiredGyms)
            region to completed
        }

        if (regionCounts.isEmpty()) return ResolvedProgress.UNKNOWN

        var completedBeforeCurrent = 0
        for ((region, completed) in regionCounts) {
            if (completed < region.requiredGyms) {
                return toResolved(region, completed, completedBeforeCurrent + completed, regionCounts)
            }
            completedBeforeCurrent += region.requiredGyms
        }

        val (lastRegion, lastCompleted) = regionCounts.last()
        return toResolved(lastRegion, lastCompleted, completedBeforeCurrent, regionCounts)
    }

    private fun toResolved(
        region: ProgressConfig.RegionRule,
        completed: Int,
        globalIndex: Int,
        allCounts: List<Pair<ProgressConfig.RegionRule, Int>>
    ): ResolvedProgress {
        val short = "${region.display} $completed/${region.requiredGyms}"
        val debug = allCounts.joinToString(" | ") { (rule, count) ->
            "${rule.display}: $count/${rule.requiredGyms}"
        }
        return ResolvedProgress(
            key = "${region.key}_${completed}_of_${region.requiredGyms}",
            region = region.display,
            completedGyms = completed,
            requiredGyms = region.requiredGyms,
            longText = short,
            shortText = short,
            index = globalIndex,
            source = "advancements-only",
            debugText = debug
        )
    }

    private fun completedAdvancementIds(player: ServerPlayerEntity): List<String> {
        val tracker = player.getAdvancementTracker()
        val loader = player.server.getAdvancementLoader()
        return loader.getAdvancements()
            .asSequence()
            .filter { advancement -> tracker.getProgress(advancement).isDone }
            .map { advancement -> advancement.id().toString().lowercase() }
            .toList()
    }

    private fun isGymCompleted(
        config: ProgressConfig.BoardConfig,
        region: ProgressConfig.RegionRule,
        gym: ProgressConfig.GymRule,
        completedIds: List<String>,
        normalizedCompletedIds: List<String>
    ): Boolean {
        if (gym.exactIds.any { exact -> completedIds.any { it == exact.lowercase() } }) {
            return true
        }

        return normalizedCompletedIds.any { id ->
            if (config.requireRegionInMatch && !id.contains(region.key)) {
                return@any false
            }
            if (!looksLikeGymBadgeOrTrial(id)) {
                return@any false
            }
            gym.matchAny.any { token -> id.contains(token) }
        }
    }

    private fun looksLikeGymBadgeOrTrial(id: String): Boolean {
        return id.contains("badge") ||
            id.contains("badges") ||
            id.contains("gym") ||
            id.contains("leader") ||
            id.contains("trial") ||
            id.contains("epreuve") ||
            id.contains("kahuna") ||
            id.contains("cobbleversebadges") ||
            id.contains("trainer/") ||
            id.contains("defeat_")
    }

    private fun String.normalizeForMatch(): String = ProgressConfig.run { this@normalizeForMatch.normalizeToken() }
}
