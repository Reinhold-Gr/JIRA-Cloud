# Migrationsplan: Jira Data Center → Cloud
**AktivSenioren Bayern e.V.**
Stand: September 2026

---

## 1. Ausgangslage & Zielsetzung

| | |
|---|---|
| **Quelle** | Jira Data Center (On-Premise) |
| **Ziel** | Jira Cloud (Sandbox bereits vorhanden: `aktivsenioren-sandbox.atlassian.net`) |
| **Treiber** | Atlassian-Roadmap (Verkaufsstopp neuer DC-Lizenzen ab Q1 2026, DC End-of-Life 28.03.2029), Wegfall Infrastrukturbetrieb, Zugang zu neuen Cloud-Features |
| **Kritische Komponenten** | Better PDF Exporter (XSL-FO/Velocity-Templates), ~8 PDF-Templates inkl. Empfangsbestätigung/Rechnung, individuelles Custom-Field-Mapping |

**Ziel dieses Plans:** strukturierte, risikoarme Migration mit funktionsfähiger PDF-Erzeugung und sauberem Berechtigungskonzept ab Tag 1 in der Cloud.

---

## 2. Migrationsphasen

### Phase 0 – Vorbereitung (abgeschlossen/laufend)
- Sandbox-Cloud-Instanz eingerichtet
- Custom-Field-Mapping-Toolchain aufgebaut (PowerShell + Groovy, `CustomFieldMapping.json`)
- Erste PDF-Templates (u. a. `initial-confirmation-fo.vm`) auf Cloud-Kompatibilität portiert

### Phase 1 – Bereinigung & Analyse (2–3 Wochen)
- Integrity Check auf der DC-Instanz durchführen, Fehler beheben
- Nicht mehr benötigte Projekte, Custom Fields, inaktive Nutzerkonten identifizieren und bereinigen
- Marketplace-Apps auf Cloud-Verfügbarkeit prüfen (Better PDF Exporter: Cloud-Version vorhanden, API-Unterschiede bekannt und dokumentiert)
- Vollständige Inventarisierung aller Custom Fields, Workflows, Berechtigungsschemata

### Phase 2 – Testmigration in Sandbox (3–4 Wochen)
- Migrationswerkzeug festlegen: **Jira Cloud Migration Assistant** (native, kostenlos) vs. **Configuration Manager for Jira (CMJ)** von Appfire (granularer, mit „Analyze Changes“-Vorschau)
  - Empfehlung: CMJ, sofern granulare Kontrolle über Custom-Field- und Workflow-Mapping nötig ist – bei eurem individuellen Field-Mapping-Ansatz vermutlich sinnvoll
- Testmigration eines repräsentativen Projekt-Subsets in die Sandbox
- Custom-Field-Mapping gegen reale Cloud-Field-IDs validieren
- Alle 8 PDF-Templates in der Sandbox durchtesten (Layout, Kopf-/Fußzeilen, Verknüpfungen via `$issueLinkManager.getLinkCollection()`)
- Ergebnisse dokumentieren, Fehler beheben, Mapping-Skripte nachschärfen

### Phase 3 – Produktivmigration (1 Wochenende / Wartungsfenster)
- Migration außerhalb der Geschäftszeiten (Vereinsbetrieb minimal)
- Vollständiger Datentransfer: Projekte, Issues, Anhänge, Kommentare, Historie, Nutzer, Berechtigungen
- Direkt im Anschluss: Custom-Field-Mapping final anwenden, PDF-Templates deployen

### Phase 4 – Validierung & Go-Live (1 Woche)
- Stichprobenprüfung: Issues, Anhänge, Kommentarhistorie
- PDF-Ausgabe-Test aller Templates mit echten (migrierten) Daten
- Nutzerkommunikation: neue URLs, ggf. neuer Login-Mechanismus (Atlassian-Konto)
- DC-Instanz zunächst read-only weiterlaufen lassen als Fallback (2–4 Wochen Übergangsfrist)

### Phase 5 – Nachbetreuung (laufend)
- DC-Instanz nach Bestätigung der Stabilität stilllegen
- Dokumentation der finalen Cloud-Konfiguration
- Lessons Learned festhalten

---

## 3. Sicherheitsbetrachtung

### 3.1 Datenschutz & Datenresidenz
- Prüfen, wo Atlassian die Cloud-Daten hostet (Data Residency-Einstellungen in Jira Cloud verfügbar) – für einen deutschen Verein relevant im Hinblick auf DSGVO
- Auftragsverarbeitungsvertrag (AVV) mit Atlassian abschließen/prüfen, bevor personenbezogene Daten (Mitgliederdaten in Issues/Custom Fields) migriert werden
- Klären, welche Daten in den PDF-Templates (Empfangsbestätigungen, Rechnungen) personenbezogen sind und entsprechend geschützt werden müssen

