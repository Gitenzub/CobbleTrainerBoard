package io.github.cobbletrainerboard

import net.minecraft.server.network.ServerPlayerEntity
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object CobblemonStatsResolver {
    private const val CACHE_MS: Long = 2_000L
    private val logger = LoggerFactory.getLogger("cobbletrainerboard")

    data class ShinyCounts(
        val total: Int = 0,
        val common: Int = 0,
        val rare: Int = 0,
        val epic: Int = 0,
        val radiant: Int = 0,
        val details: List<ShinyDetail> = emptyList(),
        val source: String = "cobblemon-runtime"
    )

    data class ShinyDetail(
        val species: String,
        val level: Int?,
        val category: String,
        val radiant: Boolean,
        val aspects: List<String>
    )

    data class DexCounts(
        val seen: Int = 0,
        val caught: Int = 0,
        val source: String = "cobblemon-pokedex-runtime"
    )

    data class PlayerStats(
        val shiny: ShinyCounts = ShinyCounts(),
        val dex: DexCounts = DexCounts()
    )

    private data class CacheEntry(
        val timestamp: Long,
        val value: PlayerStats
    )

    private val cache = ConcurrentHashMap<UUID, CacheEntry>()
    private var lastReflectionWarningAt = 0L

    fun clearCache() {
        cache.clear()
    }

    fun resolve(player: ServerPlayerEntity): PlayerStats {
        val now = System.currentTimeMillis()
        val cached = cache[player.uuid]
        if (cached != null && now - cached.timestamp <= CACHE_MS) {
            return cached.value
        }

        val value = PlayerStats(
            shiny = resolveShiny(player),
            dex = resolveDex(player)
        )
        cache[player.uuid] = CacheEntry(now, value)
        return value
    }

    private fun resolveShiny(player: ServerPlayerEntity): ShinyCounts {
        val pokemon = getStoredPokemon(player)
        if (pokemon.isEmpty()) return ShinyCounts()

        var common = 0
        var rare = 0
        var epic = 0
        var radiant = 0
        val details = mutableListOf<ShinyDetail>()

        for (poke in pokemon) {
            if (!readBoolean(poke, listOf("getShiny", "isShiny"), listOf("shiny"))) continue

            val aspects = readStringCollection(poke, listOf("getAspects"), listOf("aspects"))
                .map { it.lowercase() }
                .distinct()
                .sorted()

            val hasEpic = aspects.any { it == "epic" || it.contains("epic") }
            val hasRare = aspects.any { it == "rare" || it.contains("rare") }
            val hasRadiant = aspects.any { it == "radiant" || it.contains("radiant") }

            val category = when {
                hasEpic -> "epic"
                hasRare -> "rare"
                else -> "common"
            }

            when (category) {
                "epic" -> epic++
                "rare" -> rare++
                else -> common++
            }
            if (hasRadiant) radiant++

            details.add(
                ShinyDetail(
                    species = readSpeciesName(poke),
                    level = readInt(poke, listOf("getLevel"), listOf("level")),
                    category = category,
                    radiant = hasRadiant,
                    aspects = aspects
                )
            )
        }

        return ShinyCounts(
            total = common + rare + epic,
            common = common,
            rare = rare,
            epic = epic,
            radiant = radiant,
            details = details.sortedWith(compareBy<ShinyDetail> { it.category }.thenBy { it.species })
        )
    }

    private fun resolveDex(player: ServerPlayerEntity): DexCounts {
        val manager = getPokedexManager(player) ?: return DexCounts(source = "unavailable")
        val records = readMap(manager, listOf("getSpeciesRecords"), listOf("speciesRecords")) ?: return DexCounts(source = "unavailable")

        var seen = 0
        var caught = 0

        for (record in records.values) {
            val knowledge = invokeNoArg(record, listOf("getKnowledge"))?.toString()?.uppercase() ?: continue
            val ordinal = readEnumOrdinal(invokeNoArg(record, listOf("getKnowledge")))

            val isSeen = when {
                ordinal != null -> ordinal >= 1
                knowledge == "SEEN" || knowledge == "OWNED" || knowledge == "ENCOUNTERED" || knowledge == "CAUGHT" -> true
                else -> false
            }
            val isCaught = when {
                ordinal != null -> ordinal >= 2
                knowledge == "OWNED" || knowledge == "CAUGHT" -> true
                else -> false
            }

            if (isSeen) seen++
            if (isCaught) caught++
        }

        return DexCounts(seen = seen, caught = caught)
    }

    private fun getStoredPokemon(player: ServerPlayerEntity): List<Any> {
        val cobblemon = getKotlinObject("com.cobblemon.mod.common.Cobblemon") ?: return emptyList()
        val storage = invokeNoArg(cobblemon, listOf("getStorage")) ?: readField(cobblemon, listOf("storage")) ?: return emptyList()

        val result = mutableListOf<Any>()
        listOf("getParty", "getPC").forEach { methodName ->
            val store = invokeOneArg(storage, listOf(methodName), player)
            if (store is Iterable<*>) {
                store.filterNotNull().forEach { result.add(it) }
            }
        }
        return result.distinctBy { readAny(it, listOf("getUuid", "getUUID"), listOf("uuid"))?.toString() ?: System.identityHashCode(it).toString() }
    }

    private fun getPokedexManager(player: ServerPlayerEntity): Any? {
        val cobblemon = getKotlinObject("com.cobblemon.mod.common.Cobblemon") ?: return null
        val playerDataManager = invokeNoArg(cobblemon, listOf("getPlayerDataManager"))
            ?: readField(cobblemon, listOf("playerDataManager"))
            ?: return null
        return invokeOneArg(playerDataManager, listOf("getPokedexData"), player)
            ?: invokeOneArg(playerDataManager, listOf("getPokedexData"), player.uuid)
    }

    private fun readSpeciesName(pokemon: Any): String {
        val species = readAny(pokemon, listOf("getSpecies"), listOf("species")) ?: return "unknown"
        val identifier = readAny(species, listOf("getResourceIdentifier", "getIdentifier"), listOf("resourceIdentifier", "identifier"))
        if (identifier != null) return identifier.toString().substringAfter(":")
        val name = readAny(species, listOf("getName"), listOf("name"))
        return name?.toString() ?: species.toString().substringAfterLast('.')
    }

    private fun getKotlinObject(className: String): Any? = runCatching {
        val clazz = Class.forName(className)
        clazz.fields.firstOrNull { it.name == "INSTANCE" }?.get(null)
    }.getOrElse {
        warnReflection("Impossible de trouver $className: ${it.message}")
        null
    }

    private fun invokeNoArg(target: Any?, names: List<String>): Any? {
        if (target == null) return null
        for (name in names) {
            val method = target.javaClass.methods.firstOrNull { it.name == name && it.parameterCount == 0 }
            if (method != null) {
                return runCatching { method.invoke(target) }.getOrNull()
            }
        }
        return null
    }

    private fun invokeOneArg(target: Any?, names: List<String>, arg: Any): Any? {
        if (target == null) return null
        for (name in names) {
            val methods = target.javaClass.methods.filter { it.name == name && it.parameterCount == 1 }
            for (method in methods) {
                val param = method.parameterTypes[0]
                if (param.isAssignableFrom(arg.javaClass) || param.name.startsWith("net.minecraft.")) {
                    val value = runCatching { method.invoke(target, arg) }.getOrNull()
                    if (value != null) return value
                }
            }
            for (method in methods) {
                val value = runCatching { method.invoke(target, arg) }.getOrNull()
                if (value != null) return value
            }
        }
        return null
    }

    private fun readAny(target: Any?, methodNames: List<String>, fieldNames: List<String>): Any? {
        return invokeNoArg(target, methodNames) ?: readField(target, fieldNames)
    }

    private fun readBoolean(target: Any?, methodNames: List<String>, fieldNames: List<String>): Boolean {
        val value = readAny(target, methodNames, fieldNames)
        return when (value) {
            is Boolean -> value
            is String -> value.equals("true", ignoreCase = true)
            else -> false
        }
    }

    private fun readInt(target: Any?, methodNames: List<String>, fieldNames: List<String>): Int? {
        val value = readAny(target, methodNames, fieldNames)
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }
    }

    private fun readStringCollection(target: Any?, methodNames: List<String>, fieldNames: List<String>): List<String> {
        val value = readAny(target, methodNames, fieldNames)
        return when (value) {
            is Iterable<*> -> value.mapNotNull { it?.toString() }
            is Array<*> -> value.mapNotNull { it?.toString() }
            is String -> listOf(value)
            else -> emptyList()
        }
    }

    private fun readMap(target: Any?, methodNames: List<String>, fieldNames: List<String>): Map<*, *>? {
        val value = readAny(target, methodNames, fieldNames)
        return value as? Map<*, *>
    }

    private fun readEnumOrdinal(value: Any?): Int? {
        if (value == null) return null
        val ordinal = invokeNoArg(value, listOf("ordinal"))
        return (ordinal as? Number)?.toInt()
    }

    private fun readField(target: Any?, names: List<String>): Any? {
        if (target == null) return null
        for (name in names) {
            var clazz: Class<*>? = target.javaClass
            while (clazz != null) {
                val field = clazz.declaredFields.firstOrNull { it.name == name }
                if (field != null) {
                    return runCatching {
                        field.isAccessible = true
                        field.get(target)
                    }.getOrNull()
                }
                clazz = clazz.superclass
            }
        }
        return null
    }

    private fun warnReflection(message: String) {
        val now = System.currentTimeMillis()
        if (now - lastReflectionWarningAt > 30_000L) {
            lastReflectionWarningAt = now
            logger.warn("[CobbleTrainerBoard] $message")
        }
    }
}
