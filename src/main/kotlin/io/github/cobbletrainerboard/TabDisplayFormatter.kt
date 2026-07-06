package io.github.cobbletrainerboard

import net.minecraft.server.network.ServerPlayerEntity

object TabDisplayFormatter {
    fun shinyDisplay(stats: CobblemonStatsResolver.PlayerStats): String {
        val tab = ProgressConfig.get().tab
        return if (tab.shinyDisplayMode == "by_type") {
            val base = "C:${stats.shiny.common} R:${stats.shiny.rare} E:${stats.shiny.epic}"
            if (tab.includeRadiant) "$base Rad:${stats.shiny.radiant}" else base
        } else {
            stats.shiny.total.toString()
        }
    }

    fun tabLine(player: ServerPlayerEntity): String {
        val stats = CobblemonStatsResolver.resolve(player)
        val progress = ProgressResolver.resolve(player)
        val tab = ProgressConfig.get().tab
        val template = if (tab.shinyDisplayMode == "by_type") tab.formatByType else tab.formatTotal
        return applyTokens(template, player, stats, progress)
    }

    private fun applyTokens(
        template: String,
        player: ServerPlayerEntity,
        stats: CobblemonStatsResolver.PlayerStats,
        progress: ResolvedProgress
    ): String {
        val lastBadge = ProgressResolver.lastBadge(player)?.badge ?: "Aucun"
        val shinyDisplay = shinyDisplay(stats)
        return template
            .replace("%player%", player.name.string)
            .replace("%shiny_display%", shinyDisplay)
            .replace("%shiny_total%", stats.shiny.total.toString())
            .replace("%shiny_common%", stats.shiny.common.toString())
            .replace("%shiny_rare%", stats.shiny.rare.toString())
            .replace("%shiny_epic%", stats.shiny.epic.toString())
            .replace("%shiny_radiant%", if (ProgressConfig.get().tab.includeRadiant) stats.shiny.radiant.toString() else "0")
            .replace("%progress%", progress.longText)
            .replace("%progress_short%", progress.shortText)
            .replace("%region%", progress.region)
            .replace("%completed_gyms%", progress.completedGyms.toString())
            .replace("%required_gyms%", progress.requiredGyms.toString())
            .replace("%last_badge%", lastBadge)
            .replace("%dex_seen%", stats.dex.seen.toString())
            .replace("%dex_caught%", stats.dex.caught.toString())
    }
}
