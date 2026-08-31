# Game Plan: Black Wake (Marea Nera)

## Obiettivo

Creare un gioco browser originale di inseguimento navale a tre corsie. Il giocatore guida Elias Vane attraverso i settori di una fuga notturna, scegliendo rotte sicure, ricompense d'intelligence o passaggi volatili, per portare il dispositivo Black Tide a una trasmissione finale.

## Risk Tasks

### 1. Gioco Babylon in un host React
- **Perché è isolato:** React 19 può montare due volte in sviluppo; un doppio motore o listener di input renderebbe il canvas instabile e i comandi duplicati.
- **Approccio:** il componente React è esclusivamente la cornice del canvas; `createGameScene()` costruisce una sola scena Babylon e restituisce un handle con cleanup completo. Tutta la simulazione resta in classi TypeScript indipendenti.
- **Verifica:** il canvas mantiene proporzioni corrette dopo un ridimensionamento, risponde a tastiera e touch senza doppio movimento, e nessun errore appare nella console durante una sessione completa.

### 2. Simulazione a corsie e collisioni leggibili
- **Perché è isolato:** la pressione arcade deve dare tempo di leggere corsie e ostacoli, senza sovrapposizioni o collisioni punitive non telegraphed.
- **Approccio:** posizioni discrete su tre corsie, oggetti mossi lungo l'asse di avanzamento, spawn dati dal settore corrente e rilevamento mediante distanza/corsia. Una breve invulnerabilità impedisce danni multipli sullo stesso impatto.
- **Verifica:** sinistra/destra porta sempre alla corsia attesa; oggetti e rotte hanno sagome riconoscibili; un singolo ostacolo infligge un solo danno per passaggio.

### 3. Crescita della tensione tramite settori, rilevazione ed estrazione
- **Perché è isolato:** il gioco non può degradare in raccolta casuale; deve alternare lettura, decisione e conseguenza fino all'uscita finale.
- **Approccio:** un programmatore di settori da 12–16 secondi annuncia il tratto, applica tabelle di spawn per ambiente, gestisce forche con ricompense fisse e abilita un unico finale di trasmissione. Boost e navigazione silenziosa modificano carburante e rilevazione.
- **Verifica:** un fork appare prima dell'estrazione; lo stato SILENZIO abbassa il segnale sul radar entro due secondi; non si vince raccogliendo solo INTEL, ma sopravvivendo alla trasmissione.

## Main Build

La build iniziale include una schermata operativa di apertura, un briefing di missione, un capitolo giocabile completo “Il Relitto”, una debrief screen e un garage di moduli leggeri. La scena di gioco visualizza il mare a tre corsie, barca di Elias, pattuglie Black Tide, relitti, mine, carburante e capsule di intelligence. I controlli supportano tastiera e touch: direzione, boost, navigazione silenziosa, pausa e mute.

- **Assets:** scena guida di gameplay, barca del giocatore, barca Black Tide, panorama di porto e simbolo del marchio, più forme procedurali per HUD, onde, ostacoli e pickup minori.
- **Verifica:**
  - movimento, boost e silenzio hanno risposta immediata e stati HUD visibili;
  - HULL, FUEL, INTEL, SIGNAL e timer restano leggibili senza sovrapporsi;
  - l'intelligence raccolta incrementa il valore; i danni e il carburante scarso hanno conseguenze;
  - partita, pausa, vittoria, sconfitta, debrief e ritorno al briefing funzionano;
  - nessun asset mancante o placeholder visibile;
  - la cattura `?demo` mostra una situazione giocabile reale e coerente con `ASSETS.md`;
  - nessun errore di tipo o console nella sessione verificata.

## Fuori perimetro della prima build

La prima consegna non include multiplayer, fisica navale realistica, combattimento a fuoco come loop primario, backend o account. Il gioco conserva invece la premessa, i personaggi e la progressione narrativa di Marea Nera in una forma arcade autonoma.
