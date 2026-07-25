#!/usr/bin/env python3
"""
Jira Issue Exporter – aktivsenioren-sandbox.atlassian.net
Exportiert Issues mit allen Custom Fields nach CSV und JSON.
"""

import csv
import json
import os
import requests
from requests.auth import HTTPBasicAuth

# ─────────────────────────────────────────────
# KONFIGURATION
# ─────────────────────────────────────────────
JIRA_URL   = "https://aktivsenioren-sandbox.atlassian.net"
EMAIL      = os.environ.get("JIRA_EMAIL", "deine-email@example.com")
API_TOKEN  = os.environ.get("JIRA_TOKEN", "dein-api-token")

AUTH    = HTTPBasicAuth(EMAIL, API_TOKEN)
HEADERS = {"Accept": "application/json"}

# ─────────────────────────────────────────────
# BEKANNTE CUSTOM FIELDS (aus deiner Instanz)
# ─────────────────────────────────────────────
# Ergänze oder entferne Felder nach Bedarf.
CUSTOM_FIELDS = {
    "Auftragstyp":                    "customfield_10100",  # Beratungsauftrag etc.
    "Beratungsfall":                  "customfield_10101",
    "Mitgliedsnummer-Basis":          "customfield_10102",
    "Rechnungsnummer":                "customfield_10103",
    "Bezahlstatus":                   "customfield_10104",
    "Auftragsstatus":                 "customfield_10105",
    "Region":                         "customfield_10106",
    "Eingangsart":                    "customfield_10107",
    "Fachthemen":                     "customfield_10108",
    "Auftragsbeginn":                 "customfield_10109",
    "Gesamtaufwand in Std.":          "customfield_10110",
    "Betrag 1":                       "customfield_10111",
    "Steuersatz in Prozent":          "customfield_10112",
    "Feedback-Status":                "customfield_10113",
    "Auftragsbestätigung-Status":     "customfield_10114",
    "Abschlussbericht-Status":        "customfield_10115",
    "Rechnung gesendet":              "customfield_10116",
    "Kontodaten":                     "customfield_10117",
    "Intern":                         "customfield_10118",
}


# ─────────────────────────────────────────────
# SCHRITT 1: Alle Felder der Instanz abrufen
#            und Custom Field IDs ermitteln
# ─────────────────────────────────────────────
def get_all_fields() -> dict:
    """Gibt ein Dict {Feldname: fieldId} aller Felder zurück."""
    url = f"{JIRA_URL}/rest/api/3/field"
    resp = requests.get(url, auth=AUTH, headers=HEADERS)
    resp.raise_for_status()
    fields = {}
    for f in resp.json():
        fields[f["name"]] = f["id"]
        if f.get("custom"):
            print(f"  Custom Field: {f['name']:45s} → {f['id']}")
    return fields


# ─────────────────────────────────────────────
# SCHRITT 2: Issues per JQL abrufen (paginiert)
# ─────────────────────────────────────────────
def search_issues(jql: str, fields: list[str], max_results: int = 100) -> list[dict]:
    """Führt eine JQL-Suche durch und gibt alle Issues zurück."""
    url = f"{JIRA_URL}/rest/api/3/search"
    all_issues = []
    start_at = 0
    page_size = min(50, max_results)

    while len(all_issues) < max_results:
        params = {
            "jql":        jql,
            "startAt":    start_at,
            "maxResults": page_size,
            "fields":     ",".join(fields),
        }
        resp = requests.get(url, auth=AUTH, headers=HEADERS, params=params)
        resp.raise_for_status()
        data = resp.json()

        issues = data.get("issues", [])
        if not issues:
            break

        all_issues.extend(issues)
        start_at += len(issues)
        total = data.get("total", 0)
        print(f"  Geladen: {len(all_issues)} / {min(total, max_results)}")

        if start_at >= total or start_at >= max_results:
            break

    return all_issues[:max_results]


