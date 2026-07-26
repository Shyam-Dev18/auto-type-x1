# Security Policy — AutoType X1

## Supported Versions

| Version | Supported |
|---|---|
| 1.0.x (current) | ✅ |

Only the latest release receives security fixes.

---

## Reporting a Vulnerability

**Please do not report security vulnerabilities in public GitHub Issues.**

AutoType X1 handles Bluetooth communication and processes user-supplied scripts that are transmitted as keystrokes. A vulnerability in this area could potentially affect any device the app is connected to.

### How to Report

Use **GitHub's private vulnerability reporting** feature (Security → Report a vulnerability) on the public repository. This allows confidential disclosure before a fix is available.

After the repository is published, private reporting can be enabled at:  
`https://github.com/<owner>/<repo>/security/advisories/new`

> **Note:** Private security reporting must be enabled by the repository owner after GitHub publication.

### What to Include in a Report

A useful vulnerability report should include:

- **Description** of the vulnerability and the potential impact
- **AutoType X1 version** affected
- **Android version** and device model where observed
- **Reproduction steps** (minimal, clear, step-by-step)
- **Expected behavior** vs. **actual behavior**
- Any **proof-of-concept** or supporting code (if safe to share)
- Whether you believe it is **exploitable in practice**

### What to Expect

- Acknowledgement of your report within **7 days**
- Assessment of severity and exploitability
- A fix or mitigation plan will be developed before public disclosure
- Credit will be given to the reporter (unless you prefer to remain anonymous)

---

## Scope

Relevant security areas for AutoType X1:

- Bluetooth HID connection handling
- Input processing of user-supplied scripts
- Foreground service lifecycle and permissions
- Local data storage (Room, DataStore)
- Android component exposure (exported activities/services)

Out of scope:

- Vulnerabilities in the Android OS itself
- Vulnerabilities in the Bluetooth hardware or firmware
- Denial-of-service attacks requiring physical proximity
- Bugs that do not have a security impact

---

## Security Design Notes

- AutoType X1 holds **no INTERNET permission** — it cannot communicate with remote servers
- The foreground service is **not exported** — it cannot be started by other apps
- No data is transmitted off-device
- Scripts are stored only in the app's private local database
