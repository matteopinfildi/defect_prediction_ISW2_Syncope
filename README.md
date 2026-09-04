# Progetto di Defect Prediction: Apache Syncope

Progetto di Defect Prediction (ISW2). Progetto accademico realizzato per il corso di Laurea Magistrale in Cybersecurity presso l'Università di Roma Tor Vergata.

 **[Clicca qui per scaricare e leggere il Report completo del Progetto (Report_Pinfildi_SYNCOPE.pdf)](Report_Pinfildi_SYNCOPE.pdf)**

## Domande di Ricerca (RQ)
Il progetto è stato strutturato per rispondere a tre principali domande di ricerca:
1. Qual è il miglior classificatore per predire con accuratezza la difettosità delle classi?
2. Come cambia la probabilità di difetto predetta dal miglior classificatore se si simula l'assenza totale di code smell?
3. Durante la fase di refactoring automatico con LLM, il modello introduce nuovi smell? Le metriche correlate alla probabilità di difetto migliorano o peggiorano?

## Metodologia e Fasi del Progetto (Milestones)

*   **Milestone 1 (Estrazione Dati e Labeling):** Analisi dell'evoluzione storica su 27 release di Apache Syncope. Sono state estratte feature strutturali (tramite CK), metriche di evoluzione e code smell (tramite PMD) su un totale di 17023 istanze. Il labeling della difettosità (Bugginess) è stato implementato tramite l'algoritmo SZZ basato sui ticket Jira.
*   **Milestone 2 (Machine Learning):** Valutazione di tre classificatori (Random Forest, Naive Bayes, IBk) tramite l'ambiente Weka con tecnica 10-fold cross-validation. Sono state testate 12 configurazioni incrociando l'applicazione di Feature Selection (Forward greedy) e Balancing delle classi (Undersampling).
*   **Milestone 3 (What-If Analysis):** Valutazione dell'impatto isolato della metrica `NSMELLS`. È stato manipolato il dataset forzando gli smell a zero (simulando un "refactoring perfetto") per misurare la variazione delle predizioni del classificatore.
*   **Milestone 4 (Refactoring tramite LLM):** Esperimento di refactoring progressivo tramite Microsoft Copilot su due classi target, vincolando la generazione al superamento di test suite (Black-Box, Control Flow, Mutation Driven). Valutazione dell'impatto architetturale calcolando la correlazione di Spearman tra le metriche strutturali (WMC, RFC, LCOM, CBO, LOC) e la difettosità.

## Risultati Principali

*   **Best Classifier:** Il modello più performante è risultato essere **Random Forest senza Feature Selection e senza Balancing**, ottenendo un'AUC di 0.92 e un valore Kappa di 0.61.
*   **Impatto dei Code Smell:** La What-If Analysis ha dimostrato che i code smell sono un'informazione utile, ma la loro sola eliminazione artificiale non riduce in modo drastico la stima dei difetti del software (incremento di falsi positivi).
*   **Valutazione Refactoring LLM:** L'uso degli LLM per il refactoring riduce parzialmente il numero di smell, ma porta a un generale peggioramento delle metriche strutturali del codice (aumento di WMC, RFC e frammentazione della coesione LCOM). L'ottimizzazione dell'LLM sacrifica l'architettura per rimuovere gli smell, dimostrando che la supervisione umana resta indispensabile.
