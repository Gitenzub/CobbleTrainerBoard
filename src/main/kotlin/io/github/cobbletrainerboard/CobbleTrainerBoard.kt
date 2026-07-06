package io.github.cobbletrainerboard

import com.mojang.brigadier.arguments.StringArgumentType
import eu.pb4.placeholders.api.PlaceholderHandler
import eu.pb4.placeholders.api.PlaceholderResult
import eu.pb4.placeholders.api.Placeholders
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

object CobbleTrainerBoard : ModInitializer {
    const val MOD_ID: String = "cobbletrainerboard"
    private val logger = LoggerFactory.getLogger(MOD_ID)

    override fun onInitialize() {
        ProgressConfig.get()
        registerPlaceholders()
        registerCommands()
        logger.info("CobbleTrainerBoard initialized: gym, shiny and pokedex placeholders registered.")
    }

    private fun registerPlaceholders() {
        registerPlayerPlaceholder("progress") { player ->
            Text.literal(ProgressResolver.resolve(player).longText)
        }
        registerPlayerPlaceholder("progress_short") { player ->
            Text.literal(ProgressResolver.resolve(player).shortText)
        }
        registerPlayerPlaceholder("region") { player ->
            Text.literal(ProgressResolver.resolve(player).region)
        }
        registerPlayerPlaceholder("progress_key") { player ->
            Text.literal(ProgressResolver.resolve(player).key)
        }
        registerPlayerPlaceholder("progress_index") { player ->
            Text.literal(ProgressResolver.resolve(player).index.toString())
        }
        registerPlayerPlaceholder("completed_gyms") { player ->
            Text.literal(ProgressResolver.resolve(player).completedGyms.toString())
        }
        registerPlayerPlaceholder("required_gyms") { player ->
            Text.literal(ProgressResolver.resolve(player).requiredGyms.toString())
        }
        registerPlayerPlaceholder("last_badge") { player ->
            Text.literal(ProgressResolver.lastBadge(player)?.badge ?: "Aucun")
        }
        registerPlayerPlaceholder("debug") { player ->
            Text.literal(ProgressResolver.resolve(player).debugText)
        }

        registerPlayerPlaceholder("shiny_total") { player ->
            Text.literal(CobblemonStatsResolver.resolve(player).shiny.total.toString())
        }
        registerPlayerPlaceholder("shiny_common") { player ->
            Text.literal(CobblemonStatsResolver.resolve(player).shiny.common.toString())
        }
        registerPlayerPlaceholder("shiny_rare") { player ->
            Text.literal(CobblemonStatsResolver.resolve(player).shiny.rare.toString())
        }
        registerPlayerPlaceholder("shiny_epic") { player ->
            Text.literal(CobblemonStatsResolver.resolve(player).shiny.epic.toString())
        }
        registerPlayerPlaceholder("shiny_radiant") { player ->
            Text.literal(CobblemonStatsResolver.resolve(player).shiny.radiant.toString())
        }
        registerPlayerPlaceholder("shiny_display") { player ->
            Text.literal(TabDisplayFormatter.shinyDisplay(CobblemonStatsResolver.resolve(player)))
        }
        registerPlayerPlaceholder("dex_seen") { player ->
            Text.literal(CobblemonStatsResolver.resolve(player).dex.seen.toString())
        }
        registerPlayerPlaceholder("dex_caught") { player ->
            Text.literal(CobblemonStatsResolver.resolve(player).dex.caught.toString())
        }
        registerPlayerPlaceholder("tab_line") { player ->
            Text.literal(TabDisplayFormatter.tabLine(player))
        }
    }

    private fun registerPlayerPlaceholder(name: String, provider: (ServerPlayerEntity) -> Text) {
        Placeholders.register(Identifier.of(MOD_ID, name), PlaceholderHandler { ctx, _ ->
            val player = ctx.player()
            if (player == null) {
                PlaceholderResult.invalid("No server player")
            } else {
                PlaceholderResult.value(provider(player))
            }
        })
    }

