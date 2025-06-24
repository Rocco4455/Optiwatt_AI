#  Optiwatt AI – Smart Energy, Smarter Life

**Optiwatt AI** è un'applicazione Android che ti aiuta a monitorare e ottimizzare i consumi energetici domestici. Grazie all’integrazione di tecnologie OCR e intelligenza artificiale, legge e analizza automaticamente le bollette per generare suggerimenti personalizzati e sostenibili.

---

##  Obiettivo del Progetto

Fornire uno strumento:
- **accessibile e smart**, per analizzare i consumi energetici
- **integrato con la domotica**, per un monitoraggio costante
- **basato su AI**, per suggerimenti concreti di risparmio
- **in locale**, per sicurezza, performance e privacy

---

##  Funzionalità Principali

-  **OCR con Tesseract**: estrazione automatica dei dati energetici dalle immagini delle bollette
-  **Analisi IA con DeepSeek**: suggerimenti su misura per ridurre i consumi e ottimizzare le abitudini
-  **Database Locale (Room/SQLite)**: archiviazione sicura e offline di tutte le bollette
-  **Interfaccia Utente Intuitiva**: visualizzazione chiara di consumi, costi, emissioni CO₂, e variazioni nel tempo

---

##  Tecnologie Utilizzate

| Tecnologia      | Utilizzo                                               |
|----------------|--------------------------------------------------------|
| **Kotlin**      | Sviluppo dell'app Android                             |
| **Room / SQLite** | Gestione dati locale, operazioni offline             |
| **Tesseract OCR** | Estrazione testo dalle immagini delle bollette       |
| **DeepSeek AI** | Elaborazione semantica e generazione consigli         |
| **XML**         | Definizione layout e stile dell’interfaccia           |
| **Jira**        | Gestione del team e avanzamento delle attività        |

---

##  Architettura

- `AppDatabase`: istanza centrale SQLite con Room
- `BollettaDao`: operazioni CRUD per gestione bollette
- `DeepSeekAnalyzer`: gestisce richieste IA e parsing risposte
- `TesseractOCR`: modulo di riconoscimento testuale
- `UI (XML + Kotlin)`: dashboard, grafici, popup, e navigazione

---

##  Esempio di flusso

1. L’utente carica una foto della bolletta
2. L’OCR estrae i dati rilevanti
3. I dati vengono puliti e inviati all’IA
4. L’IA restituisce:
   - suggerimenti su abitudini e orari
   - pattern di consumo
   - confronti con periodi precedenti
5. I dati vengono salvati e mostrati nella UI

---

##  Organizzazione del Team

- Task gestiti con **Jira**
- Flusso trasparente con assegnazione, commenti e priorità
- Stato di avanzamento costantemente monitorato

---

##  Prossimi Sviluppi

- Integrazione con dispositivi smart (IoT)
- Dashboard web
- Notifiche predittive
- Supporto multiutente

---

##  Autori e Contributori

- 👤 [@VaccaMario](#)
- 👤 [@Rocco4455](#)
- 👤 [@Daddash](#)

---

## Contatti

Per info, partnership o suggerimenti:

 `mario.vacca002@studenti.uniparthenope.it`  
 `rocco.rizzitano001@studenti.uniparthenope.it`  
 `daniel.strino001@studenti.uniparthenope.it`
 `https://optiwattai.my.canva.site/`

---

## Licenza

Questo progetto è rilasciato con una licenza accademica e non può essere riutilizzato o distribuito senza autorizzazione. Vedi il file Licenza Accademica - Università degli Studi di Napoli Parthenope per maggiori dettagli.


> _"W.A.T.T. – We Are Tomorrow’s Thinkers"_ 
