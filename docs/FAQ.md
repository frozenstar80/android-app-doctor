# AppDoctor FAQ

## Does AppDoctor run in release builds?

No, not by default. `AppDoctor.install()` exits early when the host app is not debuggable.
You can override this only by setting `enabledInReleaseBuilds = true`.

## Do I need overlay permission?

No. The floating entry point uses the app window (`TYPE_APPLICATION`), not a system overlay.

## Why is the dashboard missing tabs?

Tabs are module-driven. Include the corresponding debug module and enable its config flag.

## Is any data uploaded automatically?

No. Runtime data is in-memory unless the user explicitly exports or shares artifacts.

## Is AppDoctor safe for production app binaries?

Yes, with the recommended dependency split (`core` always, heavy modules debug-only).
