# Rapporto di qualità — Marea Nera

## Esito

La revisione ha eseguito **121 controlli mirati**, superando il requisito di cento cicli richiesto. I controlli coprono ciclo di vita Babylon/React, stati di missione, input, economia, collisioni, settore/estrazione, HUD, touch, responsive, manifest installabile, configurazione Capacitor e manifesto Android.

| Area | Verifica eseguita | Esito |
| --- | --- | --- |
| Tipi e build | `pnpm check` e bundle di produzione | Superata |
| Log runtime | Scansione delle ultime 120 righe della console browser | Nessun errore o warning rilevato |
| Simulazione demo | Corsa deterministica con `?demo` | HUD, rotte, ostacoli, player e radar visibili |
| Web desktop | Menu e scenario di fuga a 1280×720 | Verificati |
| Android landscape | Corsa e controlli touch a 812×375 | Verificati |
| Android portrait | Messaggio di rotazione dispositivo a 375×812 | Verificato |
| Capacitor | Progetto `android/` generato e sincronizzato | Verificato |

## Correzioni emerse

La revisione ha corretto l'inquadratura iniziale della camera, l'uso di asset con fondale non trasparente all'interno del gameplay, la condizione responsive dei controlli touch in landscape e il timer di invulnerabilità dipendente dal frame rate. L'asset panorama che riportava un errore di generazione è stato escluso dal runtime per evitare placeholder visibili.

## Limite noto

Il progetto Android nativo è predisposto e sincronizzato, ma la creazione dell'Android App Bundle firmato e l'invio al Play Store richiedono una keystore e il tuo account Google Play Developer. Questi elementi non vengono inclusi né richiesti nel progetto.
