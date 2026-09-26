# Black Wake

> Thriller nautico operativo. Tre corsie, una scia, nessun porto sicuro.

**Black Wake** è un gioco Android nativo (Kotlin + Jetpack Compose). Elias Vane porta in mare una
prova che Black Tide vuole cancellare: ogni capitolo è una fuga a tre corsie fatta di lettura del
mare, scelte di rotta e una sola finestra di estrazione.

Il gioco è in italiano; il marchio e il codice sono in inglese.

## Build

```bash
./gradlew assembleDebug        # APK di debug
./gradlew testDebugUnitTest    # test unitari JVM della simulazione
./gradlew installDebug         # installa su un dispositivo collegato
./gradlew bundleRelease        # Android App Bundle per Google Play (richiede firma)
```

Richiede JDK 17 e l'SDK Android (compileSdk 36). Il wrapper Gradle è incluso: non serve una
installazione di Gradle di sistema.

| | |
| --- | --- |
| Application ID | `com.frenzy_rush` |
| Namespace sorgenti | `com.blackwake.game` |
| minSdk / targetSdk | 26 / 36 |
| Orientamento | Landscape |
| Permessi | `VIBRATE` |

## Come si gioca

| Comando | Effetto |
| --- | --- |
| Trascina sul mare | Sposta la barca fra le tre corsie (corsie magnetiche con inerzia) |
| Leva di potenza (in basso a sinistra) | Alta: veloce, consuma carburante, alza la firma. Bassa: silenzio, la firma cala, combo intel |
| IMMERGI (tieni premuto) | Passa sotto relitti e pattuglie. Consuma ossigeno; le mine restano letali |
| Sonar (automatico) | Quando ti danno la caccia pulsa da solo e rivela i pericoli nella nebbia |
| RAZZO | Acceca gli inseguitori e li fa perdere il contatto |
| Pausa / tasto indietro | Sospende la missione, mappa tattica e registro di bordo |

**Obiettivo di ogni capitolo:** raccogliere almeno il 70% dell'intel prima che si chiuda la finestra
di estrazione. La firma (`FIRMA`) è la risorsa centrale: quando supera il 70% Black Tide manda un
inseguitore, oltre il 91% arriva un'ondata. Navigare in silenzio o in immersione la fa scendere.

## Struttura

```
app/src/main/java/com/blackwake/game/
├── GameData.kt        # 8 capitoli, settori, moduli, vascelli
├── GameModel.kt       # stato: RunState (missione) e Progress (persistente)
├── GameSimulation.kt  # regole pure: un frame di gioco, senza dipendenze Android
├── PursuerAI.kt       # macchina a stati degli inseguitori
├── GameViewModel.kt   # input, eventi audio/haptic, salvataggio, ciclo di vita
├── ProgressStore.kt   # persistenza SharedPreferences
├── SynthAudioEngine.kt# motore audio procedurale (un solo thread audio)
├── MusicDirector.kt   # colonna sonora a tre stati con dissolvenze
├── SeaRenderer.kt     # proiezione prospettica e disegno del mare
├── Hud.kt             # HUD di missione
├── Screens.kt         # menu, briefing, officina, pausa, debriefing
└── BlackWakeApp.kt    # root Compose, frame loop, input
```

La simulazione è pura e deterministica (il generatore casuale è iniettato): è coperta da test JVM in
`app/src/test/` e non richiede un dispositivo.

## Verifica

Ogni push esegue in CI (`.github/workflows/android.yml`):

1. `assembleDebug`, test unitari e lint;
2. uno **smoke test su emulatore** che installa l'APK, attraversa menu → officina → briefing →
   missione → pausa → ripresa, verifica il comportamento del tasto indietro e del ritorno da
   background, e archivia screenshot, gerarchia UI e logcat. Fallisce su qualunque crash.

## Distribuzione

Vedi `docs/store/` per i testi Google Play e `play-store/` per grafiche e screenshot.

> ⚠️ Gli screenshot in `play-store/` provengono dalla vecchia build web e **non** corrispondono più
> al gioco: vanno rifatti dalla build nativa prima della pubblicazione.

`docs/design/` contiene la direzione creativa e l'architettura musicale; `docs/legacy-web/`
conserva la documentazione della versione web Babylon.js da cui nasce il progetto.
