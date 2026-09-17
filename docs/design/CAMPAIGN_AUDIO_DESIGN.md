# Campagna e audio — Black Wake (Marea Nera)

## Progressione

La campagna completa trasforma gli otto capitoli narrativi dell'utente in otto fughe autonome. Ogni missione conserva il nucleo **leggi → scegli → fuggi → estrai**, ma cambia ambiente, pressione, obiettivo INTEL e interventi di Maya/Kross. Un capitolo è completato solo quando il giocatore raggiunge la finestra di estrazione con almeno il 70% della sua quota dati.

| Capitolo | Titolo | Ambiente | Pressione distintiva |
| --- | --- | --- | --- |
| 01 | Il Relitto | Mare notturno | Prima fuga e prova recuperata. |
| 02 | La Contrabbandiera | Porto | Rotte strette e pattuglie. |
| 03 | Isola Morta | Arcipelago | Relitti, casse e silenzio sospetto. |
| 04 | Caccia nella Tempesta | Tempesta | Carburante e scafo sotto stress. |
| 05 | Il Tradimento | Mare notturno | Inseguimento senza fase sicura. |
| 06 | Il Porto Nero | Porto nero | Rilevazione alta e droni. |
| 07 | Kross | Alba | Contatto narrativo e pressione costante. |
| 08 | Marea Nera | Tempesta | Finale esteso e trasmissione lunga. |

## Audio runtime

L'audio usa Web Audio API e viene avviato soltanto dopo un'azione del giocatore. Una base motore controllata in tempo reale comunica velocità, boost e navigazione silenziosa. Effetti brevi e distinti segnalano cambio corsia, raccolta INTEL, recupero carburante, impatto, contatto e uscita.

La radio dinamica abbina sempre sottotitolo HUD e tono di trasmissione; quando il browser supporta la sintesi vocale italiana, Maya o Kross vengono letti con voce rallentata e filtrata dalla sequenza di beep radio. Se la sintesi non è disponibile, la linea rimane leggibile sullo schermo e il feedback sonoro non blocca il gioco.

## Escalation aggressiva

Al superamento della soglia di rilevazione, la plancia abbandona temporaneamente il tono neutro: compaiono una fascia di contatto rossa, radar pulsante, bordo d'allarme e stato tattico caldo. Il rosso rimane rigorosamente riservato alla minaccia; ciano e ambra continuano a distinguere navigazione, dati e carburante.

## Regole anti-ripetizione

Le linee radio hanno un cooldown e le chiamate di contatto cambiano in base alla soglia di rilevazione. Gli effetti di boost partono soltanto al passaggio nello stato boost, non ogni frame. Gli allarmi hanno priorità sulle linee ambientali e la radio non interrompe l'estrazione finale.
