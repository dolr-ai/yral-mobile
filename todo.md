- I want to upgrade versions in #file:libs.versions.toml to the latest ones that work for our project. THe way I want to do this is to do a couple of interrelated dependencies at a time, run our lint-build-test loop, ensure everything passes and if it looks good, commit that and keep going till everything is upgraded to the latest.

Can you help me do this?
- reevaluate detekt config to remove the config and just use the default config
- clean up the unused version code since we have separate version codes in for alpha and production apps in #build.gradle.kts
- fix security codeql issues
- update the dependencies to the latest versions
- Remove Roboelectric and corresponding tests from the codebase. We will either test logic via unit tests or test end to end flows via our e2e test pipeline
- Use version catalogs with bundles where applicable
- Use build cache and configuration cache
```java
org.gradle.caching=true
org.gradle.configuration-cache=true
```
- Parallelize the build by enabling parallel execution in gradle properties
```
./gradlew --parallel // if on gradle 8.11 or later, this is default
org.gradle.configuration-cache.parallel=true
```
- Remove all references to yral-common from mobile codebase
- lint and static analysis
```
./gradlew lintKotlin
./gradlew detekt
```
- code coverage, look into `kotlinx-kover` for this
- for dependency management, it's better to use renovate than dependabot.
- code shrinking, r8
```
android {
    buildTypes {
        release {
            minifyEnabled true
        }
    }
}
```
- Look at `@Keep` if the above messes with crashlytics
- Remove the checks limitation where we are suppressing logs to github and just use the plain jane mechanism of displaying to github actions logs
- break up ci to separate ios and android steps. ci takes too long