# Struttura tecnica — Black Wake (Marea Nera)

## Principio

> React è la cornice; Babylon è il mare; le classi TypeScript sono la fuga.

Il percorso `/` mostra solamente `GameCanvas`. Il componente React inizializza, ridimensiona e distrugge la scena in sicurezza. Le regole non dipendono da React né dal DOM.

## Moduli

| Modulo | Responsabilità |
| --- | --- |
| `components/GameCanvas.tsx` | Ciclo di vita del canvas Babylon, resize e init una sola volta. |
| `game/scene.ts` | Creazione scena, camera, luci, GUI e `GameHandle`. |
| `game/GameWorld.ts` | Stato della missione, ciclo dei settori, aggiornamento e fine partita. |
| `game/InputManager.ts` | Trasforma tastiera e touch in intenzioni semantiche: corsia, boost, silenzio, pausa, mute. |
| `game/SectorDirector.ts` | Sequenza dati dei settori, forche, timer, evento estrazione e radio. |
| `game/PlayerBoat.ts` | Corsa in corsia, carburante, scafo, rilevazione e mesh del giocatore. |
| `game/EntityManager.ts` | Creazione, movimento, raccolta, collisione e cleanup di minacce e pickup. |
| `game/HudController.ts` | Pannelli operativi edge-bound, etichette, radar e messaggi radio. |
| `game/data.ts` | Capitoli, settori, ambienti, dialoghi, costi moduli e parametri di gioco. |

## Modello dei dati

```text
MissionState = menu | briefing | running | paused | debrief | garage
RunState = hull | fuel | intel | intelGoal | detection | score | time | sectorIndex
Sector = id | title | duration | environment | density | threat | intelBias | fork | extract | radio
Entity = id | type | lane | z | speed | damage | value | resolved
InputIntent = laneDelta | boost | silent | pause | mute
```

## Convenzioni visive e asset

La camera guarda verso l'orizzonte lungo il mare. La geometria ripetitiva resta procedurale: piste d'acqua, onde, scie, barriere, barre HUD e pips radar. Le immagini generate restano sui loro URL di storage e sono usate per schermata operativa, barche e identità della plancia; nessun file pesante entra nel repository del gioco.

## Priorità di build

Prima: canvas, modello stato, barca e spawner. Poi: settori e forche. Infine: HUD, touch, pause, debrief e garage. La query `?demo` attiva un pilota deterministico, utile alle verifiche visive.
