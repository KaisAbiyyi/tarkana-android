# Tarkana Android: Branch Protection & Ruleset Policy

> Canonical branch protection rules and GitHub Ruleset configuration for the Tarkana Android repository (`tarkana-android`).

---

## 1. Target Branch: `main`

Direct pushes to `main` are strictly prohibited. All native Android changes must arrive via pull request with verified validation evidence.

---

## 2. GitHub Ruleset Configuration

### Target Enforcement
- **Rule Type**: Branch Ruleset (`Protect main branch`)
- **Enforcement Status**: Active
- **Target Branches**: `refs/heads/main`
- **Bypass List**: Repository Admin (Emergency break-glass only, requiring post-incident review)

### Restrictions
- **Restrict deletions**: Enabled (prevents accidental branch deletion)
- **Block force pushes**: Enabled (prevents history rewrite or divergent commits)
- **Require linear history**: Enabled (enforces rebase or squash-and-merge)

### Pull Request Requirements
- **Require pull request before merging**: Enabled
- **Dismiss stale pull request approvals when new commits are pushed**: Enabled
- **Require conversation resolution before merging**: Enabled

### Required Status Checks
All required checks must pass prior to merge:
1. **Lint, Test & Assemble** (Workflow: `Android CI`, `.github/workflows/android-ci.yml`)
   - Android Lint (`./gradlew lintDebug`)
   - Unit Tests (`./gradlew testDebugUnitTest`)
   - Debug Build (`./gradlew assembleDebug`)

---

## 3. Automation Setup (GitHub CLI)

To apply or verify these branch rules via GitHub CLI using the tracked configuration:

```bash
gh api \
  --method POST \
  -H "Accept: application/vnd.github+json" \
  /repos/KaisAbiyyi/tarkana-android/rulesets \
  --input .github/ruleset-config.json
```
