- I want to upgrade versions in #file:libs.versions.toml to the latest ones that work for our project. THe way I want to do this is to do a couple of interrelated dependencies at a time, run our lint-build-test loop, ensure everything passes and if it looks good, commit that and keep going till everything is upgraded to the latest.

Can you help me do this?
- reevaluate detekt config to remove the config and just use the default config
- clean up the unused version code since we have separate version codes in for alpha and production apps in #build.gradle.kts
- fix security codeql issues
- update the dependencies to the latest versions
- I would like to scour our entire codebase and figure out which of the canister modules in #file:rust-agent-uniffi  are not used and not being called by our UI code and are redundant there and remove them. Can you help me figure this out?

Verify if the sonatype repository is still being used for any dependencies and if not, remove it from the build files.
- Remove Roboelectric and corresponding tests from the codebase. We will either test logic via unit tests or test end to end flows via our e2e test pipeline

- AndroidE2eTest: Maestro ran successfully but Kafka found 0 events

Not really. The app crashed. Can you confirm that the maestro test ran successfully on Android? I saw visually that it only scrolled to the 2nd video on android

- How are we asserting whether the events from a particular Maestro test run is what is being asserted in the Kafka test? Is there some specific ID we are asserting against?