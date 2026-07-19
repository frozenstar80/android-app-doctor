# Contributing

## Development setup

```bash
./gradlew test
./gradlew lint
./gradlew :sample-app:assembleDebug
```

## Contribution expectations

- Preserve module boundaries (`core` remains UI-free).
- Keep public API changes explicit and documented.
- Prefer additive, backward-compatible evolution for `v1.x`.
- Include/update KDoc for public surfaces changed by your PR.

## Policies

- Code of Conduct: `CODE_OF_CONDUCT.md`
- Security policy: `SECURITY.md`
- License: `LICENSE`
