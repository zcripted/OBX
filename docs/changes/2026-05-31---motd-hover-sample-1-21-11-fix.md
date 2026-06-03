# MOTD Player-Count Hover Fix (Paper 1.21.11 / modern CraftPlayerProfile)

■ **Created:** 2026-05-31 7:05 pm (America/Detroit)

■ **Last Updated:** 2026-05-31 8:14 pm (America/Detroit)

---

## Symptom

On Paper 1.21.11 the server-list player-count hover never appeared, and the
console logged:

```
[MOTD] ping handled: event=StandardPaperServerListPingEventImpl, hoverLines=4,
sample=fail:no-candidate-accepted [
  PlayerProfile:no-build(GAME_PROFILE_WRAP+ctor+wrapCtor;
    first-fail=NoSuchFieldException:CraftPlayerProfile profile field of type com.mojang.authlib.GameProfile)
  PlayerProfile:no-build(...same...)
  GameProfile:cce
  ListedPlayerInfo:cce
] (no custom hover shown for this ping).
```

## Root Cause

The player-sample list on modern Paper is `List<PlayerProfile>` (element-type
checked), so it rejects a raw `GameProfile` (`GameProfile:cce`) and a
`ListedPlayerInfo` (`ListedPlayerInfo:cce`). The only viable element is the
platform's `CraftPlayerProfile`.

The previous builder used a single fixed `GAME_PROFILE_WRAP` strategy: build a
stub `GameProfile`, wrap it in `CraftPlayerProfile`, then **swap the wrapper's
backing `GameProfile` field** to a coloured one. On **modern Paper (1.20.5+ /
1.21.x) `CraftPlayerProfile` no longer keeps a `GameProfile` field** — it stores
the name as a plain `String` field and builds the `GameProfile` on demand. So the
field swap threw `NoSuchFieldException` and the whole hover was abandoned. The
code also committed to that one strategy and never fell back to the
`createProfile*` factory path.

## Fix

Rewrote the `PlayerProfile` builder in `MotdPingListener` to carry **both** the
factory-method path and the GameProfile-wrap handles, and to try them in order at
runtime until one yields a profile whose `getName()` actually returns the
coloured value (`buildPlayerProfile`):

1. **Factory method** — `createProfileExact` (preferred; skips name validation),
   then `createProfile` / `createPlayerProfile`. Clean path, no field surgery.
2. **Wrap a coloured `GameProfile`** directly via `CraftPlayerProfile(GameProfile)`
   / `asBukkit*`. `new GameProfile(uuid, value)` accepts arbitrary names (proven
   by the old `GameProfile:cce` path, which built it cleanly). If the wrapper
   copies the name verbatim, done.
3. **Stub + overwrite** — if the wrapper ctor sanitises/validates the name,
   rebuild it with a clean stub name, then overwrite the wrapper's own name via
   `setProfileName`.

`setProfileName` now handles **both** wrapper shapes:
- **Older Paper:** replace the backing `com.mojang.authlib.GameProfile` field
  with a coloured `GameProfile`.
- **Modern Paper (the 1.21.11 case):** the name lives in a plain `String` field.
  We locate it by matching its **current value** (the stub the wrapper was just
  built with) — which survives field renames and obfuscation across versions —
  then overwrite it. A field literally named `name` is the last-resort fallback.

Writes go through `writeField` (reflection, with a `sun.misc.Unsafe` fallback for
`final` fields). Each attempt is verified with `nameEquals` so a sanitised or
rejected name is never silently shipped. When the sample serialises, Paper's
`buildGameProfile()` does `new GameProfile(uuid, colouredName)`, which we've shown
works — so the coloured hover reaches the client.

Removed the brittle `injectWrapperProfile` (GameProfile-field-only swap) and the
single-strategy `GAME_PROFILE_WRAP` build path.

Colours: hover/sample lines already pass through `MotdService.colorize()`
(`translateAlternateColorCodes('&', translateHex(...))`), so the now-building
sample renders with its configured colours.

### Files
- `src/main/java/dev/zcripted/obx/listener/server/MotdPingListener.java`
  - `ProfileFactory.resolve()` — PlayerProfile branch now carries factory source
    **and** wrap handles together.
  - New `buildPlayerProfile`, `setProfileName`, `writeField`, `nameEquals`,
    `wrapImpl`, `recordFailure`; removed `injectWrapperProfile` and the
    `GAME_PROFILE_WRAP` build case.

---

## Testing
- In-project Maven build completes with **no errors** (`./maven/bin/mvn -DskipTests package`); both jars produced.
- Verified the rewritten builder has no dangling references and the candidate /
  fallback chain compiles.

