Pod::Spec.new do |spec|
    spec.name                     = 'analytics'
    spec.version                  = '1.0'
    spec.homepage                 = 'https://github.com/dolr-ai/yral-mobile'
    spec.source                   = { :http=> ''}
    spec.authors                  = ''
    spec.license                  = ''
    spec.summary                  = 'Analytics module with Firebase and Mixpanel'
    spec.vendored_frameworks      = 'build/cocoapods/framework/analytics.framework'
    spec.libraries                = 'c++'
    spec.ios.deployment_target    = '15.6'
    spec.dependency 'FBSDKCoreKit', '18.0.0'
    spec.dependency 'Mixpanel', '5.0.8'
                
    if !Dir.exist?('build/cocoapods/framework/analytics.framework') || Dir.empty?('build/cocoapods/framework/analytics.framework')
        raise "

        Kotlin framework 'analytics' doesn't exist yet, so a proper Xcode project can't be generated.
        'pod install' should be executed after running ':generateDummyFramework' Gradle task:

            ./gradlew :shared:libs:analytics:generateDummyFramework

        Alternatively, proper pod installation is performed during Gradle sync in the IDE (if Podfile location is set)"
    end
                
    spec.xcconfig = {
        'ENABLE_USER_SCRIPT_SANDBOXING' => 'NO',
    }
                
    spec.pod_target_xcconfig = {
        'KOTLIN_PROJECT_PATH' => ':shared:libs:analytics',
        'PRODUCT_MODULE_NAME' => 'analytics',
    }
                
    spec.script_phases = [
        {
            :name => 'Build analytics',
            :execution_position => :before_compile,
            :shell_path => '/bin/sh',
            :script => <<-SCRIPT
                if [ "YES" = "$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED" ]; then
                  echo "Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED environment variable set to \"YES\""
                  exit 0
                fi
                set -ev
                REPO_ROOT="$PODS_TARGET_SRCROOT"
                "$REPO_ROOT/../../../gradlew" -p "$REPO_ROOT" $KOTLIN_PROJECT_PATH:syncFramework \
                    -Pkotlin.native.cocoapods.platform=$PLATFORM_NAME \
                    -Pkotlin.native.cocoapods.archs="$ARCHS" \
                    -Pkotlin.native.cocoapods.configuration="$CONFIGURATION"
            SCRIPT
        }
    ]
                
end