### 3.2 Zugriffskontrolle während der Migration
- Migrationskonten (API-Token, Sandbox-User) nur mit minimal notwendigen Rechten ausstatten
- API-Token (`JIRA_API_TOKEN_CL`) zeitlich befristen und nach Abschluss der Migration rotieren/widerrufen
- Zugriff auf `CustomFieldMapping.json` und Migrations-Skripte auf den engsten Kreis der technisch Verantwortlichen beschränken

### 3.3 Transport- & Speichersicherheit
- Sicherstellen, dass alle Migrationswerkzeuge (CMJ/Migration Assistant) Daten verschlüsselt übertragen (Standard bei Atlassian-Tools, aber bei Drittanbieter-Apps prüfen)
- Backup der DC-Instanz **vor** der Produktivmigration (vollständiges XML-/DB-Backup), das unabhängig vom Migrationswerkzeug aufbewahrt wird

### 3.4 Absicherung der PDF-Erzeugung
- Prüfen, ob Better PDF Exporter Cloud sensible Felder standardmäßig in generierten PDFs anzeigt, die nicht angezeigt werden sollen
- Zugriff auf generierte PDF-Dokumente (Rechnungen, Empfangsbestätigungen) über Jira-Berechtigungen absichern, nicht nur über Obskurität der URL

### 3.5 Notfall-/Rollback-Plan
- DC-Instanz bleibt bis zur bestätigten Stabilität der Cloud-Instanz produktiv nutzbar (read-only-Fallback)
- Definierte Abbruchkriterien für die Produktivmigration (z. B. Datenintegritätsfehler > X %, PDF-Generierung nicht funktionsfähig)

---

## 4. Userkonzept (Cloud)

### 4.1 Kontenmodell
- Umstellung von lokaler/DC-Authentifizierung auf **Atlassian-Konten** (E-Mail-basiert)
- Vorab prüfen: Alle Nutzer im DC-System haben eine gültige, aktive E-Mail-Adresse (Pflichtvoraussetzung für Cloud-Migration)
- Für den Verein relevant: Unterscheidung zwischen internen Mitarbeitenden/Ehrenamtlichen (volle Lizenzplätze) und ggf. externen Beteiligten (eingeschränkter Zugriff, z. B. über Portal/Kundenzugang statt volle Lizenz)

### 4.2 Rollen- & Berechtigungskonzept
- Berechtigungsschemata aus DC **nicht 1:1 übernehmen**, sondern in der Cloud neu bewerten – Cloud-Berechtigungsmodell unterscheidet sich strukturell (u. a. Projektrollen, globale Berechtigungen, Gruppen vs. Rollen)
- Empfohlene Struktur:
  - **Org-Admin**: zentrale Verwaltung (möglichst wenige Personen)
  - **Projekt-Admins**: projektbezogene Konfiguration (z. B. für einzelne Vereinsbereiche/Projekte)
  - **Standardnutzer**: Bearbeitung von Issues gemäß Vereinsaufgabe
  - **Read-only/Beobachter**: für Vorstand/Prüfende ohne Bearbeitungsbedarf
- Gruppenbasierte statt individuelle Rechtevergabe, um Pflege bei Personalwechsel (typisch bei Ehrenamt) zu erleichtern

### 4.3 Lizenzplanung
- Nutzerzahl vor Migration final festlegen (Lizenzkosten in Cloud sind nutzerbasiert, anders als DC)
- Inaktive/Doppel-Accounts vor Migration bereinigen, um Lizenzkosten nicht unnötig zu erhöhen

### 4.4 Onboarding & Kommunikation
- Kurzanleitung für Nutzer: neue Login-URL, Passwort-Reset-Prozess bei Erstanmeldung
- Ansprechpartner für Rückfragen während der Übergangsphase benennen
- Hinweis an alle Nutzer, wann die DC-Instanz endgültig abgeschaltet wird

---

## 5. Grober Zeitplan

| Phase | Dauer | Zeitraum (Beispiel) |
|---|---|---|
| Vorbereitung | – | bereits laufend |
| Bereinigung & Analyse | 2–3 Wochen | KW X–X |
| Testmigration Sandbox | 3–4 Wochen | KW X–X |
| Produktivmigration | 1 Wochenende | KW X |
| Validierung & Go-Live | 1 Woche | KW X |
| Nachbetreuung | laufend | ab KW X |

---

## 6. Offene Punkte / Nächste Schritte
- Entscheidung: Migration Assistant vs. Configuration Manager (CMJ)
- Klärung Data-Residency-Standort bei Atlassian
- AVV-Prüfung/-Abschluss mit Atlassian
- Finale Nutzerliste inkl. Rollenzuordnung erstellen
