Pod::Spec.new do |spec|
    spec.name                     = 'iosSharedUmbrella'
    spec.version                  = '1.0'
    spec.homepage                 = 'https://github.com/dolr-ai/yral-mobile'
    spec.source                   = { :http=> ''}
    spec.authors                  = ''
    spec.license                  = 'MIT'
    spec.summary                  = 'Umbrella framework for shared KMM code'
    spec.vendored_frameworks      = 'build/cocoapods/framework/iosSharedUmbrella.framework'
    spec.libraries                = 'c++'
    spec.ios.deployment_target    = '15.6'
    spec.dependency 'FBSDKCoreKit', '18.0.0'
    spec.dependency 'FirebaseAnalytics', '11.14.0'
    spec.dependency 'FirebaseAppCheck', '11.14.0'
    spec.dependency 'FirebaseAuth'
    spec.dependency 'FirebaseCore', '11.14.0'
    spec.dependency 'FirebaseCoreInternal', '11.14.0'
    spec.dependency 'FirebaseCrashlytics', '11.14.0'
    spec.dependency 'FirebaseFirestore', '11.14.0'
    spec.dependency 'FirebaseInstallations', '11.14.0'
    spec.dependency 'FirebaseMessaging', '11.14.0'
    spec.dependency 'FirebasePerformance', '11.14.0'
    spec.dependency 'FirebaseStorage', '11.14.0'
    spec.dependency 'GoogleUtilities', '8.1'
    spec.dependency 'Mixpanel', '5.0.8'
    spec.dependency 'nanopb', '3.30910.0'
                
    if !Dir.exist?('build/cocoapods/framework/iosSharedUmbrella.framework') || Dir.empty?('build/cocoapods/framework/iosSharedUmbrella.framework')
        raise "

        Kotlin framework 'iosSharedUmbrella' doesn't exist yet, so a proper Xcode project can't be generated.
        'pod install' should be executed after running ':generateDummyFramework' Gradle task:

            ./gradlew :iosSharedUmbrella:generateDummyFramework

        Alternatively, proper pod installation is performed during Gradle sync in the IDE (if Podfile location is set)"
    end
                
    spec.xcconfig = {
        'ENABLE_USER_SCRIPT_SANDBOXING' => 'NO',
    }
                
    spec.pod_target_xcconfig = {
        'KOTLIN_PROJECT_PATH' => ':iosSharedUmbrella',
        'PRODUCT_MODULE_NAME' => 'iosSharedUmbrella',
    }
                
    spec.script_phases = [
        {
            :name => 'Build iosSharedUmbrella',
            :execution_position => :before_compile,
            :shell_path => '/bin/sh',
            :script => <<-SCRIPT
                if [ "YES" = "$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED" ]; then
                  echo "Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED environment variable set to \"YES\""
                  exit 0
                fi
                set -ev
                REPO_ROOT="$PODS_TARGET_SRCROOT"
                "$REPO_ROOT/../gradlew" -p "$REPO_ROOT" $KOTLIN_PROJECT_PATH:syncFramework \
                    -Pkotlin.native.cocoapods.platform=$PLATFORM_NAME \
                    -Pkotlin.native.cocoapods.archs="$ARCHS" \
                    -Pkotlin.native.cocoapods.configuration="$CONFIGURATION"
            SCRIPT
        }
    ]
                
end