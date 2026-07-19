# AppDoctor Extensions

`appdoctor-extension` provides stable contracts for third-party extension integration.

## Scope

- Contract-only module (no runtime implementation)
- Extension lifecycle and metadata contracts
- Compatibility and capability declarations
- Validator/registry interfaces

## Lifecycle

`install -> initialize -> enable -> disable -> unload -> destroy`

## Loading model

- ServiceLoader factory discovery
- Manual registration
- Dependency-injected registration

## Safety model

- Compatibility checks
- Capability validation
- Deterministic lifecycle transitions
- Optional strict compatibility enforcement