> Runtime verification (hovering the player count in the 1.21.11 client) requires
> a live server; the console line should now read `sample=ok:... type=PlayerProfile`
> instead of `fail:no-candidate-accepted`.

---

## Follow-up — `sample=immutable` (second iteration)

After the build fix above, profiles built fine but applying them failed:

```
sample=fail:no-candidate-accepted [PlayerProfile:immutable PlayerProfile:immutable GameProfile:cce ListedPlayerInfo:cce]
```

The `GameProfile:cce` / `ListedPlayerInfo:cce` confirm the sample list only
accepts `PlayerProfile` (which we now build). `PlayerProfile:immutable` means
modern Paper's `getPlayerSample()` returns an **unmodifiable** list and exposes
no setter, so `clear()` / `addAll()` on it throws `UnsupportedOperationException`.

**Fix:** `EventReflection.trySetSample` now, on `immutable`, overwrites the
event's backing `List` field with a fresh **mutable** `ArrayList` of our profiles
(`overwriteSampleField` → `overwriteListFields`):

1. **Identity pass** — write the `List` field whose current value `==` the object
   the getter returned (the getter returns the backing list directly, just
   wrapped unmodifiable).
2. **Verify pass** — if no field is identity-equal (the getter returns a fresh
   `unmodifiableList(field)` copy each call), write each `List` field in turn and
   keep the write only when the getter afterwards reports our profile count
   (`sampleReflects`); otherwise the field is restored so no unrelated list is
   clobbered.

Writes use reflection with a `sun.misc.Unsafe` fallback for `final` fields
(`writeListField` → reuses `ProfileFactory.UnsafeFieldWriter`). A plain
`ArrayList` performs no element-type check, sidestepping both the immutability
and the element-cast failures of the API-returned list.

- `src/main/java/dev/zcripted/obx/listener/server/MotdPingListener.java`
  — new `overwriteSampleField`, `overwriteListFields`, `sampleReflects`,
  `writeListField`; `trySetSample` immutable branch now calls them.

Rebuilt clean (exit 0, both jars). The console line should now read
`sample=ok:field-replace x4 type=PlayerProfile`.

---

## Follow-up 2 — hover still not shown despite `sample=ok:field-replace x4`

The previous iteration logged success and `getPlayerSample()` reflected our 4
profiles, yet the client still showed no hover. Verified the sample names are
valid: `MotdMessageUtil.formatText` converts `<gradient>` / `<#hex>` / `&` to
legacy `§` codes (BungeeCord `ChatColor`), so the content is fine, and the
vanilla client builds the hover from a non-empty `players.sample()` regardless of
the online count — so neither content nor a 0 count is the cause.

Root cause: on modern Paper the list the server actually **serialises** onto the
status packet is a **different internal field** than the one `getPlayerSample()`
exposes (the getter returns an unmodifiable view of one field; the wire is built
from another, frequently a `List<GameProfile>`). `ok:field-replace` only proved
the getter-mirrored field was written — not the serialised one. The earlier
`GameProfile:cce` confirms the getter's list is specifically `List<PlayerProfile>`,
i.e. distinct from any `GameProfile` list.

**Fix:** `applyHoverSample` now runs two phases, **both** every ping —
1. the existing official-API path, kept as-is (`trySetSample`: setter → mutate →
   field-replace the getter's list), then
2. **comprehensive field injection** (`injectProfileListFields`): fill *every*
   profile-typed `List` field on the event (and superclasses) with a mutable
   list, building each field's entries with the factory for **its own** generic
   element type — `List<GameProfile>` → raw `GameProfile`s (coloured names;
   authlib doesn't validate), `List<PlayerProfile>` → `CraftPlayerProfile`s,
   `List<ListedPlayerInfo>` → `ListedPlayerInfo`s. Whichever field Paper
   serialises is therefore populated.

Writes reuse the existing `writeListField` (reflection + `sun.misc.Unsafe`
fallback for `final`, via `ProfileFactory.UnsafeFieldWriter`). The diagnostic now
reports the end-of-handling truth: `sample=ok:api=… fields=N getterNow=M`, so any
future failure is unambiguous (`fields=0` ⇒ no profile-typed field found/written).

Added `injectProfileListFields`, `isProfileType`, `currentSampleSize` (additive —
the previous `trySetSample` / `overwriteSampleField` / `overwriteListFields` /
`sampleReflects` / `writeListField` helpers are retained and still used by the
phase-1 API path). Rebuilt clean (exit 0, both jars).

- `src/main/java/dev/zcripted/obx/listener/server/MotdPingListener.java`

---

## Suggested Commit Message
```
Fix (MOTD): Player-count hover on Paper 1.21.11 — inject coloured sample into every profile-typed list field (the serialised field differs from getPlayerSample's view); add end-of-handling readback
```
