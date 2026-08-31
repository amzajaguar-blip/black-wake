/** Cartografia d'Emergenza: dati narrativi, compatti e leggibili dal motore di fuga. */
export type EnvironmentId = "night" | "port" | "storm" | "extract" | "island" | "dawn";

export type Sector = {
  id: string;
  title: string;
  tag: string;
  duration: number;
  environment: EnvironmentId;
  density: number;
  intelBias: number;
  threat: number;
  fork?: boolean;
  extract?: boolean;
  radio: string;
};

export type Chapter = {
  code: string;
  title: string;
  subtitle: string;
  intelGoal: number;
  briefing: string[];
  maya: string;
  closer: string[];
  sectors: Sector[];
};

export const ASSETS = {
  visualTarget: "/manus-storage/marea-nera-visual-target_cb8baa16.jpg",
  logo: "/manus-storage/marea-nera-logo-v2_32012a38.png",
} as const;

const sector = (id: string, title: string, tag: string, environment: EnvironmentId, density: number, intelBias: number, threat: number, radio: string, extra: Pick<Sector, "fork" | "extract"> = {}): Sector => ({
  id, title, tag, duration: extra.extract ? 14 : 11 + Math.round(density * 2), environment, density, intelBias, threat, radio, ...extra,
});

export const CHAPTERS: Chapter[] = [
  {
    code: "01", title: "IL RELITTO", subtitle: "Qualcosa che non avresti dovuto vedere", intelGoal: 10,
    briefing: ["Operazione notturna. Una nave apparentemente abbandonata, un contenitore nero.", "Dentro: coordinate, nomi, registrazioni. Non denaro. Non armi. La prova.", "La squadra è stata eliminata. Resta una barca piccola e il mare."],
    maya: "…canale aperto. Se sei quello che penso, non tornare a riva.",
    closer: ["BLACK TIDE ha il tuo volto. Ogni porto può essere una trappola.", "Il mare sembra libero. Non lo è."],
    sectors: [
      sector("01-open", "ACQUE APERTE", "SEGNATURA BASSA", "night", .72, .68, .30, "MAYA: Radar pulito. Non significa sicuro."),
      sector("01-salt", "STRETTO DI PORTO", "SCELTA DI ROTTA", "port", .88, .56, .48, "MAYA: Tre passaggi. Uno ti compra tempo.", { fork: true }),
      sector("01-wake", "SCIA NERA", "CONTATTO PROBABILE", "night", 1.03, .47, .72, "MAYA: Una luce a tribordo. Tieni il motore basso."),
      sector("01-uplink", "FINESTRA DI ESTRAZIONE", "TRASMISSIONE IN CORSO", "extract", 1.12, .22, .90, "MAYA: Ora. Trasmetti e resta vivo.", { extract: true }),
    ],
  },
  {
    code: "02", title: "LA CONTRABBANDIERA", subtitle: "Un nome che apre tutte le rotte sbagliate", intelGoal: 12,
    briefing: ["L'ultimo passaggio della Contrabbandiera parte da un porto che non compare sulle carte.", "La sua barca è ferma tra container senza insegne. Il registro è ancora a bordo.", "Se Black Tide arriva prima, il nome sparisce con l'acqua."],
    maya: "Cerca ciò che ha nascosto, non ciò che ha lasciato in vista.",
    closer: ["Il registro porta a un arcipelago cancellato.", "Qualcuno ha trasformato le isole in magazzini."],
    sectors: [
      sector("02-diga", "DIGA EST", "LANTERNE SPENTE", "port", .86, .58, .48, "MAYA: Le luci rosse non sono del porto."),
      sector("02-container", "FILA CONTAINER", "CORRIDOIO STRETTO", "port", 1.05, .49, .66, "MAYA: I container fanno eco. Il radar qui mente."),
      sector("02-dogana", "TAGLIO DOGANALE", "ROUTE A TRE VIE", "port", 1.12, .54, .72, "MAYA: Canale profondo, prova o tanica piena.", { fork: true }),
      sector("02-scalo", "SCALO SENZA NOME", "ESTRAZIONE BREVE", "extract", 1.18, .24, .88, "MAYA: Prendi il registro e non guardare il molo.", { extract: true }),
    ],
  },
  {
    code: "03", title: "ISOLA MORTA", subtitle: "L'arcipelago che non vuole essere trovato", intelGoal: 13,
    briefing: ["Le coordinate indicano un'isola priva di fari e di abitanti.", "Le casse sono sulla riva, ma le scie intorno alla baia sono fresche.", "Recupera il manifest. Non attraccare."],
    maya: "Se senti le campane due volte, esci dalla baia.",
    closer: ["Il manifest contiene una parola ricorrente: Kross.", "Non è un carico. È una persona."],
    sectors: [
      sector("03-scogliera", "SCOGLIERA ESTERNA", "FONDALE MOBILE", "island", .92, .64, .45, "MAYA: I relitti vogliono tirarti sotto."),
      sector("03-baia", "BAIA DELLE CAMPANE", "SILENZIO RICHIESTO", "island", 1.06, .55, .63, "MAYA: Qui ogni rumore trova qualcuno."),
      sector("03-deposito", "DEPOSITO DI RIVA", "ROUTE A TRE VIE", "island", 1.12, .60, .68, "MAYA: La prova è al centro. Le altre corsie sono una domanda.", { fork: true }),
      sector("03-foschia", "USCITA NELLA FOSCHIA", "ESTRAZIONE VELATA", "extract", 1.18, .26, .84, "MAYA: Trasmetti tra le onde. La nebbia non dura.", { extract: true }),
    ],
  },
  {
    code: "04", title: "CACCIA NELLA TEMPESTA", subtitle: "La prova pesa meno di una raffica", intelGoal: 14,
    briefing: ["Una consegna è stata registrata come incidente durante una tempesta.", "Le registrazioni sono in una capsula stagna oltre il fronte freddo.", "Questa volta il mare non è uno sfondo. È il primo nemico."],
    maya: "Le onde coprono il rumore. Ma coprono anche le mine.",
    closer: ["La capsula contiene un ordine firmato da un funzionario scomparso.", "Black Tide non trasporta merce: cancella testimoni."],
    sectors: [
      sector("04-burrasca", "LINEA DI BURRASCA", "SCARICA IN ARRIVO", "storm", 1.00, .58, .59, "MAYA: Tieni la prua alle onde."),
      sector("04-muraglia", "MURAGLIA D'ACQUA", "VISIBILITÀ ZERO", "storm", 1.18, .46, .75, "MAYA: Non inseguire le luci."),
      sector("04-capsula", "DERIVA DELLA CAPSULA", "ROUTE A TRE VIE", "storm", 1.26, .62, .79, "MAYA: C'è una capsula in mezzo al caos.", { fork: true }),
      sector("04-fulmini", "FINESTRA DI FULMINI", "ESTRAZIONE IN CORSA", "extract", 1.30, .25, .96, "MAYA: Trasmetti sul lampo. Hai una sola finestra.", { extract: true }),
    ],
  },
  {
    code: "05", title: "IL TRADIMENTO", subtitle: "La voce sulla radio conosceva il tuo nome", intelGoal: 15,
    briefing: ["La firma nella capsula porta al vostro vecchio canale operativo.", "Qualcuno dall'interno ha consegnato rotte e squadre a Black Tide.", "Non sai più quali istruzioni siano sicure. Segui soltanto la prova."],
    maya: "Se arriva un ordine con la mia voce, non sono io.",
    closer: ["Il traditore è vicino alla sorgente delle trasmissioni.", "La sorgente è il Porto Nero."],
    sectors: [
      sector("05-frequenza", "FREQUENZA MORTA", "NESSUN CANALE SICURO", "night", 1.08, .53, .69, "MAYA: Silenzio radio per trenta secondi."),
      sector("05-caccia", "CORRIDOIO DI CACCIA", "CONTATTO CONTINUO", "night", 1.21, .42, .88, "KROSS: Ti hanno dato una barca piccola. È quasi un complimento."),
      sector("05-fari", "FARI FALSI", "ROUTE A TRE VIE", "night", 1.29, .56, .86, "MAYA: Tre luci. Una è mia. Non posso dirti quale.", { fork: true }),
      sector("05-uplink", "UPLINK SPORCO", "ESTRAZIONE COMPROMESSA", "extract", 1.34, .23, 1.02, "MAYA: Invia tutto. Se ci ascoltano, sapranno che ci siamo.", { extract: true }),
    ],
  },
  {
    code: "06", title: "IL PORTO NERO", subtitle: "Un porto che appare solo nelle ore senza nome", intelGoal: 16,
    briefing: ["Le trasmissioni partono da una darsena nascosta dietro un impianto dismesso.", "Il porto è pieno di occhi automatici e corsie strettissime.", "Entra senza essere un bersaglio. Esci con il nodo della rete."],
    maya: "Droni alti, barche basse. Non dare a nessuno una forma pulita da seguire.",
    closer: ["Il nodo porta a Kross in persona.", "Per trovarlo devi avvicinarti abbastanza da farti vedere."],
    sectors: [
      sector("06-servizio", "TAGLIO DI SERVIZIO", "DRONI SOPRA", "port", 1.14, .48, .77, "MAYA: Le gru sono cieche. I droni no."),
      sector("06-banchine", "BANCHINE NERE", "SEGNATURA ALTA", "port", 1.28, .43, .94, "MAYA: Sei nel loro campo."),
      sector("06-chiatta", "CHIATTA SERVER", "ROUTE A TRE VIE", "port", 1.34, .61, .93, "MAYA: La chiatta centrale tiene tutto.", { fork: true }),
      sector("06-varco", "VARCO ZERO", "ESTRAZIONE IMPOSSIBILE", "extract", 1.38, .22, 1.06, "MAYA: Hai il nodo. Sparisci prima che lo capiscano.", { extract: true }),
    ],
  },
  {
    code: "07", title: "KROSS", subtitle: "Un volto dall'altra parte del fascio di luce", intelGoal: 18,
    briefing: ["Il nodo trasmette una rotta verso il largo, poco prima dell'alba.", "Kross non fugge: vuole che tu lo segua.", "Prendi la registrazione. Non inseguire l'uomo."],
    maya: "Se Kross parla, lascia che finisca. Le persone sicure di vincere dicono sempre troppo.",
    closer: ["Kross ha nominato l'operazione finale: Marea Nera.", "Ha lasciato una rotta per il punto di scarico."],
    sectors: [
      sector("07-ora-blu", "ORA BLU", "ORIZZONTE ESPOSTO", "dawn", 1.17, .56, .82, "KROSS: Una luce piccola in un mare grande."),
      sector("07-cacciatore", "SCIA DEL CACCIATORE", "CONTATTO VISIVO", "dawn", 1.33, .41, .99, "MAYA: Non accelerare per rabbia."),
      sector("07-trappola", "TRAPPOLA APERTA", "ROUTE A TRE VIE", "night", 1.40, .59, .98, "KROSS: Scegli bene. Io ho già scelto per te.", { fork: true }),
      sector("07-impulso", "IMPULSO DI REGISTRAZIONE", "ESTRAZIONE FRAGILE", "extract", 1.43, .20, 1.10, "MAYA: La registrazione è completa.", { extract: true }),
    ],
  },
  {
    code: "08", title: "MAREA NERA", subtitle: "Quando il mare smette di nascondere", intelGoal: 20,
    briefing: ["La rotta finale porta a una piattaforma di scarico durante l'ultima tempesta.", "Qui Black Tide cancella prove, navi e nomi.", "Non c'è una squadra in arrivo. Non ci sarà un secondo passaggio."],
    maya: "Tutta la rete è in ascolto. Fai arrivare i dati.",
    closer: ["La Marea Nera è uscita dai suoi confini.", "Per la prima volta, il mare ha restituito i nomi."],
    sectors: [
      sector("08-onda", "ONDA DI FERRO", "TEMPESTA TERMINALE", "storm", 1.30, .52, .96, "MAYA: Tieni la rotta."),
      sector("08-affondamento", "CAMPO D'AFFONDAMENTO", "RELIQUATI IN ACQUA", "storm", 1.43, .47, 1.08, "MAYA: I relitti sono la loro firma."),
      sector("08-scarico", "PUNTO DI SCARICO", "ROUTE A TRE VIE", "extract", 1.52, .66, 1.12, "KROSS: Anche se invii tutto, il mare resta mio.", { fork: true }),
      sector("08-segnale", "SEGNALE APERTO", "ESTRAZIONE FINALE", "extract", 1.58, .20, 1.20, "MAYA: Tutto il mondo ascolta. Trasmetti.", { extract: true }),
    ],
  },
];

export const CHAPTER_ONE = CHAPTERS[0];
export const SECTORS = CHAPTER_ONE.sectors;

export const MODULES = [
  { id: "engine", title: "MOTORE", tag: "SPINTA", detail: "Boost più efficiente.", costs: [12, 28, 50] },
  { id: "hull", title: "SCAFO", tag: "INTEGRITÀ", detail: "Assorbe l'impatto.", costs: [14, 30, 55] },
  { id: "tank", title: "SERBATOIO", tag: "AUTONOMIA", detail: "Più carburante recuperato.", costs: [10, 24, 44] },
  { id: "radar", title: "RADAR", tag: "AVVISO", detail: "Segnali prima, panico dopo.", costs: [16, 32, 58] },
  { id: "stealth", title: "SILENZIO", tag: "FURTIVITÀ", detail: "Firma ridotta in deriva.", costs: [18, 36, 64] },
] as const;

export type ModuleId = (typeof MODULES)[number]["id"];
export type Modules = Record<ModuleId, number>;
export const DEFAULT_MODULES: Modules = { engine: 0, hull: 0, tank: 0, radar: 0, stealth: 0 };
