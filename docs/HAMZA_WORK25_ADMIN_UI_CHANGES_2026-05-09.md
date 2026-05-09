# Hamza Work 25 - Admin UI Changes

Date: 2026-05-09
Branch: `hamza-work25`

## Purpose

This note explains the admin/backoffice UI changes so the team can review and merge the work safely.

## Main Changes

### Backoffice layout

Updated the admin layout in:

- `FrontEnd/src/app/layouts/backoffice-layout/backoffice-layout.html`
- `FrontEnd/src/app/layouts/backoffice-layout/backoffice-layout.ts`
- `FrontEnd/src/app/layouts/backoffice-layout/backoffice-layout.scss`
- `FrontEnd/src/styles.scss`

Changes include:

- Cleaned the top bar.
- Moved page search/filter/sort controls into the top bar dynamically.
- Added one compact `Filters & Sort` dropdown when a page has multiple filters/sort controls.
- Kept pages without search or sort clean, with no empty controls.
- Added dynamic page statistics handling.
- Statistics are hidden from the page by default, except on the dashboard.
- Pages with statistics now show a `Statistics` button in a rectangle area under the top bar.
- Clicking `Statistics` opens a statistics panel with the original stat cards.

### Sidebar

Updated the sidebar navigation:

- Removed `Audit Logs` from the sidebar.
- Removed `Clinical Audit Logs` from the sidebar.
- Removed `Pharmacy` from the admin sidebar.
- Fixed active navigation/dropdown behavior so sidebar buttons do not stay stuck after many clicks.

### Logs center

Added a centralized logs page:

- `FrontEnd/src/app/pages/backoffice/logs-center/logs-center.ts`
- `FrontEnd/src/app/pages/backoffice/logs-center/logs-center.html`
- `FrontEnd/src/app/pages/backoffice/logs-center/logs-center.scss`

Route added in:

- `FrontEnd/src/app/app.routes.ts`

New route:

```text
/backoffice/logs-center
```

The page lets admin switch between:

- Audit Logs
- Clinical Audit Logs

The profile dropdown now has a `Logs` button that opens this page.

### Contracts pages

Updated contract filters:

- On HR contracts page, removed the role filter because it only shows HR contracts.
- On staff contracts page, removed `HR` from the role filter because HR has its own contracts page.
- Kept the existing backend behavior unchanged.

Relevant file:

- `FrontEnd/src/app/pages/backoffice/contracts-list/contracts-list.html`

## Important Behavior Notes

- Search and sort controls were moved visually, not rewritten for backend logic.
- The existing form bindings and filter logic should still work.
- Statistics cards are moved into a panel by layout code, so the original page components remain mostly unchanged.
- Dashboard statistics are not hidden.

## Build Check

Docker frontend build was run successfully:

```powershell
docker compose -f docker-compose.full.yml build frontend
```

The build completed successfully. Existing Angular warnings remain, mostly budget warnings and optional-chain template warnings, but no fatal errors.

## How To Run From Docker

```powershell
docker compose -f docker-compose.full.yml up -d frontend
```

Frontend should be available on:

```text
http://localhost:4200
```

## Branch And Push Commands

Create the branch:

```powershell
git switch -c hamza-work25
```

Commit and push:

```powershell
git add .
git commit -m "Update admin backoffice UI"
git push -u origin hamza-work25
```

## Merge Review Checklist

- Check admin dashboard still displays its statistics normally.
- Check pages with filters show them in the top bar.
- Check pages without filters do not show empty controls.
- Check `Statistics` button opens the panel instantly.
- Check sidebar navigation does not get stuck after many clicks.
- Check profile dropdown `Logs` opens the logs center.
- Check HR contracts page does not show role filter.
- Check staff contracts page does not include HR role filter.
