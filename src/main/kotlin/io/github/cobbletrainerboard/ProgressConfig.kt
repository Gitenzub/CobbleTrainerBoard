package io.github.cobbletrainerboard

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.Locale

object ProgressConfig {
    private const val CONFIG_VERSION = 4
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val configPath: Path = Paths.get("config", "cobbletrainerboard.json")
    @Volatile private var cached: BoardConfig? = null

    data class BoardConfig(
        val regions: List<RegionRule>,
        val requireRegionInMatch: Boolean
    )

    data class RegionRule(
        val key: String,
        val display: String,
        val requiredGyms: Int,
        val gyms: List<GymRule>
    )

    data class GymRule(
        val name: String,
        val matchAny: List<String>,
        val exactIds: List<String>
    )

    private data class DefaultGym(
        val name: String,
        val matchAny: List<String>,
        val exactIds: List<String> = emptyList()
    )

    fun get(): BoardConfig {
        val existing = cached
        if (existing != null) return existing

        val loaded = loadOrCreate()
        cached = loaded
        return loaded
    }

    fun reload() {
        cached = null
    }

    private fun loadOrCreate(): BoardConfig {
        if (!Files.exists(configPath)) {
            Files.createDirectories(configPath.parent)
            Files.writeString(configPath, defaultConfigJson(), StandardCharsets.UTF_8)
        }

        var json = runCatching {
            JsonParser.parseString(Files.readString(configPath, StandardCharsets.UTF_8)).asJsonObject
        }.getOrElse {
            Files.writeString(configPath, defaultConfigJson(), StandardCharsets.UTF_8)
            JsonParser.parseString(defaultConfigJson()).asJsonObject
        }

        val configVersion = json.get("config_version")?.asInt ?: 0
        if (configVersion < CONFIG_VERSION) {
            runCatching {
                val backupPath = configPath.resolveSibling("cobbletrainerboard.v${configVersion}.backup.json")
                Files.copy(configPath, backupPath, StandardCopyOption.REPLACE_EXISTING)
            }
            Files.writeString(configPath, defaultConfigJson(), StandardCharsets.UTF_8)
            json = JsonParser.parseString(defaultConfigJson()).asJsonObject
        }

        return parse(json)
    }

    private fun parse(root: JsonObject): BoardConfig {
        val requireRegionInMatch = root.get("require_region_in_match")?.asBoolean ?: true
        val regions = root.getAsJsonArray("regions")?.mapNotNull { regionElement ->
            val region = regionElement.asJsonObject
            val key = region.get("key")?.asString?.normalizeToken() ?: return@mapNotNull null
            val display = region.get("display")?.asString ?: key.replaceFirstChar { it.titlecase(Locale.ROOT) }
            val requiredGyms = region.get("required_gyms")?.asInt ?: 8
            val gyms = region.getAsJsonArray("gyms")?.mapNotNull { gymElement ->
                val gym = gymElement.asJsonObject
                val name = gym.get("name")?.asString ?: "Gym"
                val matchAny = gym.getAsJsonArray("match_any")?.mapNotNull { it.asString?.normalizeToken() } ?: emptyList()
                val exactIds = gym.getAsJsonArray("exact_ids")?.mapNotNull { it.asString?.lowercase(Locale.ROOT) } ?: emptyList()
                GymRule(name, matchAny, exactIds)
            } ?: emptyList()
            RegionRule(key, display, requiredGyms, gyms)
        } ?: emptyList()

        return BoardConfig(
            regions = regions.ifEmpty { parse(JsonParser.parseString(defaultConfigJson()).asJsonObject).regions },
            requireRegionInMatch = requireRegionInMatch
        )
    }

    fun String.normalizeToken(): String = lowercase(Locale.ROOT)
        .replace('é', 'e')
        .replace('è', 'e')
        .replace('ê', 'e')
        .replace('ë', 'e')
        .replace('à', 'a')
        .replace('â', 'a')
        .replace('ä', 'a')
        .replace('î', 'i')
        .replace('ï', 'i')
        .replace('ô', 'o')
        .replace('ö', 'o')
        .replace('ù', 'u')
        .replace('û', 'u')
        .replace('ü', 'u')
        .replace('ç', 'c')
        .replace(Regex("[^a-z0-9:_/.-]+"), "_")
        .trim('_')