# ─────────────────────────────────────────────
# SCHRITT 3: Feldwert sicher extrahieren
# ─────────────────────────────────────────────
def extract_value(value) -> str:
    """Konvertiert beliebige Jira-Feldwerte in einen lesbaren String."""
    if value is None:
        return ""
    if isinstance(value, str):
        return value
    if isinstance(value, (int, float, bool)):
        return str(value)
    if isinstance(value, dict):
        # Häufige Jira-Objekte: User, Status, Priority, IssueType …
        for key in ("displayName", "name", "value", "key", "id"):
            if key in value:
                return str(value[key])
        return json.dumps(value, ensure_ascii=False)
    if isinstance(value, list):
        return "; ".join(extract_value(v) for v in value)
    return str(value)


# ─────────────────────────────────────────────
# SCHRITT 4: Issues in CSV exportieren
# ─────────────────────────────────────────────
def export_to_csv(issues: list[dict], filename: str = "jira_export.csv"):
    if not issues:
        print("Keine Issues zum Exportieren.")
        return

    # Spalten: Standardfelder + Custom Fields
    standard_cols = ["Key", "Zusammenfassung", "Status", "Vorgangstyp",
                     "Priorität", "Zugewiesen an", "Ersteller",
                     "Erstellt", "Aktualisiert", "Projekt"]
    custom_cols   = list(CUSTOM_FIELDS.keys())
    all_cols      = standard_cols + custom_cols

    with open(filename, "w", newline="", encoding="utf-8-sig") as f:
        writer = csv.DictWriter(f, fieldnames=all_cols, extrasaction="ignore")
        writer.writeheader()

        for issue in issues:
            fields = issue.get("fields", {})
            row = {
                "Key":             issue.get("key", ""),
                "Zusammenfassung": extract_value(fields.get("summary")),
                "Status":          extract_value(fields.get("status")),
                "Vorgangstyp":     extract_value(fields.get("issuetype")),
                "Priorität":       extract_value(fields.get("priority")),
                "Zugewiesen an":   extract_value(fields.get("assignee")),
                "Ersteller":       extract_value(fields.get("reporter")),
                "Erstellt":        extract_value(fields.get("created")),
                "Aktualisiert":    extract_value(fields.get("updated")),
                "Projekt":         extract_value(fields.get("project")),
            }
            # Custom Fields hinzufügen
            for label, field_id in CUSTOM_FIELDS.items():
                row[label] = extract_value(fields.get(field_id))

            writer.writerow(row)

    print(f"\n✅ CSV gespeichert: {filename}  ({len(issues)} Issues)")


# ─────────────────────────────────────────────
# SCHRITT 5: Issues als JSON exportieren
# ─────────────────────────────────────────────
def export_to_json(issues: list[dict], filename: str = "jira_export.json"):
    with open(filename, "w", encoding="utf-8") as f:
        json.dump(issues, f, ensure_ascii=False, indent=2)
    print(f"✅ JSON gespeichert: {filename}  ({len(issues)} Issues)")


# ─────────────────────────────────────────────
# HAUPTPROGRAMM
# ─────────────────────────────────────────────
if __name__ == "__main__":

    # ── Alle Custom Fields der Instanz anzeigen ──
    print("\n📋 Custom Fields in dieser Jira-Instanz:")
    all_fields = get_all_fields()

    # ── JQL anpassen: welche Issues sollen exportiert werden? ──
    JQL = 'project = BEZ ORDER BY created DESC'
    # Weitere Beispiele:
    # JQL = 'project = BEZ AND status = "Kontakt" ORDER BY created DESC'
    # JQL = 'assignee = currentUser() ORDER BY updated DESC'
    # JQL = 'project = BEZ AND "Bezahlstatus" = "Angelegt"'

    # Felder für die API-Anfrage zusammenstellen
    fields_to_fetch = [
        "summary", "status", "issuetype", "priority",
        "assignee", "reporter", "created", "updated", "project",
    ] + list(CUSTOM_FIELDS.values())

    print(f"\n🔍 Suche Issues mit JQL: {JQL}")
    issues = search_issues(JQL, fields=fields_to_fetch, max_results=500)

    print(f"\n📦 {len(issues)} Issues gefunden.")

    # ── Exportieren ──
    export_to_csv(issues,  filename="jira_export.csv")
    export_to_json(issues, filename="jira_export.json")