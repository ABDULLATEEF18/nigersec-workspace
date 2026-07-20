# NigerSec Frontend

React 19 SPA — the browser interface for the NigerSec platform.

See the [root README](../README.md) for full setup and API documentation.

## Pages

| Route | Description |
|---|---|
| `/` | Public landing page — breach checker, fraud score demo, recent breach table |
| `/citizen` | Citizen dashboard — monitoring, alerts, check history (login required) |
| `/institution` | Institution portal — threat feed, compliance reports, fraud API (login required) |

## Local development

```bash
npm install
npm run dev      # http://localhost:5173
```

Vite proxies all `/api/*` calls to `http://localhost:8080` automatically.

## Environment variables

Create a `.env` file in this directory:

```env
# Leave blank to use the Vite dev proxy (recommended for local dev)
VITE_API_URL=

# Optional — enables the AI breach advisor and threat explainer
# Free key at https://aistudio.google.com/app/apikey
VITE_GEMINI_API_KEY=
```

## Build for production

```bash
npm run build    # output → dist/
npm run preview  # preview the production build locally
```
