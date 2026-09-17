# Memoria di produzione — Black Wake (Marea Nera)

## Decisioni prese

Il gioco mantiene l'ambientazione dell'utente: Elias Vane fugge da Black Tide con un dispositivo di prova, guidato a distanza da Maya Serrano. Il primo capitolo trasformato in una missione arcade è **Il Relitto**. Il riferimento esterno ispira soltanto categorie di dinamiche arcade ad alta leggibilità; codice, storia, nomi, livelli, progressione e design Marea Nera restano originali.

Il loop è: **annuncio settore → lettura delle tre corsie → decisione fra sicuro/intel/volatile → esecuzione → conseguenza → estrazione**. Il boost aumenta velocità e segnale; silenzio riduce segnale e velocità. L'estrazione è l'unico modo di completare la missione.

## Vincoli verificabili

Il gioco deve supportare A/D e frecce per corsia, Spazio/W per boost, S/Freccia giù per silenzio, P/Esc per pausa, M per mute; deve offrire anche controlli touch. Alla prima consegna sono centrali campagna, sector loop, HUD e debrief. L'arcade infinito e gli otto capitoli completi restano evoluzioni successive.

## Correzioni rilevanti

Nella prima ispezione della modalità `?demo`, l'HUD era visibile ma la camera non comprendeva la prua e la superficie di gioco, perché puntava troppo lontano all'orizzonte. La camera è stata abbassata, avvicinata e dotata di un angolo visivo più aperto, in modo che barca, corsie e fondale siano contemporaneamente leggibili.

La seconda ispezione ha evidenziato che alcune immagini supplementari pensate come sprite mostrano un fondale nel runtime. Pickup, relitti e pattuglie sono stati quindi promossi a geometrie Babylon stilizzate, che mantengono sagoma, contrasto e illuminazione corretti in qualsiasi schermo. Gli asset generati restano documentati come riferimento di materia e lingua visiva; la scena usa il visual target e il simbolo ufficiale nei punti in cui hanno un risultato affidabile.

La prova Android in orizzontale ha mostrato che una regola basata solo sulla larghezza non attivava i controlli touch a 812×375. La condizione responsive ora include i display landscape bassi, rendendo disponibili sia sterzo sia boost e silenzio nei formati tipici degli smartphone.

Un controllo del ciclo di simulazione ha rilevato che il contatore di invulnerabilità diminuiva una quota fissa a ogni frame anziché in rapporto al tempo trascorso. È stato spostato nell'aggiornamento con `delta time`, riportando la finestra post-impatto all'intervallo previsto e impedendo danni seriali accidentali.

Il fondale del menu è stato impostato sul panorama senza testo né controlli pre-renderizzati; il visual target più dettagliato viene ora riservato al briefing dove agisce da documento operativo. In questo modo l'interfaccia di Marea Nera resta l'unica UI che il giocatore legge.

L'asset panorama è risultato non disponibile nella verifica successiva. È stato quindi escluso: il menu usa il visual target funzionante e il briefing usa una carta radar procedurale. In questo modo nessuna schermata mostra placeholder o messaggi di errore provenienti dalla generazione degli asset.

## Asset riservati

I cinque asset generati sono in `ASSETS.md` con URL persistenti del progetto. Non vanno copiati nella cartella del gioco. I pacchetti utente estratti in `/home/ubuntu/marea-full-source` rimangono riferimento per testi e non vengono eseguiti.
