# FIDE Frontend Handoff

This document is a focused handoff for the frontend/UI agent to implement the new FIDE leaderboard experience that was added on the backend.

Use this file when updating the frontend so the UI can consume the new FIDE leaderboard APIs correctly.

## What Changed In Backend

The backend now supports a separate FIDE-backed leaderboard in addition to the existing local app leaderboard.

New capabilities:

- public `GET /api/leaderboard/fide`
- filter by `timeControl`
- filter by `country`
- filter by `gender`
- filter by `division`
- pagination support
- active/inactive filtering
- backend-managed import flow for FIDE list data

Important:

- this is not a replacement for the existing `/api/leaderboard`
- `/api/leaderboard` still represents the app's internal player leaderboard
- `/api/leaderboard/fide` is the new official FIDE ratings leaderboard

## Backend Contract

### Endpoint

`GET /api/leaderboard/fide`

Public endpoint.

### Query Params

- `query`
  - optional player-name search
- `timeControl`
  - optional
  - allowed values: `standard`, `rapid`, `blitz`
  - defaults to `standard`
- `country`
  - optional federation code such as `IND`, `USA`, `NOR`
- `gender`
  - optional
  - allowed values: `open`, `male`, `female`
  - defaults to `open`
- `division`
  - optional
  - allowed values: `open`, `junior`, `senior`
  - defaults to `open`
- `page`
  - optional zero-based page index
- `size`
  - optional page size
- `activeOnly`
  - optional boolean
  - defaults to `true`

### Response Shape

```json
{
  "timeControl": "rapid",
  "gender": "female",
  "division": "junior",
  "country": "IND",
  "query": null,
  "page": 0,
  "size": 50,
  "totalEntries": 1,
  "lastSyncedAt": "2026-04-10T10:00:00Z",
  "entries": [
    {
      "rank": 1,
      "fideId": 35009192,
      "name": "Divya Deshmukh",
      "title": "IM",
      "country": "IND",
      "gender": "F",
      "birthYear": 2005,
      "timeControl": "rapid",
      "rating": 2395,
      "gamesPlayed": 11,
      "inactive": false
    }
  ]
}
```

## UI Work Required

Update the leaderboard area so users can access both data sources:

1. existing app leaderboard from `/api/leaderboard`
2. new FIDE leaderboard from `/api/leaderboard/fide`

Recommended UI behavior:

- add a source toggle such as `App` and `FIDE`
- when `App` is selected, keep current leaderboard behavior
- when `FIDE` is selected, show the new filter controls
- fetch FIDE data whenever source or filters change
- persist filter state in component state and reset `page` to `0` when filters change

## Recommended FIDE Filters In UI

When FIDE mode is selected, provide:

- time control tabs or segmented control
  - `Standard`
  - `Rapid`
  - `Blitz`
- federation/country input
  - free text code input or select if country data already exists in UI
- gender filter
  - `Open`
  - `Male`
  - `Female`
- division filter
  - `Open`
  - `Junior`
  - `Senior`
- search field for player name
- active-only toggle
- pagination controls

## Suggested Table Columns For FIDE Mode

- rank
- player name
- title
- federation
- rating
- games played
- gender
- birth year
- inactive status

Notes:

- `fideId` does not need to be visually prominent, but it is useful as a stable React key
- if `gamesPlayed` is null, display `-`
- if `title` is null, display nothing or `-`
- if `lastSyncedAt` is present, show a subtle "Last updated" label near the leaderboard header

## Loading And Empty States

Implement clear UI states for FIDE mode:

- loading state while fetching
- empty state when `entries.length === 0`
- error state if request fails

Recommended empty-state copy:

- `No FIDE players matched these filters.`

Recommended error-state copy:

- `Unable to load FIDE leaderboard right now.`

## Important Product Notes

- `senior` is currently backend-defined as `50+`
- `junior` is backend-derived from birth year
- FIDE data depends on imported official rating lists, so if backend data has not been synced yet, the UI may show empty results
- do not call `/api/internal/fide/sync` from the public frontend
- the sync endpoint is backend-operational functionality, not user-facing UI behavior

## Acceptance Criteria

The frontend change is complete when:

1. users can switch between app leaderboard and FIDE leaderboard
2. FIDE leaderboard calls `/api/leaderboard/fide` with the correct query params
3. filters update the result set correctly
4. pagination works with backend `page` and `size`
5. loading, empty, and error states are handled
6. existing app leaderboard behavior is not broken

## Prompt For Frontend AI Agent

Use the prompt below directly with the frontend AI agent:

```text
Update the leaderboard UI so it supports the new FIDE leaderboard backend that now exists alongside the current app leaderboard.

Important requirements:

1. Keep the existing `/api/leaderboard` integration for the app leaderboard.
2. Add a separate FIDE mode powered by `GET /api/leaderboard/fide`.
3. In FIDE mode, support these query params:
   - query
   - timeControl = standard | rapid | blitz
   - country
   - gender = open | male | female
   - division = open | junior | senior
   - page
   - size
   - activeOnly
4. Treat the backend response as:
   - top-level metadata: timeControl, gender, division, country, query, page, size, totalEntries, lastSyncedAt
   - entries array with: rank, fideId, name, title, country, gender, birthYear, timeControl, rating, gamesPlayed, inactive
5. Add a UI source switch between the existing app leaderboard and the new FIDE leaderboard.
6. Show FIDE-specific filters only when FIDE mode is selected.
7. Reset pagination to page 0 when any FIDE filter changes.
8. Add loading, empty, and error states for the FIDE request.
9. Show `lastSyncedAt` in the FIDE leaderboard header if present.
10. Do not call `/api/internal/fide/sync` from the client.
11. Preserve the existing design system and page structure unless a small leaderboard-specific adjustment is needed.

Recommended FIDE UI:

- source toggle: App | FIDE
- time control tabs: Standard | Rapid | Blitz
- country filter
- gender filter
- division filter
- name search
- active-only toggle
- paginated table

Use `fideId` as a stable item key when rendering rows.
If `title` or `gamesPlayed` is missing, render a graceful fallback.
If the backend returns no entries, show an empty state message instead of a blank table.

The goal is to wire the existing leaderboard page to this new backend capability without breaking the current app leaderboard flow.
```
