# Design system — transcribed from `Abba Father.dc.html`

Source: Claude Design project `8dec4e3a-b311-47bc-af15-17488b18e52c`, file `Abba Father.dc.html`.
This document is the authority for implementation — **no future session needs to re-fetch the design.**

Direction, in the designer's words: *oat ground, sage and clay; one warm colour field per screen, never
a gradient wash; Cormorant Garamond for prayers and headings, Work Sans for anything functional;
rounded, generous, tappable; no rules, no chevron lists; the session goes to a deep forest dark so the
phone stops looking like an app.*

## Colour tokens

| Token | Hex | Used for |
|---|---|---|
| `oat` | `#F4EEE4` | app background, session text |
| `card` | `#EDE4D6` | prayer cards, search field, nav bar, ghost buttons |
| `cardPressed` | `#E6DBC9` | card hover/press |
| `clay` | `#E8DDD2` | the fourth collection tile |
| `sage` | `#4E5F48` | primary buttons, active nav pill, links, session glow |
| `sagePressed` | `#3D4C39` | primary button press |
| `sageTint` | `#DFE4D5` | home header field, tag chips, selected reader line, "Pray this" |
| `mutedSage` | `#7C8F72` | small uppercase eyebrow labels |
| `moss` | `#B9C9AC` | session progress ticks, "Breathe" line |
| `ink` | `#2B2A26` | primary text |
| `inkOnTint` | `#2B3327` | text on `sageTint` fields |
| `deepForest` | `#232A22` | prayer session background |

Opacity conventions on `ink`: `.8` body serif, `.75` secondary, `.72` meta/labels, `.7` prose,
`.6` subtitles, `.35` placeholders, `.2` sheet handle.
On `oat` inside the session: `.85` ambient button, `.6` reader byline, `.55` secondary buttons,
`.45` hint, `.38` already-read lines, `.22` unfilled ticks, `.12` translucent button fills.

## Type

Two families, bundled as OFL TTFs (not downloadable fonts, so rendering is identical offline).

- **Cormorant Garamond** (serif) — prayers, headings, anything devotional. Weights 300/400/500.
- **Work Sans** (sans) — anything functional. Weights 300/400/500/600.

| Role | Font | Size / line-height / weight | Notes |
|---|---|---|---|
| Home greeting | serif | 42 / 1.1 / 300 | letter-spacing −.01em |
| Screen title | serif | 38 / 1.1 / 300 | Library, My prayers, Saved |
| Reader title | serif | 36 / 1.12 / 300 | |
| Session line | serif | 30 / 1.42 / 300 | current line `oat`, earlier lines `oat @ .38` |
| Compose title field | serif | 32 / 1.2 / 400 | borderless |
| Suggested card title | serif | 27 / 1.2 / 400 | |
| Saved line | serif | 25 / 1.45 / 300 | |
| Sheet line | serif | 24 / 1.5 / 300 | |
| Reader line | serif | 23 / 1.6 / 300 | tappable |
| Card title | serif | 23 / 1.2 / 400 | Library + My prayers rows |
| Compose body field | serif | 21 / 1.6 / 300 | |
| Recent row title | serif | 21 / 1.25 / 400 | |
| Home verse | serif | 20 / 1.5 / 300 *italic* | |
| Collection name | serif | 19 / 1.2 / 400 | |
| Suggested excerpt | serif | 19 / 1.55 / 300 | |
| My-prayer excerpt | serif | 17 / 1.6 / 300 | |
| Primary button label | serif | 22 / 400 | "Begin prayer", tracking .04em |
| Amen button | serif | 20 / 400 | tracking .06em |
| Body sans | sans | 15 / 1.7 / 300 | subtitles, sheet buttons |
| Meta sans | sans | 12–13 / 1.4–1.7 / 300 | authors, dates, hints |
| Chip label | sans | 13 / 400 | |
| Tag chip | sans | 11 / 400 | |
| Nav label | sans | 11 / 400 | uppercase, tracking .1em |
| Section label | sans | 10 / 500 | uppercase, tracking .2em, `ink @ .72` |
| Brand eyebrow | sans | 10–11 / 500 | uppercase, tracking .22em, `sage` / `mutedSage` |

## Shape and spacing

Nothing is ruled off; separation comes from colour fields and radius.

| Element | Radius | Size |
|---|---|---|
| Pill buttons, chips, nav pills, search field | fully rounded (999) | primary 64h, sheet primary 56h, sheet secondary 54h, nav pill 54h, search 52h, compose keep 46h, scripture field 50h |
| Home header field | 0 0 40 40 | padding 44/28/36 |
| Saved card, suggested card | 30 | |
| My-prayer card | 28 | |
| Bottom sheet | 36 36 44 44 | |
| Kind tile, collection tile, seed card | 24 | min height 100–104 |
| Library / recent row | 22–24 | |
| Reader line hit area | 14 | negative 12 margin so text stays flush |
| Round buttons (add, back) | circle | 52 / 44 |
| Phone frame | 44 | 390 × 844 |

Screen padding: 44 top / 24 sides / 30 bottom for list screens; 26 all round for Reader and Compose;
34/32/30 for the session. Card gaps 10–12; section-label bottom margin 14; block gaps 26–36.

## Motion

- `fadeup` — 14px rise + fade, ~1s ease, on each newly revealed session line; .35s on the bottom sheet.
- `glow` — the session's blurred sage orb breathes between .5/1.0 and .85/1.06 scale over 9s.

## Components to build (`core/designsystem/component/`)

`SoftCard`, `PillButton` (sage / ghost-card / tinted / translucent-on-dark variants), `SectionLabel`,
`SelectableChip`, `TagChip`, `RoundIconButton`, `AbbaIcons` — arrow-right, back-chevron, home, book,
pencil, bookmark, plus, search, all transcribed from the design's inline SVG paths (24×24 viewport,
1.6–1.7 stroke, round joins).

## Screens

`Home`, `Library`, `My prayers`, `Saved`, `Reader`, `Compose`, `Session` (full-bleed dark, no nav bar),
plus the three-stage keep-a-line bottom sheet. The pill bottom bar shows only on the first four.
