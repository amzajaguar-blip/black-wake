# Verifica media — Release offline

La release è stata controllata dopo l'upgrade a File Storage e l'integrazione del plugin nativo di filesystem. Le tre tracce vengono scaricate dal File Storage persistente; nell'app Android, dopo il primo download, vengono archiviate nella cache dell'app e riutilizzate ai successivi avvii anche senza rete.

| Ambito | Verifica | Esito |
| --- | --- | --- |
| File Storage | Tre MP3 presenti e referenziati con percorsi `/manus-storage/` | Superata |
| Cache Android | Plugin `@capacitor/filesystem` sincronizzato e code path `stat`/`writeFile`/`Cache` presente | Superata |
| Mixer | Slider Musica ed Effetti, valori 72% e 78% al primo avvio, preferenze persistenti | Superata |
| Trailer | MP4 1280×720, 8 secondi, video e audio presenti | Superata |
| Screenshot | Feature graphic più cinque PNG promozionali | Superata |
| Qualità codice | 5 test Vitest, controllo TypeScript e 100 cicli ricorsivi | Superati |

> La prima riproduzione offline richiede naturalmente una connessione, necessaria a trasferire le tracce da File Storage nella cache nativa. I test automatizzati validano il percorso di cache; il collaudo fisico finale senza rete richiede un dispositivo Android con l'AAB firmato.
