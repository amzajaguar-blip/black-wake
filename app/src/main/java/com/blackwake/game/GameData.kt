package com.blackwake.game

enum class EnvironmentId {
    NIGHT, PORT, STORM, EXTRACT, ISLAND, DAWN
}

data class Sector(
    val id: String,
    val title: String,
    val tag: String,
    /** Length of the sector in cruise-seconds: boosting covers it faster, running silent slower. */
    val duration: Float,
    val environment: EnvironmentId,
    val density: Float,
    val intelBias: Float,
    val threat: Float,
    val radio: String,
    val fork: Boolean = false,
    val extract: Boolean = false
)

data class Chapter(
    val code: String,
    val title: String,
    val subtitle: String,
    val intelGoal: Int,
    val briefing: List<String>,
    val maya: String,
    val closer: List<String>,
    val sectors: List<Sector>
) {
    /** Intel needed when the extraction window closes: 70% of the goal, rounded up. */
    val intelRequired: Int get() = (intelGoal * 7 + 9) / 10
}

fun createSector(
    id: String, title: String, tag: String,
    environment: EnvironmentId, density: Float, intelBias: Float, threat: Float,
    radio: String, fork: Boolean = false, extract: Boolean = false
): Sector {
    val duration = if (extract) 14f else 11f + Math.round(density * 2f).toFloat()
    return Sector(id, title, tag, duration, environment, density, intelBias, threat, radio, fork, extract)
}

