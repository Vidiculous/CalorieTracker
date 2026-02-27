# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
npm run dev      # Start Vite dev server (HMR enabled)
npm run build    # Production build to /dist
npm run lint     # ESLint static analysis
npm run preview  # Preview production build locally
```

No test framework is configured.

## Architecture

**React + Vite SPA** — fully client-side calorie tracker with AI-powered food analysis. No backend; all data persists in localStorage.

### State Management

All global state lives in `src/context/AppContext.jsx` — a single React Context that manages:
- User settings (Gemini API key, calorie goal, macro targets, weight goals)
- Food logs keyed by date
- Saved recipes
- Weight history
- Chat message history

Five localStorage keys are kept in sync automatically. The context exposes 15+ methods (`addLog`, `updateLog`, `getTotalsForDate`, etc.) consumed throughout the app via `useApp()`.

### AI Integration (`src/services/ai.js`)

Wraps the Google Gemini API (`@google/generative-ai`). Three entry points:
- `analyzeText(text, context)` — text input, including URL content fetched via CORS proxy
- `analyzeImage(base64, context)` — food photo analysis
- `transcribeAudio(blob, context)` — voice input (webm)

Each call injects the user's current macro totals and conversation history for context-aware responses. The AI responds with structured JSON classified by intent: `"log"` / `"recipe"` / `"update"` / `"clarification"` / `"conversation"`. Falls back to `gemini-2.0-flash-exp` on 404 errors.

### Component Layout

`App.jsx` orchestrates top-level routing between four views (Dashboard, Analytics, RecipeList, ChatInterface) and owns the FAB (floating action bar) at the bottom. `src/layouts/Layout.jsx` wraps all views with the header.

Key components:
- **Dashboard** — day selector, calorie progress ring, macro breakdown, expandable food log
- **ChatInterface** — multi-modal AI chat (text / camera photo / voice recording via MediaRecorder API)
- **Analytics** — recharts-based charts and weight tracking
- **EditLogModal / ManualEntryModal** — edit/delete and manual-entry flows

### Food Entry Data Model

```js
{
  id: UUID,
  timestamp: ISO string,
  food_name: string,
  calories: number,
  protein: number, carbs: number, fat: number,
  quantity: string,
  source: 'ai' | 'manual' | 'recipe',
  meal_type: 'Breakfast' | 'Lunch' | 'Dinner' | 'Snacks',
  items: [/* sub-items for multi-food meals */]
}
```

### Styling

TailwindCSS v4 (PostCSS plugin). Mobile-first, dark theme (neutral-900 bg), max-width `max-w-md` centered layout, rose/orange gradient accents.
