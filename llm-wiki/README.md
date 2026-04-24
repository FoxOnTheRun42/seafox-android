# seaFOX LLM Wiki

Dieses Verzeichnis macht das LLM-Wiki-Pattern fuer seaFOX direkt nutzbar. Die Idee: Quellen werden gesammelt, das LLM pflegt daraus eine dauerhafte Markdown-Wissensbasis, und jede neue Erkenntnis kann in diese Wissensbasis zurueckfliessen.

## Struktur

- `raw/` - unveraenderte externe Quellen, Clips, PDFs, Meetingnotizen und Assets.
- `wiki/` - die gepflegte Wissensbasis.
- `wiki/index.md` - Einstieg und Inhaltskatalog.
- `wiki/log.md` - chronologisches Aenderungs- und Ingest-Protokoll.
- `AGENTS.md` - Arbeitsregeln fuer Codex/Claude/andere LLM-Agenten.

Interne Projektdateien wie `../README.md`, `../AGENTS.md` oder `../docs/PRODUCTION_READINESS.md` muessen nicht in `raw/` kopiert werden. Sie werden als Quellen direkt verlinkt. Externe Quellen sollten dagegen unter `raw/` abgelegt werden, bevor daraus Wiki-Seiten entstehen.

## Schnellstart fuer eine Codex-Session

1. Lies `llm-wiki/wiki/index.md`.
2. Lies die fuer die Aufgabe relevanten Seiten.
3. Pruefe bei wichtigen Aussagen die verlinkten Quellen oder den Code.
4. Wenn neues dauerhaftes Wissen entsteht, aktualisiere die passende Wiki-Seite, den Index und `wiki/log.md`.

## Nuetzliche Kommandos

```bash
rg -n "Suchbegriff" llm-wiki/wiki
rg "^## \\[" llm-wiki/wiki/log.md | tail -10
find llm-wiki/wiki -name "*.md" | sort
```

Optional kann Obsidian direkt auf dieses Projektverzeichnis oder auf `llm-wiki/` zeigen. Die Seiten verwenden Wikilinks, bleiben aber normales Markdown.
