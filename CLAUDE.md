# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build System

EGit uses **Apache Maven with Tycho** (Eclipse's OSGi build system). Requires Maven 3.9+ and Java 21.

```bash
# Full build (skipping UI tests)
mvn clean verify -DskipTests

# Build with all tests
mvn clean verify

# Build with static analysis (SpotBugs + PMD)
mvn clean verify -Pstatic-checks

# Skip UI tests explicitly
mvn clean verify -Pskip-ui-tests

# Single module build (run from repo root)
mvn clean verify -pl org.eclipse.egit.core
```

JVM heap is configured in `.mvn/jvm.config` (`-Xmx1024m`).

## Running Tests

Tests use **Tycho Surefire**. There are three test bundles:

- `org.eclipse.egit.core.test` — non-UI unit tests
- `org.eclipse.egit.gitflow.test` — gitflow unit tests
- `org.eclipse.egit.ui.test` — SWTBot UI tests (slow, requires display)

```bash
# Run only core tests
mvn clean verify -pl org.eclipse.egit.core.test

# Run a single test class (Tycho Surefire syntax)
mvn clean verify -pl org.eclipse.egit.core.test -Dtest=MyTestClass

# Skip all UI tests
mvn clean verify -Pskip-ui-tests
```

UI tests write screenshots to `target/screenshots/` on failure and have a 30-second SWTBot timeout.

## Architecture

EGit is a layered Eclipse plugin stack:

```
org.eclipse.egit.ui / org.eclipse.egit.gitflow.ui   ← Eclipse workbench UI
        ↓
org.eclipse.egit.core / org.eclipse.egit.gitflow     ← Eclipse Team provider + JGit bridge
        ↓
JGit (org.eclipse.jgit, separate sibling project)    ← Pure-Java Git implementation
        ↓
Git repositories on disk
```

**`org.eclipse.egit.core`** implements the Eclipse Team provider abstraction (`GitProvider`), hooks into workspace resource changes (`GitMoveDeleteHook`), manages repository discovery within the Eclipse workspace, and bridges JGit operations to Eclipse's progress/proxy/authentication APIs.

**`org.eclipse.egit.ui`** contains all workbench UI: staging view, history view, resource decorators, compare editor integration, and dialogs for clone/push/pull/merge/rebase/cherry-pick/commit operations.

**`org.eclipse.egit.gitflow`** + **`.gitflow.ui`** are optional components implementing the gitflow branching model.

**`org.eclipse.egit.doc`** provides Eclipse help system integration (cheat sheets, context help, intro pages).

**`org.eclipse.egit.target`** defines target platforms for multiple Eclipse release trains (4.36–4.39) and OS/arch combinations (Linux x86_64, Windows x86_64, macOS x86_64/aarch64).

**`org.eclipse.egit.repository`** defines the P2 update site for distribution.

## Commit Conventions

Commit messages follow the standard Eclipse/Gerrit format:

```
Short imperative subject line (no period)

Optional body explaining the what and why. Wrap at ~72 characters.
Reference issues or describe motivation here.

Bug: egit-123
Change-Id: I0000000000000000000000000000000000000000
```

- **Subject**: imperative mood, no trailing period, ~72 chars max
- **Body**: separated from subject by a blank line; explain motivation and context, not just what changed
- **`Bug:`**: optional; references a GitHub issue number (e.g. `Bug: egit-63`)
- **`Change-Id:`**: required by Gerrit; use a placeholder (`I000...`) when creating the commit locally — Gerrit assigns the real ID on push
- Do **not** add a `Co-Authored-By:` trailer — the Eclipse Contributor Agreement (ECA) check on CI will fail for unknown authors

## Code Review Process

This project uses **GerritHub** for code review — GitHub PRs are not merged. Submit patches with:

```bash
git push ssh://USERNAME@eclipse.gerrithub.io:29418/eclipse-egit/egit HEAD:refs/for/master
```

See [CONTRIBUTING.md](CONTRIBUTING.md) for the full contributor guide link.

## Key Developer Files

- `tools/eclipse-JGit-Format.xml` — Eclipse code formatter configuration (import before editing)
- `tools/release.sh` / `tools/version.sh` — Release and version bump scripts
- `.mvn/jvm.config` — JVM heap settings for Maven builds
