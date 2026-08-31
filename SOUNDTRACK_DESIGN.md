# Marea Nera — architettura musicale

La colonna sonora è strumentale e mantiene la stessa identità in tutti gli stati: elettronica cinematica, 126 BPM, Re minore, basso sub pulsante, percussioni metalliche asciutte, impulsi sonar ciano e una sirena sintetica rossa riservata all'inseguimento. Non contiene voci, così la radio dinamica può occupare il primo piano senza competizione.

| Stato | Traccia | Funzione nel gioco | Intensità |
| --- | --- | --- | --- |
| Rotta | `route-bed` | Navigazione e raccolta Intel | 3/10 |
| Contatto | `pursuit-bed` | Rilevazione sopra il 64% e inseguitori attivi | 8/10 |
| Estrazione | `uplink-bed` | Settore di trasmissione e finale | 6/10 |

Le transizioni usano dissolvenze brevi e senza stacchi. Le tracce vengono riprodotte a volume prudente rispetto a effetti e radio, con una riduzione ulteriore durante una trasmissione vocale. Tutto l'audio richiede prima un gesto esplicito del giocatore e rispetta il mute esistente.

| Asset pubblicato | Durata verificata | Percorso runtime |
| --- | ---: | --- |
| Route Bed | 142,6 s | `/manus-storage/marea-nera/audio/marea-nera-route-bed_caaa6ee8.mp3` |
| Pursuit Bed | 146,2 s | `/manus-storage/marea-nera/audio/marea-nera-pursuit-bed_86441fda.mp3` |
| Uplink Bed | 117,7 s | `/manus-storage/marea-nera/audio/marea-nera-uplink-bed_2fbb7fea.mp3` |

La build web e la variante Android richiamano ora le tracce dal **File Storage** persistente del progetto. I file sono separati dal repository e rimangono disponibili tramite percorsi firmati `/manus-storage/`. Nel browser, Cache Storage conserva le tracce dopo il primo caricamento quando disponibile. Nell'app Android, il primo download salva ogni MP3 nella cache nativa dell'app e i successivi avvii leggono il file locale: la riproduzione è quindi offline dopo il primo download riuscito. Il mixer permette al giocatore di salvare volumi indipendenti per musica ed effetti sul dispositivo.
