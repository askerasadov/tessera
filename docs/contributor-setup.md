# Contributor Setup

This document walks a new contributor through the local-machine setup needed to open a pull request against this repository: cloning, configuring Git identity, and setting up SSH commit and tag signing. Where [`CONTRIBUTING.md`](../CONTRIBUTING.md) points at the rules and [`.claude/git-workflow.md`](../.claude/git-workflow.md) describes the per-PR workflow, this document covers the one-time machine setup that has to happen before either of those is useful.

This document is living. The instructions evolve as the project's tooling and contributor base grow.

The macOS path is the project-verified one as of 2026-05. Linux and Windows follow the same logic but have not been end-to-end verified against this project's branch protection. If you complete setup on Linux or Windows and find a gap, opening a PR to firm up that section is welcome.

---

## What you need before starting

These are universal across platforms:

- **Git 2.34 or later.** `git --version` to check. SSH signing requires 2.34+.
- **JDK 21.** The project compiles to JDK 21 (`jvmToolchain(21)` in every module's `build.gradle.kts`) and CI runs on JDK 21. The Gradle daemon itself is pinned to JDK 17 (`gradle/gradle-daemon-jvm.properties`); foojay-resolver auto-provisions it on first build if it's missing locally, so installing JDK 21 alone is enough — expect a one-time JDK 17 download the first time you run `./gradlew`. [Adoptium Temurin](https://adoptium.net/) is a safe default on any platform.
- **A GitHub account** with permission to fork or push to [askerasadov/Tessera](https://github.com/askerasadov/Tessera).
- **(Optional) The GitHub CLI** (`gh`). Useful for opening PRs from the command line; see [the GitHub CLI docs](https://cli.github.com/). The per-PR workflow in [`.claude/git-workflow.md`](../.claude/git-workflow.md) uses `gh` in its examples.

You do not need a Sonatype account, a GPG key for artifact signing, or any other release-time tooling. Those are author-only concerns.

---

## 1. Clone the repository

```sh
git clone https://github.com/askerasadov/Tessera.git
cd Tessera
```

If you plan to contribute via fork, fork on GitHub first and clone your fork; you can add the upstream remote afterwards:

```sh
git remote add upstream https://github.com/askerasadov/Tessera.git
```

---

## 2. Configure your Git identity

The email you set here must match an email registered on your GitHub account. GitHub uses it to attribute commits and to display the "Verified" badge on signed commits when it matches a registered Signing Key.

```sh
git config --local user.name "Your Name"
git config --local user.email "you@example.com"
```

Using `--local` (not `--global`) keeps these settings scoped to this repository, which matches the convention the rest of this setup uses. If you prefer a global identity for all your repos, use `--global` instead.

---

## 3. SSH signing setup — pick your platform

Branch protection on `main` requires every commit and every release tag to be signed by a key registered to a GitHub account with push permission.

**Choose your path:**

- **Already have an SSH key you want to use for signing** (most commonly `~/.ssh/id_ed25519` from prior SSH setup): skip step 3a in your platform's section below. The same key file works for both authentication and signing — GitHub treats them as separate registrations, so you can register one key under both types. Continue from step 3b.
- **Setting up from scratch**: start with step 3a in your platform's section.

Jump to yours:

- [macOS](#macos)
- [Linux](#linux)
- [Windows](#windows)

If you already have a GPG signing setup you prefer, GitHub supports that too; substitute accordingly. The walkthroughs below use SSH signing because it's the simplest path and what the project author uses.

---

### macOS

#### 3a. Generate an SSH signing key (skip if you already have one)

```sh
ssh-keygen -t ed25519 -f ~/.ssh/id_ed25519 -C "you@example.com"
```

Replace `you@example.com` with your real email — it's stored as the key's comment field and helps you identify the key in `ssh-add -l` and similar listings.

Set a passphrase when prompted. You'll cache it in your login Keychain in step 3e so you only enter it once. Store the passphrase in a password manager — losing it means regenerating the key.

#### 3b. Configure Git to sign with that key

From inside the Tessera checkout (substitute your key's path if you generated it under a different name):

```sh
git config --local gpg.format ssh
git config --local user.signingkey ~/.ssh/id_ed25519.pub
git config --local commit.gpgsign true
git config --local tag.gpgsign true
```

#### 3c. Add the public key to GitHub as a Signing Key

Go to **Settings → SSH and GPG keys → New SSH key** (<https://github.com/settings/ssh/new> when logged in). Change the **Key type** dropdown from the default **Authentication Key** to **Signing Key**, then paste the contents of:

```sh
cat ~/.ssh/id_ed25519.pub
```

The distinction matters: **Authentication Key** lets the key push to repositories, while **Signing Key** lets GitHub verify the signature it sees on a commit or tag. They are independent slots; the same key can be registered as both but each registration is separate. If you're reusing an existing key already registered as Authentication Key, register it again as Signing Key — same key contents, different type.

#### 3d. (Optional) Set up local verification

GitHub will show "Verified" on signed commits and tags it can verify against a registered Signing Key. If you also want `git log --show-signature` and `git tag -v` to verify locally — useful for due diligence on commits you receive from others — set up an allowed-signers file:

```sh
echo "you@example.com $(cat ~/.ssh/id_ed25519.pub)" >> ~/.ssh/allowed_signers
git config --local gpg.ssh.allowedSignersFile ~/.ssh/allowed_signers
```

The file format is one line per signer: `<email> <ssh-public-key>`. Add entries for other contributors' keys as you receive them.

#### 3e. Cache the passphrase in your Keychain

Without this step, every signed commit prompts for the passphrase.

```sh
ssh-add --apple-use-keychain ~/.ssh/id_ed25519
```

You'll be prompted once; macOS stores the passphrase in your login Keychain. **The `~/.ssh/config` block below is required, not optional** — without it, you'd have to run `ssh-add` manually every new login to repopulate the agent (macOS clears the agent on every reboot regardless of this block; the block makes the repopulation automatic on first SSH client use). Add to `~/.ssh/config`:

```
Host *
  IdentityFile ~/.ssh/id_ed25519
  UseKeychain yes
  AddKeysToAgent yes
```

The block instructs the SSH **client** (used by `git push`, `git fetch`, `ssh`) to read the passphrase from Keychain on first use and load the key into the agent. After any one ssh client operation in a new login session, the agent holds the key and `git commit` can sign through it. `git commit`'s signing path (`ssh-keygen -Y sign`) does not read `~/.ssh/config` itself — it relies on the agent already being populated. In normal workflow this is invisible (most sessions start with a `pull` or `push`); see section 4 if your first SSH operation in a session is a commit and signing fails.

#### 3f. Verify with a test commit

Make any small change, commit, and check:

```sh
git log --show-signature -1
```

You should see `Good "git" signature for you@example.com with ED25519 key SHA256:...`. If you set up step 3d, this also confirms local verification works.

---

### Linux

#### 3a. Generate an SSH signing key (skip if you already have one)

```sh
ssh-keygen -t ed25519 -f ~/.ssh/id_ed25519 -C "you@example.com"
```

Replace `you@example.com` with your real email — it's stored as the key's comment field and helps you identify the key in `ssh-add -l` and similar listings.

Set a passphrase when prompted. You'll load it into `ssh-agent` in step 3e to avoid re-entering it on every commit. Store the passphrase in a password manager — losing it means regenerating the key.

#### 3b. Configure Git to sign with that key

From inside the Tessera checkout (substitute your key's path if you generated it under a different name):

```sh
git config --local gpg.format ssh
git config --local user.signingkey ~/.ssh/id_ed25519.pub
git config --local commit.gpgsign true
git config --local tag.gpgsign true
```

#### 3c. Add the public key to GitHub as a Signing Key

Go to **Settings → SSH and GPG keys → New SSH key** (<https://github.com/settings/ssh/new> when logged in). Change the **Key type** dropdown from the default **Authentication Key** to **Signing Key**, then paste the contents of:

```sh
cat ~/.ssh/id_ed25519.pub
```

The distinction matters: **Authentication Key** lets the key push to repositories, while **Signing Key** lets GitHub verify the signature it sees on a commit or tag. They are independent slots; the same key can be registered as both but each registration is separate. If you're reusing an existing key already registered as Authentication Key, register it again as Signing Key — same key contents, different type.

#### 3d. (Optional) Set up local verification

GitHub will show "Verified" on signed commits and tags it can verify against a registered Signing Key. If you also want `git log --show-signature` and `git tag -v` to verify locally — useful for due diligence on commits you receive from others — set up an allowed-signers file:

```sh
echo "you@example.com $(cat ~/.ssh/id_ed25519.pub)" >> ~/.ssh/allowed_signers
git config --local gpg.ssh.allowedSignersFile ~/.ssh/allowed_signers
```

The file format is one line per signer: `<email> <ssh-public-key>`. Add entries for other contributors' keys as you receive them.

#### 3e. Load the key into ssh-agent

Without this step, every signed commit prompts for the passphrase.

```sh
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_ed25519
```

You'll be prompted once per agent session. Unlike macOS, Linux `ssh-agent` doesn't persist across reboots on its own. The right way to make it persistent depends on your distribution and desktop environment:

- Most desktop environments (GNOME, KDE, etc.) start an `ssh-agent` automatically and integrate with the system keyring. Check `echo "$SSH_AUTH_SOCK"` — if it's set on login, you're already set; just `ssh-add` once per login.
- For headless or minimal setups, the [`keychain`](https://www.funtoo.org/Keychain) wrapper is the common solution. Install via your package manager, then add `eval "$(keychain --eval --quiet ~/.ssh/id_ed25519)"` to your shell rc.
- See the [Arch wiki SSH keys page](https://wiki.archlinux.org/title/SSH_keys#SSH_agents) for a thorough overview.

#### 3f. Verify with a test commit

Make any small change, commit, and check:

```sh
git log --show-signature -1
```

You should see `Good "git" signature for you@example.com with ED25519 key SHA256:...`. If you set up step 3d, this also confirms local verification works.

---

### Windows

OpenSSH is included with Windows 10 (1809) and later as an optional feature; on most modern Windows installs it's already present. If `ssh -V` reports "command not found", enable it via **Settings → Apps → Optional features → Add a feature → OpenSSH Client**, or run `winget install Microsoft.OpenSSH.Beta` for the latest.

The commands below assume PowerShell. Adjust quoting if you're using Git Bash or another shell.

#### 3a. Generate an SSH signing key (skip if you already have one)

```powershell
ssh-keygen -t ed25519 -f $HOME\.ssh\id_ed25519 -C "you@example.com"
```

Replace `you@example.com` with your real email — it's stored as the key's comment field and helps you identify the key in `ssh-add -l` and similar listings.

Set a passphrase when prompted. You'll load it into the OpenSSH Authentication Agent service in step 3e to avoid re-entering it on every commit. Store the passphrase in a password manager — losing it means regenerating the key.

#### 3b. Configure Git to sign with that key

From inside the Tessera checkout (substitute your key's path if you generated it under a different name):

```powershell
git config --local gpg.format ssh
git config --local user.signingkey "$HOME\.ssh\id_ed25519.pub"
git config --local commit.gpgsign true
git config --local tag.gpgsign true
```

#### 3c. Add the public key to GitHub as a Signing Key

Go to **Settings → SSH and GPG keys → New SSH key** (<https://github.com/settings/ssh/new> when logged in). Change the **Key type** dropdown from the default **Authentication Key** to **Signing Key**, then paste the contents of:

```powershell
Get-Content $HOME\.ssh\id_ed25519.pub
```

The distinction matters: **Authentication Key** lets the key push to repositories, while **Signing Key** lets GitHub verify the signature it sees on a commit or tag. They are independent slots; the same key can be registered as both but each registration is separate. If you're reusing an existing key already registered as Authentication Key, register it again as Signing Key — same key contents, different type.

#### 3d. (Optional) Set up local verification

GitHub will show "Verified" on signed commits and tags it can verify against a registered Signing Key. If you also want `git log --show-signature` and `git tag -v` to verify locally — useful for due diligence on commits you receive from others — set up an allowed-signers file:

```powershell
$pub = Get-Content $HOME\.ssh\id_ed25519.pub
Add-Content -Path $HOME\.ssh\allowed_signers -Value "you@example.com $pub"
git config --local gpg.ssh.allowedSignersFile "$HOME\.ssh\allowed_signers"
```

The file format is one line per signer: `<email> <ssh-public-key>`. Add entries for other contributors' keys as you receive them.

#### 3e. Enable the ssh-agent service and load the key

Without this step, every signed commit prompts for the passphrase. The `ssh-agent` service ships disabled by default on Windows; enable it once as Administrator:

```powershell
Get-Service ssh-agent | Set-Service -StartupType Automatic
Start-Service ssh-agent
```

Then, from a regular (non-admin) PowerShell:

```powershell
ssh-add $HOME\.ssh\id_ed25519
```

The agent persists across reboots because it runs as a service. You'll only re-enter the passphrase if the key changes or the agent's stored keys are cleared.

**Note on Git for Windows:** Git for Windows ships its own bundled OpenSSH that is independent of the system one. If you're using Git for Windows, point its `ssh.exe` at the system OpenSSH so `git` and `ssh-add` agree on which agent they're talking to:

```powershell
git config --global core.sshCommand "C:/Windows/System32/OpenSSH/ssh.exe"
```

Mismatched agents are the most common Windows-specific failure mode.

#### 3f. Verify with a test commit

Make any small change, commit, and check:

```powershell
git log --show-signature -1
```

You should see `Good "git" signature for you@example.com with ED25519 key SHA256:...`. If you set up step 3d, this also confirms local verification works.

---

## 4. Troubleshooting

These issues are platform-agnostic. Platform-specific passphrase / agent problems are covered in step 3e of your platform's section above.

### `git commit` hangs silently after a reboot or login

Symptom: a commit hangs with no output (interactive terminal), or fails with an `incorrect passphrase` / similar error (non-interactive context — scripted commits, IDE-driven commits with no passphrase UI, agentic flows). Running `ssh-add -l` in another terminal reports "The agent has no identities" (or returns an error).

The agent is empty and `git`'s call into `ssh-keygen -Y sign` has no key to sign with. ssh-keygen falls back to prompting for the passphrase on a TTY — which hangs interactively, or reads garbage from a missing stdin and fails with `incorrect passphrase` in non-interactive contexts. The fix is platform-specific:

- **macOS** — usually the `~/.ssh/config` block from step 3e is missing, or your Keychain passphrase entry was cleared. Re-do step 3e. To confirm Keychain has the passphrase: `ssh-add -D && ssh-add --apple-use-keychain ~/.ssh/id_ed25519` should not prompt; if it does, enter the passphrase once and Keychain will store it. Note: the `security find-generic-password` CLI may report "not found" even when an iCloud or data-protection keychain entry exists — trust the empirical `ssh-add` test over the CLI query. **Edge case — the bridge is fine but hasn't fired yet:** the `~/.ssh/config` block populates the agent on first ssh *client* operation (`git push`, `git fetch`, `ssh`), not on signing. If your first SSH operation in a session happens to be a commit (common in scripted, IDE-driven, or agentic flows), the agent will still be empty even with the bridge correctly configured. One manual `ssh-add --apple-use-keychain ~/.ssh/id_ed25519` (silent if Keychain holds the passphrase) populates the agent and unblocks signing; or just run any `git pull` / `git fetch` first.
- **Linux** — your DE's keyring integration or the `keychain` wrapper didn't start on this login. Re-do step 3e for the current session (`eval "$(ssh-agent -s)" && ssh-add ~/.ssh/id_ed25519`). For permanent persistence, pick one of the mechanisms listed in step 3e (DE keyring auto-start, or the `keychain` wrapper invoked from your shell rc) so future logins don't repeat the problem.
- **Windows** — verify `Get-Service ssh-agent` reports both `Running` and `Automatic`. If you use Git for Windows, also verify `git config --get core.sshCommand` points at the system OpenSSH; mismatched agents is the most common Windows-specific failure per step 3e's note.

### "Good 'git' signature" locally but GitHub shows "Unverified"

The signature is valid against your local key, but GitHub can't find a matching Signing Key registered to an account with permission to attribute the commit. Common causes:

- The key was added as **Authentication** instead of **Signing** in step 3c — re-add it as Signing.
- The email on your `user.email` (step 2) doesn't match an email registered to your GitHub account. Check **Settings → Emails** on GitHub.
- The commit was made before the Signing Key was added to GitHub. GitHub does not re-verify historical commits; create a new commit signed under the new configuration.

### "missing allowed signers file" when running `git tag -v`

Step 3d is incomplete — `gpg.ssh.allowedSignersFile` is configured but the referenced file doesn't exist or isn't readable. Re-run the file-creation lines from step 3d, or unset the config if you don't need local verification:

```sh
git config --local --unset gpg.ssh.allowedSignersFile
```

GitHub-side verification works independent of this file; the setting is purely for your own local `git verify-commit` and `git tag -v` checks.

### Push rejected with "Commits must have verified signatures"

The remote saw an unsigned commit in the range you're pushing. Check `git log --show-signature <range>`; the unsigned commit needs to be re-signed (rebase / amend, depending on where it sits) before the push will be accepted. The branch-protection rule has no bypass, including for repo admins.

### Squash-merged commits show "Unverified" or "committed by GitHub" in IntelliJ

You merged a PR via GitHub's "Squash and merge" button. Locally, the merged commit shows up with **committer: `GitHub <noreply@github.com>`** and IntelliJ's commit panel reports **GPG signature unverified**. This is normal and not a problem.

What's happening:

- When you click "Squash and merge" on github.com, GitHub's servers create a **new** squash commit on their end and **sign it with GitHub's own GPG key**. That commit's *author* is preserved as you, but the *committer* becomes `GitHub <noreply@github.com>`.
- On github.com, that commit shows a green **Verified** badge — GitHub knows its own key.
- Locally, IntelliJ doesn't have GitHub's public GPG key in your GnuPG keyring, so it can't verify the signature and labels it "Unverified."

If you want IntelliJ to recognize these as verified locally, import GitHub's public signing key once:

```sh
curl https://github.com/web-flow.gpg | gpg --import
```

After that, `git log --show-signature` (and IntelliJ, after a restart) will report "Good signature from 'GitHub <noreply@github.com>'." Purely cosmetic; doesn't change anything about security.

---

## Related documents

- [`CONTRIBUTING.md`](../CONTRIBUTING.md) — the contributor's entry point; this document is the one-time setup that supports it
- [`.claude/git-workflow.md`](../.claude/git-workflow.md) — the per-PR workflow (branch, commit, push, PR) once setup is done
- [`docs/conventions.md`](conventions.md) — naming conventions, code style, documentation rules
- [`docs/decisions/0007-strict-backward-compat-from-0x.md`](decisions/0007-strict-backward-compat-from-0x.md) — why discipline around the main branch matters even in `0.x`
