# Widen QQ Music Player Implementation Plan

> **For agentic workers:** REQUIRED: Use $subagent-driven-development (if subagents available) or $executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Increase the desktop floating music-player card from 292px to 320px while keeping exactly 16px visible when collapsed.

**Architecture:** Keep the existing component and interaction behavior unchanged. Define the panel and edge widths as CSS custom properties on `.music-player`, then derive the collapsed translation from those values so future width changes stay aligned.

**Tech Stack:** Nuxt 4, Vue 3, CSS custom properties

---

## Chunk 1: Player width adjustment

### Task 1: Widen the desktop player card

**Files:**
- Modify: `frontend/web/app/assets/css/main.css:4199`

- [ ] **Step 1: Replace hard-coded width values with shared custom properties**

Update `.music-player` to use:

```css
.music-player {
  --music-player-width: 320px;
  --music-player-edge-width: 16px;
  box-sizing: border-box;
  width: var(--music-player-width);
  transform: translate(calc(var(--music-player-edge-width) - var(--music-player-width)), -50%);
}
```

Keep the existing positioning, z-index, and transition declarations unchanged.

- [ ] **Step 2: Reuse the edge-width property**

Set `.music-player__edge` width to `var(--music-player-edge-width)` and set the expanded edge offset to `calc(0px - var(--music-player-edge-width))`. This preserves the current 16px edge behavior without duplicating the value.

- [ ] **Step 3: Check the CSS diff**

Run:

```powershell
git diff --check
git diff -- frontend/web/app/assets/css/main.css
```

Expected: no whitespace errors; the diff changes only the player width variables, derived collapsed offset, edge width, and expanded edge offset.

- [ ] **Step 4: Hand off visual verification**

Keep the existing Nuxt development server running at `http://localhost:3001/`. The user will verify that the expanded card is 320px wide and that the collapsed state exposes exactly 16px on desktop.

- [ ] **Step 5: Commit**

```powershell
git add frontend/web/app/assets/css/main.css
git commit -m "style: widen music player card"
```
