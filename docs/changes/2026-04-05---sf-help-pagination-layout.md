■ **Created:** 2026-04-05 12:46 PM
■ **Last Updated:** 2026-04-05 12:46 PM

## Commands
- Updated `/obx help` pagination so the previous/next navigation buttons render side by side on a single chat line.
- Adjusted the visible navigation labels to use `[\u25C4 Previous Page]` and `[Next Page \u25BA]` formatting, with matching German translations.

## Internal
- Added joined interactive chat-part sending so multiple clickable hoverable help navigation buttons can be delivered in one combined message line while preserving each button's own action.

## Files
- `src/main/java/dev/zcripted/obx/command/core/ObxCommand.java`
- `src/main/java/dev/zcripted/obx/language/MessageDefaults.java`
- `src/main/java/dev/zcripted/obx/util/text/ComponentMessenger.java`
- `src/main/resources/languages/language_en.yml`
- `src/main/resources/languages/sprache_de.yml`
- `docs/changes/2026-04-05---sf-help-pagination-layout.md`

Suggested Commit Message
```
Fix(help): align sf help pagination buttons on one line
```