val CHAPTERS = listOf(
    Chapter(
        code = "01", title = "IL RELITTO", subtitle = "Qualcosa che non avresti dovuto vedere", intelGoal = 10,
        briefing = listOf("Operazione notturna. Una nave apparentemente abbandonata, un contenitore nero.", "Dentro: coordinate, nomi, registrazioni. Non denaro. Non armi. La prova.", "La squadra è stata eliminata. Resta una barca piccola e il mare."),
        maya = "…canale aperto. Se sei quello che penso, non tornare a riva.",
        closer = listOf("BLACK TIDE ha il tuo volto. Ogni porto può essere una trappola.", "Il mare sembra libero. Non lo è."),
        sectors = listOf(
            createSector("01-open", "ACQUE APERTE", "SEGNATURA BASSA", EnvironmentId.NIGHT, 0.72f, 0.68f, 0.30f, "MAYA: Radar pulito. Non significa sicuro."),
            createSector("01-salt", "STRETTO DI PORTO", "SCELTA DI ROTTA", EnvironmentId.PORT, 0.88f, 0.56f, 0.48f, "MAYA: Tre passaggi. Uno ti compra tempo.", fork = true),
            createSector("01-wake", "SCIA NERA", "CONTATTO PROBABILE", EnvironmentId.NIGHT, 1.03f, 0.47f, 0.72f, "MAYA: Una luce a tribordo. Tieni il motore basso."),
            createSector("01-uplink", "FINESTRA DI ESTRAZIONE", "TRASMISSIONE IN CORSO", EnvironmentId.EXTRACT, 1.12f, 0.22f, 0.90f, "MAYA: Ora. Trasmetti e resta vivo.", extract = true)
        )
    ),
    Chapter(
        code = "02", title = "LA CONTRABBANDIERA", subtitle = "Un nome che apre tutte le rotte sbagliate", intelGoal = 12,
        briefing = listOf("L'ultimo passaggio della Contrabbandiera parte da un porto che non compare sulle carte.", "La sua barca è ferma tra container senza insegne. Il registro è ancora a bordo.", "Se Black Tide arriva prima, il nome sparisce con l'acqua."),
        maya = "Cerca ciò che ha nascosto, non ciò che ha lasciato in vista.",
        closer = listOf("Il registro porta a un arcipelago cancellato.", "Qualcuno ha trasformato le isole in magazzini."),
        sectors = listOf(
            createSector("02-diga", "DIGA EST", "LANTERNE SPENTE", EnvironmentId.PORT, 0.86f, 0.58f, 0.48f, "MAYA: Le luci rosse non sono del porto."),
            createSector("02-container", "FILA CONTAINER", "CORRIDOIO STRETTO", EnvironmentId.PORT, 1.05f, 0.49f, 0.66f, "MAYA: I container fanno eco. Il radar qui mente."),
            createSector("02-dogana", "TAGLIO DOGANALE", "ROTTA A TRE VIE", EnvironmentId.PORT, 1.12f, 0.54f, 0.72f, "MAYA: Canale profondo, prova o tanica piena.", fork = true),
            createSector("02-scalo", "SCALO SENZA NOME", "ESTRAZIONE BREVE", EnvironmentId.EXTRACT, 1.18f, 0.24f, 0.88f, "MAYA: Prendi il registro e non guardare il molo.", extract = true)
        )
    ),
    Chapter(
        code = "03", title = "ISOLA MORTA", subtitle = "L'arcipelago che non vuole essere trovato", intelGoal = 13,
        briefing = listOf("Le coordinate indicano un'isola priva di fari e di abitanti.", "Le casse sono sulla riva, ma le scie intorno alla baia sono fresche.", "Recupera il manifest. Non attraccare."),
        maya = "Se senti le campane due volte, esci dalla baia.",
        closer = listOf("Il manifest contiene una parola ricorrente: Kross.", "Non è un carico. È una persona."),
        sectors = listOf(
            createSector("03-scogliera", "SCOGLIERA ESTERNA", "FONDALE MOBILE", EnvironmentId.ISLAND, 0.92f, 0.64f, 0.45f, "MAYA: I relitti vogliono tirarti sotto."),
            createSector("03-baia", "BAIA DELLE CAMPANE", "SILENZIO RICHIESTO", EnvironmentId.ISLAND, 1.06f, 0.55f, 0.63f, "MAYA: Qui ogni rumore trova qualcuno."),
            createSector("03-deposito", "DEPOSITO DI RIVA", "ROTTA A TRE VIE", EnvironmentId.ISLAND, 1.12f, 0.60f, 0.68f, "MAYA: La prova è al centro. Le altre corsie sono una domanda.", fork = true),
            createSector("03-foschia", "USCITA NELLA FOSCHIA", "ESTRAZIONE VELATA", EnvironmentId.EXTRACT, 1.18f, 0.26f, 0.84f, "MAYA: Trasmetti tra le onde. La nebbia non dura.", extract = true)
        )
    ),
    Chapter(
        code = "04", title = "CACCIA NELLA TEMPESTA", subtitle = "La prova pesa meno di una raffica", intelGoal = 14,
        briefing = listOf("Una consegna è stata registrata come incidente durante una tempesta.", "Le registrazioni sono in una capsula stagna oltre il fronte freddo.", "Questa volta il mare non è uno sfondo. È il primo nemico."),
        maya = "Le onde coprono il rumore. Ma coprono anche le mine.",
        closer = listOf("La capsula contiene un ordine firmato da un funzionario scomparso.", "Black Tide non trasporta merce: cancella testimoni."),
        sectors = listOf(
            createSector("04-burrasca", "LINEA DI BURRASCA", "SCARICA IN ARRIVO", EnvironmentId.STORM, 1.00f, 0.58f, 0.59f, "MAYA: Tieni la prua alle onde."),
            createSector("04-muraglia", "MURAGLIA D'ACQUA", "VISIBILITÀ ZERO", EnvironmentId.STORM, 1.18f, 0.46f, 0.75f, "MAYA: Non inseguire le luci."),
            createSector("04-capsula", "DERIVA DELLA CAPSULA", "ROTTA A TRE VIE", EnvironmentId.STORM, 1.26f, 0.62f, 0.79f, "MAYA: C'è una capsula in mezzo al caos.", fork = true),
            createSector("04-fulmini", "FINESTRA DI FULMINI", "ESTRAZIONE IN CORSA", EnvironmentId.EXTRACT, 1.30f, 0.25f, 0.96f, "MAYA: Trasmetti sul lampo. Hai una sola finestra.", extract = true)
        )
    ),
    Chapter(
        code = "05", title = "IL TRADIMENTO", subtitle = "La voce sulla radio conosceva il tuo nome", intelGoal = 15,
        briefing = listOf("La firma nella capsula porta al vostro vecchio canale operativo.", "Qualcuno dall'interno ha consegnato rotte e squadre a Black Tide.", "Non sai più quali istruzioni siano sicure. Segui soltanto la prova."),
        maya = "Se arriva un ordine con la mia voce, non sono io.",
        closer = listOf("Il traditore è vicino alla sorgente delle trasmissioni.", "La sorgente è il Porto Nero."),
        sectors = listOf(
            createSector("05-frequenza", "FREQUENZA MORTA", "NESSUN CANALE SICURO", EnvironmentId.NIGHT, 1.08f, 0.53f, 0.69f, "MAYA: Silenzio radio per trenta secondi."),
            createSector("05-caccia", "CORRIDOIO DI CACCIA", "CONTATTO CONTINUO", EnvironmentId.NIGHT, 1.21f, 0.42f, 0.88f, "KROSS: Ti hanno dato una barca piccola. È quasi un complimento."),
            createSector("05-fari", "FARI FALSI", "ROTTA A TRE VIE", EnvironmentId.NIGHT, 1.29f, 0.56f, 0.86f, "MAYA: Tre luci. Una è mia. Non posso dirti quale.", fork = true),
            createSector("05-uplink", "UPLINK SPORCO", "ESTRAZIONE COMPROMESSA", EnvironmentId.EXTRACT, 1.34f, 0.23f, 1.02f, "MAYA: Invia tutto. Se ci ascoltano, sapranno che ci siamo.", extract = true)
        )
    ),
    Chapter(
        code = "06", title = "IL PORTO NERO", subtitle = "Un porto che appare solo nelle ore senza nome", intelGoal = 16,
        briefing = listOf("Le trasmissioni partono da una darsena nascosta dietro un impianto dismesso.", "Il porto è pieno di occhi automatici e corsie strettissime.", "Entra senza essere un bersaglio. Esci con il nodo della rete."),
        maya = "Droni alti, barche basse. Non dare a nessuno una forma pulita da seguire.",
        closer = listOf("Il nodo porta a Kross in persona.", "Per trovarlo devi avvicinarti abbastanza da farti vedere."),
        sectors = listOf(
            createSector("06-servizio", "TAGLIO DI SERVIZIO", "DRONI SOPRA", EnvironmentId.PORT, 1.14f, 0.48f, 0.77f, "MAYA: Le gru sono cieche. I droni no."),
            createSector("06-banchine", "BANCHINE NERE", "SEGNATURA ALTA", EnvironmentId.PORT, 1.28f, 0.43f, 0.94f, "MAYA: Sei nel loro campo."),
            createSector("06-chiatta", "CHIATTA SERVER", "ROTTA A TRE VIE", EnvironmentId.PORT, 1.34f, 0.61f, 0.93f, "MAYA: La chiatta centrale tiene tutto.", fork = true),
            createSector("06-varco", "VARCO ZERO", "ESTRAZIONE IMPOSSIBILE", EnvironmentId.EXTRACT, 1.38f, 0.22f, 1.06f, "MAYA: Hai il nodo. Sparisci prima che lo capiscano.", extract = true)
        )
    ),
    Chapter(
        code = "07", title = "KROSS", subtitle = "Un volto dall'altra parte del fascio di luce", intelGoal = 18,
        briefing = listOf("Il nodo trasmette una rotta verso il largo, poco prima dell'alba.", "Kross non fugge: vuole che tu lo segua.", "Prendi la registrazione. Non inseguire l'uomo."),
        maya = "Se Kross parla, lascia che finisca. Le persone sicure di vincere dicono sempre troppo.",
        closer = listOf("Kross ha nominato l'operazione finale: Marea Nera.", "Ha lasciato una rotta per il punto di scarico."),
        sectors = listOf(
            createSector("07-ora-blu", "ORA BLU", "ORIZZONTE ESPOSTO", EnvironmentId.DAWN, 1.17f, 0.56f, 0.82f, "KROSS: Una luce piccola in un mare grande."),
            createSector("07-cacciatore", "SCIA DEL CACCIATORE", "CONTATTO VISIVO", EnvironmentId.DAWN, 1.33f, 0.41f, 0.99f, "MAYA: Non accelerare per rabbia."),
            createSector("07-trappola", "TRAPPOLA APERTA", "ROTTA A TRE VIE", EnvironmentId.NIGHT, 1.40f, 0.59f, 0.98f, "KROSS: Scegli bene. Io ho già scelto per te.", fork = true),
            createSector("07-impulso", "IMPULSO DI REGISTRAZIONE", "ESTRAZIONE FRAGILE", EnvironmentId.EXTRACT, 1.43f, 0.20f, 1.10f, "MAYA: La registrazione è completa.", extract = true)
        )
    ),
    Chapter(
        code = "08", title = "MAREA NERA", subtitle = "Quando il mare smette di nascondere", intelGoal = 20,
        briefing = listOf("La rotta finale porta a una piattaforma di scarico durante l'ultima tempesta.", "Qui Black Tide cancella prove, navi e nomi.", "Non c'è una squadra in arrivo. Non ci sarà un secondo passaggio."),
        maya = "Tutta la rete è in ascolto. Fai arrivare i dati.",
        closer = listOf("La Marea Nera è uscita dai suoi confini.", "Per la prima volta, il mare ha restituito i nomi."),
        sectors = listOf(
            createSector("08-onda", "ONDA DI FERRO", "TEMPESTA TERMINALE", EnvironmentId.STORM, 1.30f, 0.52f, 0.96f, "MAYA: Tieni la rotta."),
            createSector("08-affondamento", "CAMPO D'AFFONDAMENTO", "RELIQUATI IN ACQUA", EnvironmentId.STORM, 1.43f, 0.47f, 1.08f, "MAYA: I relitti sono la loro firma."),
            createSector("08-scarico", "PUNTO DI SCARICO", "ROTTA A TRE VIE", EnvironmentId.EXTRACT, 1.52f, 0.66f, 1.12f, "KROSS: Anche se invii tutto, il mare resta mio.", fork = true),
            createSector("08-segnale", "SEGNALE APERTO", "ESTRAZIONE FINALE", EnvironmentId.EXTRACT, 1.58f, 0.20f, 1.20f, "MAYA: Tutto il mondo ascolta. Trasmetti.", extract = true)
        )
    )
)

