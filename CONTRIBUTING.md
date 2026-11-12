# Contributing

- Branch from `dev` using a descriptive name, and open pull requests into `dev`. Do not commit directly to `main`; it is the release branch.
- In the PR, describe the change and the milestone it serves, and provide verification evidence. Include screenshots for UI changes.
- Run `./gradlew assembleDebug lintDebug testDebugUnitTest :core:test` first. CI runs the same.
- Follow the server's [API contract](https://github.com/Aulteran/DriveLoom/blob/dev/docs/CONTRACTS.md). If the client needs something the contract lacks, change the server contract first.
- Keep signing keys, `local.properties` and other local files out of commits.
