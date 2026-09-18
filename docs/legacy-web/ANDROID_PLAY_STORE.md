# Black Wake — Android / Google Play delivery

This project is wired to be wrapped into a native Android app via Capacitor. The game is designed for **landscape**, touch, fullscreen and with margins that respect the rounded corners of modern devices.

| Field | Initial value |
| --- | --- |
| App name | Black Wake |
| Application ID | `com.frenzy_rush` |
| Initial version | `1.0.0` |
| Web bundle | `dist/public` |
| Orientation | Landscape |
| Min SDK recommended | Android 24 |

## Generazione del progetto Android

Dal terminale nella radice del progetto, eseguire `pnpm sync:android`; il comando crea la build web e sincronizza Capacitor. La colonna sonora richiama gli asset musicali web pubblicati e necessita di connettività per la prima riproduzione. Per aprire il progetto nativo in Android Studio, eseguire `pnpm exec cap open android`. La prima build per Play deve essere un **Android App Bundle** firmato (`.aab`), con keystore privata custodita fuori dal repository.

## Prima pubblicazione

Prima della pubblicazione occorre sostituire l'icona di test del progetto Android con l'icona finale approvata, impostare screenshot reali della build Android e completare in Play Console la scheda dello store, la classificazione dei contenuti, l'indirizzo e-mail di assistenza e le dichiarazioni richieste. Poiché Marea Nera non richiede account, raccolta dati o pubblicità nella prima build, la sezione privacy va verificata nuovamente se queste funzionalità vengono aggiunte in futuro.

> Il file di firma e le credenziali del tuo account sviluppatore Google non sono inclusi e non devono essere inseriti nello ZIP del progetto.
