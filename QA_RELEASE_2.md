# Rapporto qualità — Release campagna estesa

## Esito della revisione

La release con i capitoli dal 2 all'8 ha completato la suite storica di **142 controlli** e la nuova revisione ricorsiva di **100 cicli**. Le due suite sono complementari: la prima controlla la struttura generale di rendering, input, stati, Android e accessibilità; la seconda esegue invarianti ripetute su campagna, audio, progressione, ambienti, escalation e materiali Play Store.

| Area | Controllo | Risultato |
| --- | --- | --- |
| Campagna | Otto capitoli, 32 settori, 8 fork e 8 estrazioni | Superato |
| Progressione | Dossier bloccati, sblocco e salvataggio locale | Superato |
| Audio | Effetti Web Audio, motore adattivo, mute e radio italiana | Superato |
| Tema | Allarme rosso, radar in escalation, motion ridotto rispettato | Superato |
| Mobile | Controlli touch e orientamento Android landscape | Superato |
| Google Play | Testi, feature graphic e quattro screenshot reali | Pronti per caricamento |
| Release | `pnpm check`, build di produzione e `cap sync android` | Superati |

## Verifiche visive

Sono state controllate la pagina di campagna, una fuga di porto, la tempesta e la pressione del capitolo finale. Il controllo di contatto alto conferma che fascia rossa, radar, colori di minaccia e leggibilità delle rotte coesistono senza cancellare la gerarchia ciano della navigazione.

## Note per la pubblicazione

Il progetto Android è sincronizzato, ma la firma dell'Android App Bundle e l'invio in Play Console devono essere eseguiti dal titolare dell'account sviluppatore. La feature graphic inclusa deriva dal riferimento nautico fornito e non contiene testo. Una nuova feature graphic generativa non è stata aggiunta perché la quota gratuita giornaliera di generazione immagini era già esaurita; gli screenshot reali e l'asset 1024×500 incluso restano utilizzabili per predisporre la scheda.