    private fun defaultConfigJson(): String {
        val root = JsonObject()
        root.addProperty("config_version", CONFIG_VERSION)
        root.addProperty("mode", "advancements_only")
        root.addProperty("require_region_in_match", true)
        root.addProperty("comment", "CobbleTrainerBoard lit uniquement les advancements/succes de gyms completes. Les 4 premieres regions utilisent les IDs exacts des datapacks custom Cobblemon gym datapackss: Kanto, Johto, Hoenn, Sinnoh. L'ordre force est Kanto -> Johto -> Hoenn -> Sinnoh -> Unys -> Kalos -> Alola -> Galar -> Paldea.")
        val regions = JsonArray()

        fun region(key: String, display: String, gyms: List<DefaultGym>, required: Int = 8) {
            val r = JsonObject()
            r.addProperty("key", key)
            r.addProperty("display", display)
            r.addProperty("required_gyms", required)
            val gymsArray = JsonArray()
            gyms.forEach { gym ->
                val g = JsonObject()
                g.addProperty("name", gym.name)
                val match = JsonArray()
                gym.matchAny.forEach { match.add(it) }
                g.add("match_any", match)
                val exact = JsonArray()
                gym.exactIds.forEach { exact.add(it) }
                g.add("exact_ids", exact)
                gymsArray.add(g)
            }
            r.add("gyms", gymsArray)
            regions.add(r)
        }

        region("kanto", "Kanto", listOf(
            DefaultGym("Pierre / Brock", listOf("brock", "pierre", "boulder", "roche", "badge_roche"), listOf("cobbleverse:trainer/kanto/defeat_brock")),
            DefaultGym("Ondine / Misty", listOf("misty", "ondine", "cascade", "badge_cascade"), listOf("cobbleverse:trainer/kanto/defeat_misty")),
            DefaultGym("Major Bob / Lt. Surge", listOf("lt_surge", "ltsurge", "surge", "major_bob", "foudre", "thunder"), listOf("cobbleverse:trainer/kanto/defeat_ltsurge")),
            DefaultGym("Erika", listOf("erika", "rainbow", "prisme"), listOf("cobbleverse:trainer/kanto/defeat_erika")),
            DefaultGym("Koga", listOf("koga", "soul", "ame"), listOf("cobbleverse:trainer/kanto/defeat_koga")),
            DefaultGym("Morgane / Sabrina", listOf("sabrina", "morgane", "marsh", "marais"), listOf("cobbleverse:trainer/kanto/defeat_sabrina")),
            DefaultGym("Auguste / Blaine", listOf("blaine", "auguste", "volcano", "volcan"), listOf("cobbleverse:trainer/kanto/defeat_blaine")),
            DefaultGym("Giovanni", listOf("giovanni", "earth", "terre"), listOf("cobbleverse:trainer/kanto/defeat_giovanni"))
        ))
        region("johto", "Johto", listOf(
            DefaultGym("Valerio / Falkner", listOf("valerio", "falkner", "albert", "zephyr"), listOf("cobbleverse:trainer/johto/defeat_valerio")),
            DefaultGym("Raffaello / Bugsy", listOf("raffaello", "bugsy", "hector", "hive", "essaim"), listOf("cobbleverse:trainer/johto/defeat_raffaello")),
            DefaultGym("Chiara / Whitney", listOf("chiara", "whitney", "blanche", "plain", "plaine"), listOf("cobbleverse:trainer/johto/defeat_chiara")),
            DefaultGym("Angelo / Morty", listOf("angelo", "morty", "mortimer", "fog", "brume"), listOf("cobbleverse:trainer/johto/defeat_angelo")),
            DefaultGym("Furio / Chuck", listOf("furio", "chuck", "storm", "tempete"), listOf("cobbleverse:trainer/johto/defeat_furio")),
            DefaultGym("Jasmine", listOf("jasmine", "mineral"), listOf("cobbleverse:trainer/johto/defeat_jasmine")),
            DefaultGym("Alfredo / Pryce", listOf("alfredo", "pryce", "fredo", "glacier"), listOf("cobbleverse:trainer/johto/defeat_alfredo")),
            DefaultGym("Sandra / Clair", listOf("sandra", "clair", "rising", "lever"), listOf("cobbleverse:trainer/johto/defeat_sandra"))
        ))
        region("hoenn", "Hoenn", listOf(
            DefaultGym("Petra / Roxanne", listOf("petra", "roxanne", "stone", "roche"), listOf("cobbleverse:trainer/hoenn/defeat_petra")),
            DefaultGym("Rudi / Brawly", listOf("rudi", "brawly", "bastien", "knuckle", "poing"), listOf("cobbleverse:trainer/hoenn/defeat_rudi")),
            DefaultGym("Walter / Wattson", listOf("walter", "wattson", "voltere", "dynamo"), listOf("cobbleverse:trainer/hoenn/defeat_walter")),
            DefaultGym("Fiammetta / Flannery", listOf("fiammetta", "flannery", "adriane", "heat", "chaleur"), listOf("cobbleverse:trainer/hoenn/defeat_fiammetta")),
            DefaultGym("Norman", listOf("norman", "balance"), listOf("cobbleverse:trainer/hoenn/defeat_norman")),
            DefaultGym("Alice / Winona", listOf("alice", "winona", "alizee", "feather", "plume"), listOf("cobbleverse:trainer/hoenn/defeat_alice")),
            DefaultGym("Tell / Tate & Liza", listOf("tell", "tate", "liza", "levy", "tatia", "mind", "esprit"), listOf("cobbleverse:trainer/hoenn/defeat_tell")),
            DefaultGym("Adriano / Wallace/Juan", listOf("adriano", "wallace", "marc", "juan", "adam", "rain", "pluie"), listOf("cobbleverse:trainer/hoenn/defeat_adriano"))
        ))
        region("sinnoh", "Sinnoh", listOf(
            DefaultGym("Pedro / Roark", listOf("pedro", "roark", "pierrick", "coal", "charbon"), listOf("cobbleverse:trainer/sinnoh/defeat_pedro")),
            DefaultGym("Gardenia / Flo", listOf("gardenia", "flo", "forest", "foret"), listOf("cobbleverse:trainer/sinnoh/defeat_gardenia")),
            DefaultGym("Marzia / Maylene", listOf("marzia", "maylene", "melina", "cobble", "pave"), listOf("cobbleverse:trainer/sinnoh/defeat_marzia")),
            DefaultGym("Omar / Crasher Wake", listOf("omar", "crasher_wake", "wake", "lovis", "fen"), listOf("cobbleverse:trainer/sinnoh/defeat_omar")),
            DefaultGym("Fannie / Fantina", listOf("fannie", "fantina", "kimera", "relic", "relique"), listOf("cobbleverse:trainer/sinnoh/defeat_fannie")),
            DefaultGym("Ferruccio / Byron", listOf("ferruccio", "byron", "charles", "mine"), listOf("cobbleverse:trainer/sinnoh/defeat_ferruccio")),
            DefaultGym("Bianca / Candice", listOf("bianca", "candice", "gladys", "icicle", "glacon"), listOf("cobbleverse:trainer/sinnoh/defeat_bianca")),
            DefaultGym("Corrado / Volkner", listOf("corrado", "volkner", "tanguy", "beacon", "phare"), listOf("cobbleverse:trainer/sinnoh/defeat_corrado"))
        ))
        region("unova", "Unys", listOf(
            DefaultGym("Rachid/Noa/Armando", listOf("cilan", "chili", "cress", "rachid", "noa", "armando", "trio")),
            DefaultGym("Aloé / Lenora", listOf("lenora", "aloe", "basic", "basique")),
            DefaultGym("Artie / Burgh", listOf("burgh", "artie", "insect", "bug")),
            DefaultGym("Inezia / Elesa", listOf("elesa", "inezia", "bolt", "volt")),
            DefaultGym("Bardane / Clay", listOf("clay", "bardane", "quake", "seisme")),
            DefaultGym("Carolina / Skyla", listOf("skyla", "carolina", "jet")),
            DefaultGym("Zhu / Brycen", listOf("brycen", "zhu", "freeze", "gel")),
            DefaultGym("Watson/Iris/Drayden", listOf("drayden", "iris", "watson", "legend"))
        ))
        region("kalos", "Kalos", listOf(
            DefaultGym("Violette / Viola", listOf("viola", "violette", "bug", "insecte")),
            DefaultGym("Lino / Grant", listOf("grant", "lino", "cliff", "falaise")),
            DefaultGym("Cornélia / Korrina", listOf("korrina", "cornelia", "rumble", "lutte")),
            DefaultGym("Amaro / Ramos", listOf("ramos", "amaro", "plant", "plante")),
            DefaultGym("Lem / Clemont", listOf("clemont", "lem", "voltage")),
            DefaultGym("Valériane / Valerie", listOf("valerie", "valeriane", "fairy", "fee")),
            DefaultGym("Astera / Olympia", listOf("olympia", "astera", "psychic", "psy")),
            DefaultGym("Urup / Wulfric", listOf("wulfric", "urup", "iceberg"))
        ))
        region("alola", "Alola", listOf(
            DefaultGym("Épreuve Ilima", listOf("ilima", "normalium", "normal")),
            DefaultGym("Épreuve Néphie / Lana", listOf("lana", "nephie", "waterium", "eau")),
            DefaultGym("Épreuve Kiawe", listOf("kiawe", "firium", "feu")),
            DefaultGym("Épreuve Barbara / Mallow", listOf("mallow", "barbara", "grassium", "plante")),
            DefaultGym("Épreuve Chrys / Sophocles", listOf("sophocles", "chrys", "electrium", "electrique")),
            DefaultGym("Épreuve Margie / Acerola", listOf("acerola", "margie", "ghostium", "spectre")),
            DefaultGym("Épreuve Oléa / Mina", listOf("mina", "olea", "fairium", "fee")),
            DefaultGym("Grande épreuve Poni / Hapu", listOf("hapu", "poni", "groundium", "sol", "grand_trial"))
        ))
        region("galar", "Galar", listOf(
            DefaultGym("Percy / Milo", listOf("milo", "percy", "grass", "plante")),
            DefaultGym("Donna / Nessa", listOf("nessa", "donna", "water", "eau")),
            DefaultGym("Kabu", listOf("kabu", "fire", "feu")),
            DefaultGym("Faïza / Bea", listOf("bea", "faiza", "fighting", "combat")),
            DefaultGym("Alistair / Allister", listOf("allister", "alistair", "ghost", "spectre")),
            DefaultGym("Charmilly / Opal", listOf("opal", "charmilly", "fairy", "fee")),
            DefaultGym("Chaz / Gordie", listOf("gordie", "chaz", "rock", "roche")),
            DefaultGym("Lona / Melony", listOf("melony", "lona", "ice", "glace")),
            DefaultGym("Peterson / Piers", listOf("piers", "peterson", "dark", "tenebres")),
            DefaultGym("Roy / Raihan", listOf("raihan", "roy", "dragon"))
        ), required = 8)
        region("paldea", "Paldea", listOf(
            DefaultGym("Katy", listOf("katy", "mold", "bug", "insecte")),
            DefaultGym("Brassius", listOf("brassius", "colza", "grass", "plante")),
            DefaultGym("Iono", listOf("iono", "e_raihi", "electrique", "electric")),
            DefaultGym("Kofu", listOf("kofu", "kombu", "water", "eau")),
            DefaultGym("Larry", listOf("larry", "aoki", "normal")),
            DefaultGym("Ryme", listOf("ryme", "lime", "ghost", "spectre")),
            DefaultGym("Tulip", listOf("tulip", "tully", "psychic", "psy")),
            DefaultGym("Grusha", listOf("grusha", "ice", "glace"))
        ))

        root.add("regions", regions)
        return gson.toJson(root)
    }
}
