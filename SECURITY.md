# Security Policy

## Supported Versions

We provide security updates for the latest stable release line of this Android SDK.

| Version | Supported |
| --- | --- |
| Latest stable major/minor | ✅ Yes |
| Previous stable major | ⚠️ Limited (critical fixes only, at maintainer discretion) |
| Older releases | ❌ No |
| Pre-release builds (alpha/beta/rc) | ❌ No security support guarantee |

If you are running an unsupported version, upgrade to the latest stable release before requesting security assistance.

## Reporting Vulnerabilities

Please report suspected vulnerabilities **privately** and **do not** open public issues for security reports.

<!-- Maintainers: add your private security contact channel below (for example: dedicated security email, private issue form, or advisory contact URL). -->
<!-- Example format to replace this comment:
Security contact: <add private contact method here>
-->

When reporting, include:
1. A clear description of the issue and affected SDK version(s)
2. Reproduction steps or proof-of-concept
3. Impact assessment (what an attacker can do)
4. Any known mitigations or workarounds

Encrypted reports are preferred when possible.
<!-- Maintainers: add PGP/public key details or secure submission instructions here if available. -->

## Response Timeline

We aim to follow this response process:
1. **Acknowledgement:** within 3 business days
2. **Initial triage:** within 7 business days
3. **Status update cadence:** at least every 14 days until resolution
4. **Fix and release target:** based on severity, complexity, and release risk

Critical issues are prioritized and may receive expedited handling.

## Disclosure Policy

We follow coordinated vulnerability disclosure:
1. Report is received privately and validated by maintainers
2. A fix is prepared and tested
3. A security release is published
4. Public disclosure is made after users have had a reasonable time to update

Please avoid public disclosure before a fix is available, unless legally required or there is active exploitation that justifies earlier warning.

## Security Best Practices

For maintainers and contributors:
1. Keep dependencies updated and monitor advisories
2. Require code review for security-sensitive changes
3. Avoid logging secrets, tokens, or sensitive user data
4. Validate and sanitize all external input
5. Use secure defaults and least-privilege principles

For SDK integrators:
1. Always use the latest stable SDK version
2. Store API keys and secrets in secure platform storage
3. Enforce HTTPS/TLS and certificate validation
4. Minimize sensitive data collection and retention
5. Follow Android security guidance for permissions, storage, and networking

