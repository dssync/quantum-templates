# Quantum 3 File Templates — Rider Plugin

Adds a **Quantum Script** entry to the top of Rider's **New** menu (right-click a folder → New / Add).
It opens a dialog where you enter a name and pick one of these file types:

| Kind | Creates | Base type |
|---|---|---|
| Component (.qtn) | `Name.qtn` | `component Name { }` |
| System (Filter) | `Name.cs` | `SystemMainThreadFilter<Name.Filter>` |
| System (Signals Only) | `Name.cs` | `SystemSignalsOnly` |
| System | `Name.cs` | `SystemMainThread` |
| Command | `Name.cs` | `DeterministicCommand` |
| Asset Script | `Name.cs` | `AssetObject` |
| Entity View Context | `Name.cs` | `QuantumMonoBehaviour, IQuantumViewContext` |
| Entity View Component | `Name.cs` | `QuantumEntityViewComponent` |
| Entity View Component With Context | `Name.cs` | `QuantumEntityViewComponent<IQuantumViewContext>` |
| Input Script | `Name.cs` | `MonoBehaviour` polling `CallbackPollInput` |

### Namespaces

- Simulation files (systems, commands, assets) always use `namespace Quantum`, as Quantum 3 requires.
- View and input scripts get a namespace built from the folder path below the nearest `Assets`, `Scripts`,
  `Simulation`, `QuantumUser` or `src` folder (or the project root). For example `Assets/Game/View/` gives `Game.View`.
  Folder names are made into valid identifiers (`My-Views` → `My_Views`, `2D` → `_2D`). With no folders in between,
  the namespace is `Quantum`.

### Brace style

New files use K&R braces (`class Foo {`) by default. Switch to Allman (brace on its own line) under
**Settings → Tools → Quantum**. This applies to `.qtn` components too.

## Go to QTN Declaration

**Ctrl+Alt+Q** (also in the editor right-click menu and **Navigate** menu) jumps from the identifier under the
caret to the `.qtn` line that declared it.

Matching is by name, so it works from any file. These generated forms map back to their QTN name:

| Under the caret | Finds |
|---|---|
| `Foo` | `component`/`struct`/`enum`/`flags`/`union`/`asset`/`event`/`signal`/`bitset[N] Foo` |
| `QPrototypeFoo`, `FooPrototype` | `component Foo` |
| `ISignalOnFoo` | `signal OnFoo` |
| `EventFoo` | `event Foo` |
| `Input` | `input` |

Rebind it under **Settings → Keymap → Plug-ins → Quantum 3 File Templates**. A mouse shortcut also works
(e.g. Alt+Shift+Click), in which case it uses the word under the pointer instead of the caret.

### Ctrl+Click

Ctrl+Click (and Ctrl+B) on a C# symbol that has a QTN declaration can offer the QTN too. Choose the behaviour
under **Settings → Tools → Quantum**:

- **Ask** (default): a popup offers `QTN File (Foo.qtn)` or `Generated C# Declaration`. Picking the C# hands over to Rider's
  normal Go to Declaration, so partial classes still get Rider's own chooser.
- **Go to the QTN declaration**: skips the popup.
- **Go to the generated C#**: Rider's default behaviour, untouched.

Symbols with no QTN declaration always go straight to Rider's Go to Declaration.

How it works: Rider sends Go to Declaration to its ReSharper backend, so a frontend plugin can't add entries to
Rider's own chooser. Instead the plugin wraps the `GotoDeclaration` action at startup (`QtnCtrlClick.kt`) and
puts the original back if the plugin is unloaded.

---

## Project layout

```
build.gradle.kts            Gradle build (IntelliJ Platform Gradle Plugin 2.x)
gradle.properties           Plugin version, supported Rider builds, SDK location
src/main/kotlin/...         NewQuantumFileAction.kt (new-file dialog), QtnLookup.kt + GoToQtnDeclarationAction.kt
                            (Ctrl+Alt+Q), QtnCtrlClick.kt (Ctrl+Click wrapper), BraceStyle.kt (K&R -> Allman),
                            QuantumSettings.kt (settings + Tools > Quantum page), Icons.kt
src/main/resources/
  META-INF/plugin.xml       Plugin descriptor: actions, Ctrl+Click startup hook, settings page
  fileTemplates/internal/   One .ft template per file kind (<TemplateName>.<ext>.ft)
  icons/quantum.png         16x16 menu icon
```

---

## Building

### Prerequisites

- **JDK 25**. Rider's bundled runtime works:
  ```
  set JAVA_HOME=C:\Program Files\JetBrains\JetBrains Rider 2026.2.0.2\jbr
  ```
  (PowerShell: `$env:JAVA_HOME = "C:\Program Files\JetBrains\JetBrains Rider 2026.2.0.2\jbr"`)
- Internet access on the first build (Gradle 9.8.2, Kotlin and the IntelliJ Platform plugin are downloaded).

### Rider SDK

The plugin compiles against a Rider SDK, configured in `gradle.properties`:

- `platformLocalPath`: a local Rider install. If it exists it is used directly, so nothing big is downloaded.
- `platformVersion`: used instead when `platformLocalPath` is missing (downloads the SDK, several GB, cached afterwards).

### Commands

```
./gradlew buildPlugin     # builds build/distributions/QuantumTemplates-<version>.zip
./gradlew verifyPlugin    # runs JetBrains' Plugin Verifier against the configured Rider
./gradlew runIde          # launches a sandboxed Rider with the plugin installed
```

### Working on it in Rider

Rider has no Gradle/Kotlin support, so open this folder in Rider (**File → Open → folder**) and edit the
`.kt`, `.xml` and `.ft` files as plain text. Then build from Rider's terminal (PowerShell):

```
$env:JAVA_HOME = "C:\Program Files\JetBrains\JetBrains Rider 2026.2.0.2\jbr"
./gradlew buildPlugin
```

To skip the first line, set `JAVA_HOME` permanently as a Windows user environment variable and restart Rider.
For Kotlin code completion and error highlighting, use IntelliJ IDEA (optional).

---

## Installing

1. In Rider: **Settings → Plugins → ⚙️ → Install Plugin from Disk…**
2. Select the `.zip` from `build/distributions/`.
3. Restart Rider if prompted.

---

## Customising templates

Edit the files in `src/main/resources/fileTemplates/internal/`, then rebuild. They use Apache Velocity syntax:

- `${NAME}`: the name entered in the dialog.
- `${NAMESPACE}`: the namespace inferred from the folder (see above).

Write templates in K&R style with 4-space indents. The Allman option converts them as the file is created
(`BraceStyle.kt`) by moving any brace that ends a code line onto its own line, and dropping the blank line K&R
templates leave after a class's opening brace. Avoid lines ending in `{` that aren't block openers.

To add a new kind, add `<TemplateName>.<ext>.ft` and a matching `.addKind("Label", icon, "<TemplateName>")`
line in `NewQuantumFileAction.buildDialog`.

---

## Supported Rider versions

`pluginSinceBuild` in `gradle.properties` sets the oldest supported build (currently `262` = Rider 2026.2).
There is no upper limit, so new Rider releases load the plugin without a rebuild.

The build targets the Java and Kotlin versions of the SDK it compiles against. To support an older Rider, lower
`pluginSinceBuild` **and** set `platformVersion` to that release (and clear `platformLocalPath`), then rebuild.
