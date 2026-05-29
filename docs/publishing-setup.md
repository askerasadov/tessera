# Publishing Setup (Maintainer Guide)

This document is for the person who publishes Tessera artifacts to Maven Central — currently the project maintainer. It walks through the one-time setup that has to happen before the signing slice ([slice 4 of the publishing infrastructure committed in ADR-016](decisions/0016-maven-coordinates-and-first-publish.md)) can be wired up, and before the first staging publish to the Sonatype Central Publishing Portal can run.

This document is living. As the publishing process evolves (CI takes over, rotations happen, additional registries are added), this document updates.

**Contributors do not need to do any of this.** The per-contributor machine setup (cloning, Git signing, JDK toolchain) lives in [`contributor-setup.md`](contributor-setup.md). This document is the maintainer-side counterpart — the steps a contributor explicitly does NOT need per `contributor-setup.md`'s "You do not need a Sonatype account, a GPG key for artifact signing, or any other release-time tooling" line.

---

## What needs to exist

Before the SDK can publish to Maven Central:

1. A **PGP signing key** in the maintainer's local GnuPG keyring, with its public key uploaded to a public keyserver
2. Sonatype Central Publishing Portal **account access** for the `io.lightine` namespace (already done — the namespace was claimed and verified during the [Distribution channels deferral resolution](open-questions.md), banked in PR [#75](https://github.com/askerasadov/tessera/pull/75) and locked under [ADR-016](decisions/0016-maven-coordinates-and-first-publish.md))
3. A Sonatype Central Portal **user token** (generated; distinct from the account login password)
4. **Credentials stored** where Gradle can read them — either user-level `gradle.properties` or environment variables

Steps below cover each in order. Once all four exist, the signing slice (PR 4) becomes unblocked and the staging-publish slice (PR 5) can run end-to-end.

---

## 1. PGP signing key

### Why this is separate from the SSH signing key

The project already uses an SSH signing key for git commits (set up per [`contributor-setup.md`](contributor-setup.md) section 3 and described in [`.claude/git-workflow.md`](../.claude/git-workflow.md)). Maven Central requires a **PGP/GnuPG** key for signing published artifacts: different algorithm family (RSA/ECC under the OpenPGP standard), different toolchain (`gpg`), different file format, different keyserver infrastructure. The two have nothing to do with each other.

Do not try to convert or reuse the SSH key. Generate the PGP key fresh.

### Install GnuPG

**macOS:**

```bash
brew install gnupg
gpg --version  # confirm 2.4.x or newer
```

**Linux (Ubuntu/Debian):** GnuPG is usually preinstalled. Check `gpg --version`; install via `sudo apt install gnupg` if missing.

**Windows:** Install [Gpg4win](https://gpg4win.org/) (bundles GnuPG plus a GUI).

The macOS path is the project-verified one as of 2026-05. Linux and Windows follow the same `gpg` CLI verbs; the package-management commands differ.

### Generate the key

```bash
gpg --full-generate-key
```

When prompted:

- **Key type:** `(1) RSA and RSA` (default — primary key for signing, subkey for encryption)
- **Key size:** `4096` (current recommendation; `3072` is also fine, and is the GnuPG default)
- **Expiry:** Choose based on preference:
  - `0` (no expiry) — simplest; same key forever
  - `2y` (2 years) — best practice; renew before expiry. Past artifacts stay verifiable even after the key expires; only new signing requires extension or rotation
  - Recommendation: `2y` with a calendar reminder to renew at the 22-month mark
- **Real name:** `Asker Asadov` (matches the POM `<developer><name>` field)
- **Email:** `asker.asadov@gmail.com` (matches the POM `<developer><email>` field)
- **Comment:** `Maven Central signing` (optional; helps you identify the key in `gpg --list-keys` output later — leave blank if you prefer a UID without a comment)
- **Passphrase:** Strong. Save in your password manager. Required every time you sign a release (Gradle can cache for a daemon session if you configure `gpg-agent`)

### Find your key ID

```bash
gpg --list-secret-keys --keyid-format LONG
```

Output looks like:

```
sec   rsa4096/ABCD1234EF567890 2026-05-27 [SC]
      0123456789ABCDEF0123456789ABCDEF01234567
uid                 [ultimate] Asker Asadov (Maven Central signing) <asker.asadov@gmail.com>
ssb   rsa4096/0987654321FEDCBA 2026-05-27 [E]
```

The 16-character ID after `rsa4096/` on the `sec` line is your **signing key ID** (`ABCD1234EF567890` in the example above). You'll need this in step 4. The full fingerprint on the next line is what keyservers and Maven Central look up.

### Back up the secret key

The key only exists on this machine. If you lose it, you lose the ability to sign **new** releases. Already-published artifacts on Maven Central stay verifiable forever (Sonatype caches the public key); the loss only affects future signing.

Export to an ASCII-armored file:

```bash
gpg --export-secret-keys --armor <KEY_ID> > tessera-signing-key.asc
```

Store the resulting `.asc` file encrypted — examples:

- 1Password "secure note" attachment
- Encrypted USB stick in a physical safe
- A second machine with disk encryption that is rarely online

Then delete the unencrypted file from the maintainer machine:

```bash
rm tessera-signing-key.asc
```

---

## 2. Upload the public key to a keyserver

Maven Central validates signed artifacts by fetching the signer's public key from a keyserver. The signature in the `.asc` file references your key fingerprint; Maven Central looks the fingerprint up at publish time.

**Recommended keyserver:** [`keys.openpgp.org`](https://keys.openpgp.org/about) — verified-email model; only keys whose email has been confirmed are searchable by email. Other keyservers commonly used (no email verification required): `keyserver.ubuntu.com`, `pgp.mit.edu`.

Send the public key:

```bash
gpg --keyserver keys.openpgp.org --send-keys <KEY_ID>
```

Then check the email account you used for the key (`asker.asadov@gmail.com` per step 1). `keys.openpgp.org` sends a verification link. Click it. The key becomes searchable by email once verified.

**Verify the upload from a different terminal session** (or, ideally, a different machine):

```bash
gpg --keyserver keys.openpgp.org --recv-keys <KEY_ID>
```

Should retrieve the public key cleanly. If it returns "key not found," wait a few minutes for propagation and try again.

For redundancy, you can `--send-keys` to multiple servers — Maven Central's validation can fetch from any of them. One server is enough for first publish.

---

## 3. Sonatype Central Portal user token

The Sonatype Central Publishing Portal (the post-2024 endpoint, replacing the legacy OSSRH that was deprecated in mid-2024) authenticates publish requests with **user tokens**, not account passwords. Generate one:

1. Go to [https://central.sonatype.com](https://central.sonatype.com) and sign in with the account that owns the `io.lightine` namespace (verified earlier; the namespace verification step is the one done before [ADR-016](decisions/0016-maven-coordinates-and-first-publish.md) was written)
2. Top-right account menu → "View Account" → "Generate User Token"
3. Sonatype shows the token's **username** and **password** **once** — copy both immediately into your password manager. They cannot be retrieved later; if lost, generate a new token (which invalidates the previous one)

The token pair (username + password) is what Gradle uses to authenticate publish requests. It is not the same as your Sonatype account login.

---

## 4. Store credentials where Gradle can read them

Vanniktech's `gradle-maven-publish-plugin` (wired up in PR [#80](https://github.com/askerasadov/tessera/pull/80)) reads five credential values:

| What | Gradle property | Env var (cross-platform) |
|---|---|---|
| Signing key (ASCII-armored, multi-line as `\n`-joined single line) | `signingInMemoryKey` | `ORG_GRADLE_PROJECT_signingInMemoryKey` |
| Signing key ID | `signingInMemoryKeyId` | `ORG_GRADLE_PROJECT_signingInMemoryKeyId` |
| Signing key passphrase | `signingInMemoryKeyPassword` | `ORG_GRADLE_PROJECT_signingInMemoryKeyPassword` |
| Sonatype token username | `mavenCentralUsername` | `ORG_GRADLE_PROJECT_mavenCentralUsername` |
| Sonatype token password | `mavenCentralPassword` | `ORG_GRADLE_PROJECT_mavenCentralPassword` |

Pick **one** storage approach.

### Option A — user-level `~/.gradle/gradle.properties` (recommended for the first publish)

The user-level Gradle properties file is per-machine and is never committed to any project. Edit `~/.gradle/gradle.properties` (create it if it doesn't exist) and add:

```properties
# Tessera Maven Central publishing — credentials, do not share
signingInMemoryKey=-----BEGIN PGP PRIVATE KEY BLOCK-----\n\nlQc...\n-----END PGP PRIVATE KEY BLOCK-----
signingInMemoryKeyId=ABCD1234EF567890
signingInMemoryKeyPassword=<your passphrase>
mavenCentralUsername=<token username from step 3>
mavenCentralPassword=<token password from step 3>
```

**Important:** the PGP key is a multi-line block, but Gradle properties don't support multi-line values. The vanniktech plugin parses literal `\n` two-character sequences as newlines. Convert the multi-line key to a single line:

**macOS:**

```bash
gpg --export-secret-keys --armor <KEY_ID> | awk '{printf "%s\\n", $0}' | pbcopy
```

The result on your clipboard is one long line; paste after `signingInMemoryKey=`.

**Linux:**

```bash
gpg --export-secret-keys --armor <KEY_ID> | awk '{printf "%s\\n", $0}' | xclip -selection clipboard
```

(Or pipe to a file and copy manually.)

### Option B — environment variables per release (better for CI later)

```bash
export ORG_GRADLE_PROJECT_signingInMemoryKey="$(gpg --export-secret-keys --armor <KEY_ID>)"
export ORG_GRADLE_PROJECT_signingInMemoryKeyId="ABCD1234EF567890"
export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword="<passphrase>"
export ORG_GRADLE_PROJECT_mavenCentralUsername="<token username>"
export ORG_GRADLE_PROJECT_mavenCentralPassword="<token password>"

./gradlew publishToMavenCentral  # (slice 5 will introduce this task)
```

Env vars don't persist across shells; you set them each release. Pair with a secrets manager (1Password CLI, AWS Secrets Manager, etc.) to populate them on demand. The multi-line PGP key works naturally with env vars (no `\n` escaping needed).

### Recommendation for the first publish

Option A (`~/.gradle/gradle.properties`) is the path of least friction for the first 0.1.1 publish. Later, when CI takes over publishing, switch to Option B's env-var pattern populated by GitHub Actions secrets.

---

## 5. Verification before slice 4 ships

These are sanity checks the maintainer can do before the signing-slice PR opens:

```bash
# Confirm the key is in the local keyring
gpg --list-secret-keys --keyid-format LONG
# Should show the rsa4096 sec key with the expected name + email + comment

# Confirm the public key is on the keyserver
gpg --keyserver keys.openpgp.org --recv-keys <KEY_ID>
# Should retrieve cleanly, not "key not found"

# Confirm Gradle reads the credentials (after slice 4 wires the signing plugin —
# this command currently runs without signing, so it succeeds either way)
./gradlew publishToMavenLocal --console=plain
```

Once slice 4 lands, the same `publishToMavenLocal` invocation will produce `.asc` signature files alongside each artifact, e.g.:

```
~/.m2/repository/io/lightine/tessera/tessera-mrz-core/0.1.1/tessera-mrz-core-0.1.1.jar
~/.m2/repository/io/lightine/tessera/tessera-mrz-core/0.1.1/tessera-mrz-core-0.1.1.jar.asc
~/.m2/repository/io/lightine/tessera/tessera-mrz-core/0.1.1/tessera-mrz-core-0.1.1.pom
~/.m2/repository/io/lightine/tessera/tessera-mrz-core/0.1.1/tessera-mrz-core-0.1.1.pom.asc
```

Verify a signature manually:

```bash
gpg --verify ~/.m2/repository/io/lightine/tessera/tessera-mrz-core/0.1.1/tessera-mrz-core-0.1.1.jar.asc
# Should report "Good signature from Asker Asadov ... <asker.asadov@gmail.com>"
```

---

## Forward-looking: when CI publishes

Once the maintainer-driven first publish lands and the workflow is proven, future releases likely move to a GitHub Actions workflow. The setup is similar but credentials live in repository secrets rather than local files:

| Repository secret | Value |
|---|---|
| `MAVEN_CENTRAL_USERNAME` | Sonatype token username |
| `MAVEN_CENTRAL_PASSWORD` | Sonatype token password |
| `SIGNING_KEY` | ASCII-armored secret key (multi-line preserved — env vars handle it natively) |
| `SIGNING_KEY_ID` | Signing key ID |
| `SIGNING_KEY_PASSWORD` | Signing key passphrase |

The Actions workflow maps secrets to `ORG_GRADLE_PROJECT_*` env vars before invoking Gradle. That's a separate slice (currently uncaptured); add it when the maintainer decides to move publishing to CI.

---

## Cross-references

- [`decisions/0016-maven-coordinates-and-first-publish.md`](decisions/0016-maven-coordinates-and-first-publish.md) — coordinate shape, lockstep versioning, BOM, first-publish version and scope; the umbrella decision this setup serves
- [`contributor-setup.md`](contributor-setup.md) — per-contributor machine setup (SSH signing key, JDK toolchain, IDE config); the contributor-side counterpart to this document
- [`conventions.md`](conventions.md) — module naming, package conventions, including the "Module Boundaries" rule that constrains what each published artifact contains
- The publishing infrastructure slices to date: PR [#80](https://github.com/askerasadov/tessera/pull/80) (vanniktech plugin + POM metadata + lockstep version), PR [#81](https://github.com/askerasadov/tessera/pull/81) (Dokka 2 for javadoc jars), PR [#82](https://github.com/askerasadov/tessera/pull/82) (`tessera-bom`); the signing + Sonatype-Central + 0.1.1-tag slices land after the setup in this document is complete
