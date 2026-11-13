# Flame in Freefall

**NASA Space Apps Challenge 2026 — "Flame in Freefall" challenge**

An AI-assisted dashboard that turns NASA microgravity combustion research into fire-safety insight for spacecraft, space stations, lunar bases and pressurized rovers.

- **Live web app:** https://flame.mgacreative.com
- **Team:** Aren Azat · Arda Özkan

---

## Why it matters

Fire behaves differently without gravity. With no buoyancy, a flame becomes a dim, spherical flame fed by diffusion, so fire-safety knowledge from Earth does not transfer directly. Future lunar habitats are also expected to run at higher oxygen levels (≥30% O₂ at ≤10.2 psi), where materials burn more readily. NASA has decades of combustion experiments, but the data is spread across many studies and files. Flame in Freefall brings it together and makes it searchable, comparable and explainable.

## Features

| Section | What it does |
|---|---|
| **PSI Experiments** | Catalog of the NASA PSI combustion investigations. Filter by fire-safety role (prevention, detection, suppression, fundamental, modeling), fuel state, hardware, flight vs. ground, and O₂ / pressure range. Select up to 3 studies to compare. |
| **Experiment detail** | Objective, approach, hypothesis, test conditions, sensors, related studies, publications with DOI, and the full test matrix with CSV / JSON export for modeling. |
| **Compare** | Side-by-side view with shared (✓) and different (≠) attributes highlighted automatically. |
| **Habitat Panel** | Pick a habitat preset or set O₂ and pressure. Experiments are ranked by habitat relevance, coverage of the target atmosphere and safety role, with an O₂–pressure map of every study's test range. |
| **History & Earth** | Space fire incidents and PSI experiments on one timeline, plus applications of this research on Earth. |
| **AI Assistant** | Retrieval-augmented answers grounded in NASA NTRS technical reports. Every answer cites the source record; if nothing relevant is found, it says "insufficient data" instead of guessing. Answers in the language of the question. |

Scope is stated openly in the app: PSI does not yet contain partial-gravity (Moon / Mars) combustion data, and rocket propulsion fires are out of scope.

## Space agency data

- **NASA Physical Sciences Informatics (PSI)** — combustion investigations, their Experimental Table CSVs and linked publications. https://psi.nasa.gov
- **NASA Technical Reports Server (NTRS)** — microgravity combustion technical reports used as the AI assistant's knowledge base. https://ntrs.nasa.gov

PSI data is cited by DOI on each experiment page, as required by NASA SMD Open Source Science.

## How it works

```
NASA PSI + NTRS ──► Supabase (PostgreSQL + pgvector, RLS, RPC, Edge Functions) ──► KMP shared module ──► Web · Android · Desktop
```

1. **Ingestion** (`/ingestion`, Kotlin CLI): fetches NTRS records, extracts PDF text, splits it into chunks and creates 384-dimension embeddings (Supabase `gte-small`) stored in pgvector.
2. **PSI data** (`/supabase/migrations`): investigation metadata, test conditions, publications, habitat profiles and the `rank_investigations_for_habitat` ranking function.
3. **AI assistant** (`/supabase/functions/ask`): the question is embedded, matched against report chunks with cosine similarity, and Google Gemini writes an answer using only the retrieved text. API keys stay server-side.
4. **App** (`/shared`): one Kotlin Multiplatform codebase with Compose Multiplatform screens, shared by the web (Wasm), Android and desktop apps.

## Tech stack

Kotlin Multiplatform · Compose Multiplatform (Web/Wasm, Android, Desktop) · Koin · Ktor · supabase-kt · Supabase (PostgreSQL, pgvector, Row Level Security, Edge Functions on Deno) · Supabase gte-small embeddings · Google Gemini API · Cloudflare Pages

## Project structure

```
shared/        Shared models, repositories, use cases, view models and all UI screens
webApp/        Web (Wasm) entry point
androidApp/    Android entry point
desktopApp/    Desktop (Windows / macOS / Linux) entry point
ingestion/     Kotlin CLI for NTRS fetch, text extraction, embeddings and summaries
supabase/      Database migrations, seed data and Edge Functions
data/          Raw NASA PSI data used to build the seed
```

## Running locally

Requirements: JDK 21, a Supabase project.

1. Create `local.properties` in the project root (it is git-ignored):
   ```
   SUPABASE_URL=https://<your-project>.supabase.co
   SUPABASE_ANON_KEY=<anon key>
   ```
2. Apply the SQL files in `supabase/migrations` to your database.
3. Deploy the Edge Functions and set the `GEMINI_API_KEY` secret:
   ```
   npx supabase functions deploy --no-verify-jwt
   ```
4. Run a target:
   ```
   ./gradlew :webApp:wasmJsBrowserDevelopmentRun     # Web
   ./gradlew :desktopApp:run                          # Desktop
   ./gradlew :androidApp:installDebug                 # Android
   ```

## Use of AI

AI coding assistants were used to help write and debug code and documentation. Inside the app, Google Gemini generates answers only from retrieved NASA text, and Supabase gte-small creates the search embeddings. No AI-generated data is presented as NASA data.

## Data credit

NASA data remains subject to NASA's data use and citation policies. Please cite the PSI DOI of any experiment you use.