    private fun registerCommands() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                CommandManager.literal("ctb")
                    .executes { context -> sendHelp(context.source); 1 }
                    .then(CommandManager.literal("help").executes { context -> sendHelp(context.source); 1 })
                    .then(
                        CommandManager.literal("progress")
                            .executes { context ->
                                val player = context.source.getPlayerOrThrow()
                                sendProgress(context.source, player)
                                1
                            }
                            .then(playerArgument { source, target -> sendProgress(source, target) })
                    )
                    .then(
                        CommandManager.literal("gym")
                            .executes { context ->
                                val player = context.source.getPlayerOrThrow()
                                sendGym(context.source, player)
                                1
                            }
                            .then(playerArgument { source, target -> sendGym(source, target) })
                    )
                    .then(
                        CommandManager.literal("shiny")
                            .executes { context ->
                                val player = context.source.getPlayerOrThrow()
                                sendShiny(context.source, player)
                                1
                            }
                            .then(playerArgument { source, target -> sendShiny(source, target) })
                    )
                    .then(
                        CommandManager.literal("shinydetail")
                            .requires { source -> source.hasPermissionLevel(2) }
                            .executes { context ->
                                val player = context.source.getPlayerOrThrow()
                                sendShinyDetail(context.source, player)
                                1
                            }
                            .then(playerArgument { source, target -> sendShinyDetail(source, target) })
                    )
                    .then(
                        CommandManager.literal("dex")
                            .then(
                                CommandManager.literal("view")
                                    .executes { context ->
                                        val player = context.source.getPlayerOrThrow()
                                        sendDexSeen(context.source, player)
                                        1
                                    }
                                    .then(playerArgument { source, target -> sendDexSeen(source, target) })
                            )
                            .then(
                                CommandManager.literal("seen")
                                    .executes { context ->
                                        val player = context.source.getPlayerOrThrow()
                                        sendDexSeen(context.source, player)
                                        1
                                    }
                                    .then(playerArgument { source, target -> sendDexSeen(source, target) })
                            )
                            .then(
                                CommandManager.literal("catch")
                                    .executes { context ->
                                        val player = context.source.getPlayerOrThrow()
                                        sendDexCaught(context.source, player)
                                        1
                                    }
                                    .then(playerArgument { source, target -> sendDexCaught(source, target) })
                            )
                            .then(
                                CommandManager.literal("caught")
                                    .executes { context ->
                                        val player = context.source.getPlayerOrThrow()
                                        sendDexCaught(context.source, player)
                                        1
                                    }
                                    .then(playerArgument { source, target -> sendDexCaught(source, target) })
                            )
                    )
                    .then(
                        CommandManager.literal("debug")
                            .requires { source -> source.hasPermissionLevel(2) }
                            .executes { context ->
                                val player = context.source.getPlayerOrThrow()
                                sendDebug(context.source, player)
                                1
                            }
                            .then(playerArgument { source, target -> sendDebug(source, target) })
                    )
                    .then(
                        CommandManager.literal("advancements")
                            .requires { source -> source.hasPermissionLevel(2) }
                            .executes { context ->
                                val player = context.source.getPlayerOrThrow()
                                sendAdvancements(context.source, player)
                                1
                            }
                            .then(playerArgument { source, target -> sendAdvancements(source, target) })
                    )
                    .then(
                        CommandManager.literal("reload")
                            .requires { source -> source.hasPermissionLevel(2) }
                            .executes { context ->
                                ProgressConfig.reload()
                                ProgressResolver.clearCache()
                                CobblemonStatsResolver.clearCache()
                                ProgressConfig.get()
                                context.source.sendMessage(Text.literal("CobbleTrainerBoard config et caches rechargés."))
                                1
                            }
                    )
            )
        }
    }

    private fun playerArgument(action: (ServerCommandSource, ServerPlayerEntity) -> Unit) =
        CommandManager.argument("player", StringArgumentType.word())
            .executes { context ->
                val name = StringArgumentType.getString(context, "player")
                val target = context.source.server.playerManager.getPlayer(name)
                if (target == null) {
                    context.source.sendError(Text.literal("Joueur introuvable ou hors ligne: $name"))
                    0
                } else {
                    action(context.source, target)
                    1
                }
            }

    private fun sendHelp(source: ServerCommandSource) {
        source.sendMessage(Text.literal("§6CobbleTrainerBoard commands"))
        source.sendMessage(Text.literal("§e/ctb shiny [joueur]§7 - total shiny + commun/rare/epic/radiant"))
        source.sendMessage(Text.literal("§e/ctb shinydetail [joueur]§7 - détail des shiny (admin)"))
        source.sendMessage(Text.literal("§e/ctb gym [joueur]§7 - dernier badge selon l'ordre des régions"))
        source.sendMessage(Text.literal("§e/ctb dex view [joueur]§7 - Pokémon vus dans le Pokédex"))
        source.sendMessage(Text.literal("§e/ctb dex catch [joueur]§7 - Pokémon attrapés dans le Pokédex"))
        source.sendMessage(Text.literal("§e/ctb progress [joueur]§7 - progression badge actuelle"))
        source.sendMessage(Text.literal("§e/ctb reload§7 - recharge la config et les caches (admin)"))
        source.sendMessage(Text.literal("§7TAB: configure config/cobbletrainerboard.json -> tab.shiny_display_mode = total ou by_type"))
    }

    private fun sendProgress(source: ServerCommandSource, player: ServerPlayerEntity) {
        val progress = ProgressResolver.resolve(player)
        source.sendMessage(Text.literal("${player.name.string}: ${progress.shortText}"))
        source.sendMessage(Text.literal(progress.debugText))
    }

    private fun sendGym(source: ServerCommandSource, player: ServerPlayerEntity) {
        val progress = ProgressResolver.resolve(player)
        val last = ProgressResolver.lastBadge(player)
        if (last == null) {
            source.sendMessage(Text.literal("${player.name.string}: aucun badge obtenu. Progression actuelle: ${progress.shortText}"))
        } else {
            source.sendMessage(Text.literal("${player.name.string}: dernier badge = ${last.badge} (${last.region} ${last.completedGyms}/${last.requiredGyms}). Progression actuelle: ${progress.shortText}"))
        }
    }

    private fun sendShiny(source: ServerCommandSource, player: ServerPlayerEntity) {
        val shiny = CobblemonStatsResolver.resolve(player).shiny
        source.sendMessage(Text.literal("${player.name.string}: ${shiny.total} shiny | Commun ${shiny.common} | Rare ${shiny.rare} | Epic ${shiny.epic} | Radiant ${shiny.radiant}"))
        source.sendMessage(Text.literal("Note: Radiant est compté comme bonus visuel séparé; il ne s'ajoute pas au total."))
    }

    private fun sendShinyDetail(source: ServerCommandSource, player: ServerPlayerEntity) {
        val shiny = CobblemonStatsResolver.resolve(player).shiny
        source.sendMessage(Text.literal("${player.name.string}: ${shiny.total} shiny | Commun ${shiny.common} | Rare ${shiny.rare} | Epic ${shiny.epic} | Radiant ${shiny.radiant}"))
        if (shiny.details.isEmpty()) {
            source.sendMessage(Text.literal("Aucun shiny trouvé dans la party/PC."))
            return
        }
        shiny.details.take(80).forEachIndexed { index, detail ->
            val level = detail.level?.let { " niv.$it" } ?: ""
            val radiant = if (detail.radiant) " + radiant" else ""
            val aspects = if (detail.aspects.isEmpty()) "-" else detail.aspects.joinToString(",")
            source.sendMessage(Text.literal("${index + 1}. ${detail.species}$level | ${detail.category}$radiant | aspects: $aspects"))
        }
        if (shiny.details.size > 80) {
            source.sendMessage(Text.literal("... ${shiny.details.size - 80} shiny supplémentaires masqués."))
        }
    }

    private fun sendDexSeen(source: ServerCommandSource, player: ServerPlayerEntity) {
        val dex = CobblemonStatsResolver.resolve(player).dex
        source.sendMessage(Text.literal("${player.name.string}: ${dex.seen} Pokémon vus dans le Pokédex."))
    }

    private fun sendDexCaught(source: ServerCommandSource, player: ServerPlayerEntity) {
        val dex = CobblemonStatsResolver.resolve(player).dex
        source.sendMessage(Text.literal("${player.name.string}: ${dex.caught} Pokémon attrapés dans le Pokédex."))
    }

    private fun sendDebug(source: ServerCommandSource, player: ServerPlayerEntity) {
        ProgressResolver.debugRegions(player).forEach { region ->
            val gyms = if (region.completedGymNames.isEmpty()) "-" else region.completedGymNames.joinToString(", ")
            source.sendMessage(Text.literal("${player.name.string} | ${region.display}: ${region.completed}/${region.required} -> $gyms"))
        }
        val stats = CobblemonStatsResolver.resolve(player)
        source.sendMessage(Text.literal("${player.name.string} | Shiny ${stats.shiny.total} (C ${stats.shiny.common}, R ${stats.shiny.rare}, E ${stats.shiny.epic}, Rad ${stats.shiny.radiant}) | Dex vus ${stats.dex.seen}, attrapés ${stats.dex.caught}"))
        source.sendMessage(Text.literal("TAB mode: ${ProgressConfig.get().tab.shinyDisplayMode} | ${TabDisplayFormatter.tabLine(player)}"))
    }

    private fun sendAdvancements(source: ServerCommandSource, player: ServerPlayerEntity) {
        val ids = ProgressResolver.completedRelevantAdvancementIds(player)
        source.sendMessage(Text.literal("${player.name.string}: advancements complets probablement liés aux gyms/badges: ${ids.size}"))
        ids.take(60).forEach { id -> source.sendMessage(Text.literal("- $id")) }
        if (ids.size > 60) {
            source.sendMessage(Text.literal("... ${ids.size - 60} autres masqués"))
        }
    }
}
