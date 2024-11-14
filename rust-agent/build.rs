const XCODE_CONFIGURATION_ENV: &'static str = "CONFIGURATION";

fn main() {
    let out_dir = "../iosApp/Generated";

    let bridges: Vec<&str> = vec![
        "src/lib.rs",
        "src/individual_user_template/individual_user_template_ffi.rs",
        "src/platform_orchestrator/platform_orchestrator_ffi.rs",
        "src/post_cache/post_cache_ffi.rs"
    ];
    for path in &bridges {
        println!("cargo:rerun-if-changed={}", path);
    }
    println!("cargo:rerun-if-env-changed={}", XCODE_CONFIGURATION_ENV);

    swift_bridge_build::parse_bridges(bridges)
        .write_all_concatenated(out_dir, env!("CARGO_PKG_NAME"));
}
