use std::env;
const XCODE_CONFIGURATION_ENV: &'static str = "CONFIGURATION";

fn main() {
    let ci_env_var = env::var("CI").unwrap_or_else(|_| "false".to_string());
    if ci_env_var == "true" {
        // We are in CI, so do NOT generate Swift bridging files
        println!("cargo:warning=Skipping Swift bridging file generation because CI=true");
        return;
    }
    let out_dir = "../iosApp/Generated";

    let bridges: Vec<&str> = vec![
        "src/lib.rs",
        "src/individual_user_template/individual_user_template_ffi.rs",
        "src/ledger/ledger_ffi.rs"
    ];
    for path in &bridges {
        println!("cargo:rerun-if-changed={}", path);
    }
    println!("cargo:rerun-if-env-changed={}", XCODE_CONFIGURATION_ENV);

    swift_bridge_build::parse_bridges(bridges)
        .write_all_concatenated(out_dir, env!("CARGO_PKG_NAME"));
}
