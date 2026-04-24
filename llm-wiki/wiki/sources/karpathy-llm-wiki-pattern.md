---
title: Karpathy LLM Wiki Pattern
type: source
status: current
updated: 2026-04-24
sources:
  - https://gist.github.com/karpathy/442a6bf555914893e9891c11519de94f
---

# Karpathy LLM Wiki Pattern

## Summary

Der Gist beschreibt ein Pattern fuer persoenliche oder projektbezogene Wissensbasen: Statt Quellen nur bei jeder Frage neu per RAG zu durchsuchen, pflegt ein LLM eine dauerhafte, verlinkte Markdown-Wiki-Schicht. Diese Wiki-Schicht verdichtet Quellen, haelt Querverweise aktuell, markiert Widersprueche und macht spaetere Fragen schneller und konsistenter.

## Core Adaptation for seaFOX

seaFOX nutzt das Pattern so:

- `llm-wiki/raw/`: externe Quellen bleiben unveraendert.
- `llm-wiki/wiki/`: LLM-gepflegte Projektwissensbasis.
- `llm-wiki/wiki/index.md`: inhaltlicher Einstieg.
- `llm-wiki/wiki/log.md`: chronologisches Arbeitsprotokoll.
- `llm-wiki/AGENTS.md`: Schema fuer Ingest, Query und Lint.
- Root `AGENTS.md`: Hinweis, wann das Wiki im Projektworkflow gelesen und aktualisiert werden soll.

## Operations

- Ingest: Quelle lesen, Quellenzusammenfassung erstellen, relevante Themen-/Modulseiten aktualisieren, Index und Log pflegen.
- Query: Index zuerst lesen, relevante Seiten und Quellen pruefen, Antwort mit Pfaden geben, wiederverwendbare Synthese ins Wiki zurueckschreiben.
- Lint: Widersprueche, stale Claims, orphan pages, fehlende Querverweise und Datenluecken finden.

## Design Choices

- Kein Embedding- oder Suchserver als Startvoraussetzung.
- `rg`, Index und Log reichen fuer moderate Groesse.
- Obsidian-kompatible Wikilinks, aber normales Markdown.
- Repo-Code und offizielle Projektdateien bleiben Source of Truth.

## Related Pages

- [[index]]
- [[project-docs-ingest-2026-04-24]]
