use std::env;
use std::fs;
use std::path::Path;
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

    // swift-bridge generates `convenience init` bodies using a closure pattern that
    // is rejected by Swift 6+: `try { ... return T(ptr:) }()` never calls self.init.
    // Post-process the generated file to replace these with a valid self.init/throw form.
    let generated_swift = Path::new(out_dir)
        .join(env!("CARGO_PKG_NAME"))
        .join(format!("{}.swift", env!("CARGO_PKG_NAME")));
    if let Ok(source) = fs::read_to_string(&generated_swift) {
        let fixed = fix_swift6_convenience_inits(&source);
        if fixed != source {
            fs::write(&generated_swift, fixed)
                .expect("Failed to write patched Swift bindings");
            println!("cargo:warning=Patched swift-bridge generated file for Swift 6 compatibility");
        }
    }
}

/// swift-bridge generates `convenience init` bodies using a closure pattern that
/// is rejected by Swift 6+: `try { ... return T(ptr:) }()` never calls self.init.
/// The broken pattern always appears on a single line. Process line by line and
/// replace it with a valid self.init / throw form.
fn fix_swift6_convenience_inits(source: &str) -> String {
    let mut result = String::with_capacity(source.len() + 256);
    let mut changed = false;
    let mut in_convenience_init = false;

    for line in source.lines() {
        let trimmed = line.trim();

        // Track entry into a convenience init body
        if trimmed.contains("convenience init") && trimmed.ends_with('{') {
            in_convenience_init = true;
        }
        // Track exit: closing brace at the function-body indentation level (4 spaces)
        if in_convenience_init && trimmed == "}" {
            in_convenience_init = false;
        }

        // Pattern (single line):
        //   <indent>try { let val = CALL; if val.is_ok { return TYPE(args) } else { throw ETYPE(args) } }()
        if trimmed.starts_with("try { let val = ")
            && trimmed.contains("if val.is_ok { return ")
            && trimmed.contains("} else { throw ")
            && trimmed.ends_with("}()")
        {
            let indent = &line[..line.len() - trimmed.len()];
            if let Some(inner) = trimmed.strip_prefix("try { ").and_then(|s| s.strip_suffix("}()")) {
                let inner = inner.trim_end_matches('}').trim_end();
                if let Some((let_part, rest)) = inner.split_once("; if val.is_ok { return ") {
                    if let Some((return_expr, rest2)) = rest.split_once(" } else { throw ") {
                        let throw_expr = rest2.trim_end_matches('}').trim();
                        if let Some(args_start) = return_expr.find('(') {
                            let args = &return_expr[args_start..];
                            // Inside a convenience init: call self.init; elsewhere: return T(...)
                            let ok_branch = if in_convenience_init {
                                format!("self.init{args}")
                            } else {
                                format!("return {return_expr}")
                            };
                            changed = true;
                            result.push_str(&format!(
                                "{indent}{let_part}\n{indent}if val.is_ok {{\n{indent}    {ok_branch}\n{indent}}} else {{\n{indent}    throw {throw_expr}\n{indent}}}\n"
                            ));
                            continue;
                        }
                    }
                }
            }
        }

        result.push_str(line);
        result.push('\n');
    }

    if changed { result } else { source.to_string() }
}
