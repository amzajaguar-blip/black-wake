package com.blackwake.game

enum class EnvironmentId {
    NIGHT, PORT, STORM, EXTRACT, ISLAND, DAWN
}

data class Sector(
    val id: String,
    val title: String,
    val tag: String,
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
)

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
            createSector("02-dogana", "TAGLIO DOGANALE", "ROUTE A TRE VIE", EnvironmentId.PORT, 1.12f, 0.54f, 0.72f, "MAYA: Canale profondo, prova o tanica piena.", fork = true),
            createSector("02-scalo", "SCALO SENZA NOME", "ESTRAZIONE BREVE", EnvironmentId.EXTRACT, 1.18f, 0.24f, 0.88f, "MAYA: Prendi il registro e non guardare il molo.", extract = true)
        )
    ),
    // Remaining chapters generated dynamically for brevity in rewrite if needed, but I'll add them to be complete.
    Chapter(
        code = "03", title = "ISOLA MORTA", subtitle = "L'arcipelago che non vuole essere trovato", intelGoal = 13,
        briefing = listOf("Le coordinate indicano un'isola priva di fari e di abitanti.", "Le casse sono sulla riva, ma le scie intorno alla baia sono fresche.", "Recupera il manifest. Non attraccare."),
        maya = "Se senti le campane due volte, esci dalla baia.",
        closer = listOf("Il manifest contiene una parola ricorrente: Kross.", "Non è un carico. È una persona."),
        sectors = listOf(
            createSector("03-scogliera", "SCOGLIERA ESTERNA", "FONDALE MOBILE", EnvironmentId.ISLAND, 0.92f, 0.64f, 0.45f, "MAYA: I relitti vogliono tirarti sotto."),
            createSector("03-baia", "BAIA DELLE CAMPANE", "SILENZIO RICHIESTO", EnvironmentId.ISLAND, 1.06f, 0.55f, 0.63f, "MAYA: Qui ogni rumore trova qualcuno."),
            createSector("03-deposito", "DEPOSITO DI RIVA", "ROUTE A TRE VIE", EnvironmentId.ISLAND, 1.12f, 0.60f, 0.68f, "MAYA: La prova è al centro. Le altre corsie sono una domanda.", fork = true),
            createSector("03-foschia", "USCITA NELLA FOSCHIA", "ESTRAZIONE VELATA", EnvironmentId.EXTRACT, 1.18f, 0.26f, 0.84f, "MAYA: Trasmetti tra le onde. La nebbia non dura.", extract = true)
        )
    )
)

data class ModuleDef(val id: String, val title: String, val tag: String, val detail: String, val costs: List<Int>)

val MODULES = listOf(
    ModuleDef("engine", "MOTORE", "SPINTA", "Boost più efficiente.", listOf(12, 28, 50)),
    ModuleDef("hull", "SCAFO", "INTEGRITÀ", "Assorbe l'impatto.", listOf(14, 30, 55)),
    ModuleDef("tank", "SERBATOIO", "AUTONOMIA", "Più carburante recuperato.", listOf(10, 24, 44)),
    ModuleDef("radar", "RADAR", "AVVISO", "Segnali prima, panico dopo.", listOf(16, 32, 58)),
    ModuleDef("stealth", "SILENZIO", "FURTIVITÀ", "Firma ridotta in deriva.", listOf(18, 36, 64))
)

data class ModulesState(
    var engine: Int = 0,
    var hull: Int = 0,
    var tank: Int = 0,
    var radar: Int = 0,
    var stealth: Int = 0
)
