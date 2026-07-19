# AppDoctor Extension SDK (Phase 10)

`appdoctor-extension` is the stable, public contract module for third-party AppDoctor extensions.
It contains contracts only (no runtime implementation logic).

## Module split

- **`appdoctor-extension`**: contracts (`Extension`, `ExtensionFactory`, metadata, compatibility, lifecycle, validation contracts, installer/registry contracts).
- **`appdoctor-core`**: optional runtime implementation that loads and manages extensions.
- **`appdoctor-ui`**: Extensions tab for visibility and control.

Apps that do not enable extensions behave exactly as before.

## Lifecycle

AppDoctor runs extensions through a deterministic lifecycle:

1. `install`
2. `initialize`
3. `enable`
4. `disable`
5. `unload`
6. `destroy`

Lifecycle state is exposed through `ExtensionRegistry.lifecycle(id)`.

## Capabilities

Extensions declare explicit capabilities through `ExtensionCapabilities`:

- `COLLECTORS`
- `DASHBOARD_TABS`
- `DIAGNOSTICS_RULES`
- `TIMELINE_EVENT_ENRICHERS`
- `SESSION_REPORT_ENRICHERS`
- `AI_PROMPT_ENRICHERS`
- `REMOTE_INSPECTOR_COMMANDS`
- `EXPORTERS`
- `FORMATTERS`
- `RECOMMENDATIONS`

## Compatibility and versioning

Each extension must declare:

- `ExtensionMetadata.version` (semantic version string)
- `ExtensionCompatibility.minimumSdkVersion`
- `ExtensionCompatibility.maximumSdkVersion`
- `ExtensionCompatibility.supportedCapabilities`

Core validates semantic version format and host-SDK compatibility range. Incompatible
extensions are rejected gracefully.

## Validation

`ExtensionValidator` is invoked before load and checks:

- duplicate IDs
- capability consistency/conflicts
- semantic-version/range validity
- dependency conflicts (`requiredExtensionIds`)
- configuration constraints (`allowThirdPartyExtensions`, `strictCompatibilityChecking`)

## Registration

`ExtensionConfiguration` supports:

- `enableExtensions`
- `allowThirdPartyExtensions`
- `strictCompatibilityChecking`
- `extensionLoadingStrategy`

Registration strategies:

- **ServiceLoader** via `ExtensionFactory`
- **manual registration** via `AppDoctor.registerExtension(...)`
- **dependency injection** via `dependencyInjectedFactories` / `dependencyInjectedExtensions`
- **future package manager** placeholder via `LoadingStrategy.PACKAGE_MANAGER`

## Dashboard support

The AppDoctor dashboard includes an **Extensions** tab showing:

- installed extensions
- version
- lifecycle status
- capabilities
- health
- metadata
- enable/disable controls

## Publishing model

Future extension authors should publish separate Gradle artifacts that depend on:

- `appdoctor-extension` (required contracts)
- optionally `appdoctor-core` / `appdoctor-ui` for deeper integrations

No AppDoctor core-source modifications are required for new extension projects.

## Sample extensions

The sample app includes extension implementations for:

- WorkManager
- Paging
- Coil
- Firebase Performance
- SQLDelight

They demonstrate metadata/capability declarations and lifecycle wiring through the SDK.
