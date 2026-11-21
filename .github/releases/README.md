# Release notes

One file per version, `<version>.md`, named as `versionName` in `app/build.gradle.kts`.
Commit it with the version bump, then push a tag `v<version>` on that commit
(`git tag v1.8.0 && git push origin v1.8.0`): `.github/workflows/release.yml` builds the
signed APK, draws the banner and publishes the release. Pushes without a tag never release.

Above the first `---`: the banner's words (`tools/make-release-banner.py`), a subtitle and
up to four short highlights. Below it: the release text, in Markdown. The workflow adds the
banner on top and the install line and signing certificate at the end.

```
subtitle: Faster, cleaner, closer to instant
- Faster start
- Cleaner offline
- Timetable week view
- AI chat, upgraded
---
**What this release is about**, in one sentence.

### A section
- **A change:** what it does for the user.
```

Preview the banner locally with `python3 tools/make-release-banner.py` (it reads the version
from the Gradle file, or `--version`). It needs `numpy` and `Pillow`.