const val MODULE_MAX_LEVEL = 3

data class ModuleDef(val id: String, val title: String, val tag: String, val detail: String, val costs: List<Int>)

val MODULES = listOf(
    ModuleDef("engine", "MOTORE", "SPINTA", "La spinta alta consuma meno carburante.", listOf(12, 28, 50)),
    ModuleDef("hull", "SCAFO", "INTEGRITÀ", "Più scafo, impatti meno violenti.", listOf(14, 30, 55)),
    ModuleDef("tank", "SERBATOIO", "AUTONOMIA", "Più carburante da ogni tanica.", listOf(10, 24, 44)),
    ModuleDef("radar", "RADAR", "AVVISO", "Durante la caccia il sonar pulsa più spesso.", listOf(16, 32, 58)),
    ModuleDef("stealth", "SILENZIO", "FURTIVITÀ", "In silenzio la firma cala più in fretta.", listOf(18, 36, 64))
)

data class Modules(
    val engine: Int = 0,
    val hull: Int = 0,
    val tank: Int = 0,
    val radar: Int = 0,
    val stealth: Int = 0
) {
    fun level(id: String): Int = when (id) {
        "engine" -> engine
        "hull" -> hull
        "tank" -> tank
        "radar" -> radar
        "stealth" -> stealth
        else -> 0
    }

    fun upgraded(id: String): Modules = when (id) {
        "engine" -> copy(engine = engine + 1)
        "hull" -> copy(hull = hull + 1)
        "tank" -> copy(tank = tank + 1)
        "radar" -> copy(radar = radar + 1)
        "stealth" -> copy(stealth = stealth + 1)
        else -> this
    }
}

