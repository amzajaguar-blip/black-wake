# Distribuzione di Marea Nera

## Giocare sul web

Nel progetto, eseguire `pnpm install` e quindi `pnpm dev`. La pagina principale apre il dossier di missione; `?demo` avvia la corsa deterministica per verifiche visive. La build di produzione si crea con `pnpm build`.

## Preparare Android / Google Play

Il contenitore Capacitor è già presente in `android/` e bloccato in landscape. Dopo modifiche al gioco, eseguire `pnpm build` e `pnpm exec cap sync android`; in seguito aprire `android/` con Android Studio e generare un **signed Android App Bundle**. Le risorse originali create per il progetto sono incluse in `generated-assets/` nello ZIP di consegna come archivio grafico di riferimento.

## Contenuto dello ZIP

Lo ZIP include il codice React/Babylon, il progetto Android Capacitor, la build web, i file di pianificazione, la guida Play Store, il rapporto QA e gli asset generati disponibili. Sono esclusi `node_modules`, cache, log ed eventuali build Gradle transitorie perché sono rigenerabili con i comandi sopra riportati.
