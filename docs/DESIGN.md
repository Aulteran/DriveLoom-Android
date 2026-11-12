# Design: Loom HUD on Android

The app uses the server web client's design language, "Loom HUD" ([server docs/DESIGN.md](https://github.com/Aulteran/DriveLoom/blob/dev/docs/DESIGN.md)), adapted to Jetpack Compose and Material 3: a calm instrument panel with glass surfaces, a cyan to violet signal gradient and monospaced readouts. Dark is the default; a light theme and "follow the system" are under Settings, Appearance.

## Principles

- **Data first.** Numbers are the product. Readouts use JetBrains Mono, and the headline economy figure is the largest, brightest thing on a vehicle's screen.
- **Quiet ambience, loud signal.** The backdrop (two aurora glows over a fading grid) and the glass panels stay low contrast. The gradient is reserved for the one primary action on a screen, the active dock item, panel markers and the chart.
- **Few, large things.** One column, generous spacing, 48 dp minimum touch targets, a three-item bottom dock (Garage, Friends, Settings) and list rows with chevrons for everything deeper.
- **Private by construction.** Fonts are bundled. Nothing is fetched from anywhere but the configured server.

## Where it lives

| File | Contents |
| --- | --- |
| `ui/theme/Theme.kt` | Colour tokens for both themes (the same values as the web client), type styles, and the mapping onto a Material 3 colour scheme so dialogs and pickers match. |
| `ui/theme/Icons.kt` | The DriveLoom stroke icons, the same drawings as the web client's `icons.js`, plus a few added for the app. |
| `ui/components/Components.kt` | Backdrop, panels, buttons, fields, segmented control, stat tiles, list rows, notices, dialogs. |
| `ui/components/EconomyChart.kt` | The chart: gradient line drawn in on arrival over a soft area, hollow points with the latest one filled. |

## Tokens

Colours are the web client's, for example background `#05070E` / `#EDF1F8`, text `#E7EEF8` / `#0B1220`, accent `#22D3EE` → `#A78BFA` (dark) and `#0E7490` → `#6D28D9` (light). Radii are 16 dp for panels, 10 dp for controls and 8 dp for chips.

| Role | Face |
| --- | --- |
| Headings, buttons, navigation | Space Grotesk 400 to 700 |
| Body text | The system font |
| Numbers, micro-labels, codes | JetBrains Mono 400, 500, 700 |

The font files in `app/src/main/res/font/` are static instances made from the variable fonts the web client ships (latin and latin-ext merged), under the SIL OFL 1.1 in `licenses/`.

## Motion

A screen rises slightly as it arrives and the chart draws in once. Colours never animate, so text is never read mid-fade and a theme switch is instant.

## Accessibility

Text sizes are in `sp` and follow the system font scale. Icons are decorative and sit beside a visible label; icon-only buttons carry a description. The chart has a text description of its range. Status is never shown by colour alone.

## Not yet verified

The interface has been type-checked and built but not yet looked at on a device. Contrast, spacing at large font scales, small screens and TalkBack order still need a visual pass.