/** Vessel trade-offs. Every field is read by the simulation; nothing here is cosmetic only. */
data class BoatSpec(
    val type: PlayerBoatType,
    val title: String,
    val detail: String,
    val speed: Float,
    val hullBonus: Float,
    val detection: Float,
    val fuelUse: Float,
    val hullColor: Long
)

val BOATS = listOf(
    BoatSpec(PlayerBoatType.SPEEDBOAT, "MOTOSCAFO", "Equilibrato. Nessun punto debole.", 1.00f, 0f, 1.00f, 1.00f, 0xFF132F38),
    BoatSpec(PlayerBoatType.RACING, "CORSA", "Veloce e rumoroso. Scafo leggero.", 1.12f, -20f, 1.10f, 1.05f, 0xFF3A1D24),
    BoatSpec(PlayerBoatType.PATROL, "PATTUGLIATORE", "Scafo rinforzato, consumi più alti.", 0.97f, 15f, 1.00f, 1.12f, 0xFF1F2F3F),
    BoatSpec(PlayerBoatType.SMUGGLER, "CONTRABBANDIERE", "Serbatoio parsimonioso, più lento.", 0.93f, 0f, 1.00f, 0.75f, 0xFF2F2A1C),
    BoatSpec(PlayerBoatType.STEALTH, "OMBRA", "Firma radar ridotta. Scafo fragile.", 1.00f, -15f, 0.75f, 1.00f, 0xFF0B1418),
    BoatSpec(PlayerBoatType.ARMORED, "CORAZZATO", "Scafo pesante: lento e visibile.", 0.88f, 35f, 1.12f, 1.10f, 0xFF2E3336)
)

fun boatSpec(type: PlayerBoatType): BoatSpec = BOATS.first { it.type == type }

fun enemyLabel(type: EnemyBoatType): String = when (type) {
    EnemyBoatType.PATROL -> "PATTUGLIA"
    EnemyBoatType.INTERCEPTOR -> "INTERCETTORE"
    EnemyBoatType.POLICE -> "GUARDIA"
    EnemyBoatType.HUNTER -> "CACCIATORE"
    EnemyBoatType.ARMORED -> "CORAZZATA"
    EnemyBoatType.ELITE -> "ÉLITE"
}
