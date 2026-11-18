# PayFlow Web

React + TypeScript frontend for the PayFlow payments API.

**Screens:** sign in / sign up, wallet dashboard with top-up, send money, transaction history with ledger entries, and an admin fraud-review queue.

## Run locally

```bash
cd frontend
cp .env.example .env        # points at http://localhost:8080 by default
npm install
npm run dev                 # http://localhost:5173
```

Start the Spring Boot API first (see the main README). Its `CORS_ALLOWED_ORIGINS` defaults to `http://localhost:5173`, so no extra setup is needed locally.

To use the admin screens, start the API with `ADMIN_EMAIL` and `ADMIN_PASSWORD` set, then sign in with that account.

## Deploy to Vercel

1. Import the GitHub repo in Vercel and set **Root Directory** to `frontend`.
2. Add the environment variable `VITE_API_URL`, e.g. `https://payflow-api-7uxe.onrender.com`.
3. Deploy. `vercel.json` sends every route to `index.html` so deep links work.
4. On Render, set `CORS_ALLOWED_ORIGINS` to your Vercel URL (e.g. `https://payflow-web.vercel.app`) and redeploy the API.

## How it talks to the API

- `src/api/client.ts` is a small `fetch` wrapper. It adds the JWT, turns error responses into `ApiError` with the server's `message`, and on a `401` swaps the refresh token for a new access token once and retries.
- If a request takes more than 4 seconds, a banner explains that the free-tier server is waking up.
- Every transfer and top-up sends an idempotency key (`crypto.randomUUID()`). The key is kept when a request fails on the network, so pressing Send again is a safe retry, and replaced as soon as the form changes or the transfer succeeds.

| File | Purpose |
|---|---|
| `src/api/types.ts` | TypeScript types matching the API's JSON |
| `src/api/endpoints.ts` | One function per endpoint |
| `src/auth/AuthContext.tsx` | Session state, login, register, logout |
| `src/pages/*` | One component per screen |
| `src/styles.css` | All styling (no CSS framework) |

Tokens are kept in `localStorage` to keep the demo simple. A production app would usually keep the refresh token in an `HttpOnly` cookie instead.
