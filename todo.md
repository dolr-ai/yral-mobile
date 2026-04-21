- I want to upgrade versions in #file:libs.versions.toml to the latest ones that work for our project. THe way I want to do this is to do a couple of interrelated dependencies at a time, run our lint-build-test loop, ensure everything passes and if it looks good, commit that and keep going till everything is upgraded to the latest.

Can you help me do this?
- reevaluate detekt config to remove the config and just use the default config
- clean up the unused version code since we have separate version codes in for alpha and production apps in #build.gradle.kts
- fix security codeql issues
- update the dependencies to the latest versions
- I would like to scour our entire codebase and figure out which of the canister modules in #file:rust-agent-uniffi  are not used and not being called by our UI code and are redundant there and remove them. Can you help me figure this out?

From conversations with other team members who have more context of this entire codebase, what I understand is, we are ideally only calling these 3 canisters, so the rest are not in use:
rate_limits
user_info_service
user_post_service

Can you help me figure out this entire situation?

Verify if the sonatype repository is still being used for any dependencies and if not, remove it from the build files.