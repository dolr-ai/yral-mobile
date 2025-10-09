use crate::UniffiCustomTypeConverter;
use candid::Error as CandidError;
use candid::{CandidType, Deserialize, Nat, Principal};
use cfg_if::cfg_if;
use ic_agent::export::PrincipalError;
use ic_agent::AgentError;
use serde_bytes::ByteBuf;
use std::str::FromStr;
use uniffi::{Enum, Record};

uniffi::custom_type!(Principal, String);
impl UniffiCustomTypeConverter for Principal {
    type Builtin = String;

    fn into_custom(val: Self::Builtin) -> uniffi::Result<Self> {
        Ok(Principal::from_text(val)?)
    }

    fn from_custom(obj: Self) -> Self::Builtin {
        obj.to_text()
    }
}

uniffi::custom_type!(Nat, String);
impl UniffiCustomTypeConverter for Nat {
    type Builtin = String;
    fn into_custom(val: Self::Builtin) -> uniffi::Result<Self> {
        Ok(Nat::from_str(&val)?)
    }

    fn from_custom(obj: Self) -> Self::Builtin {
        obj.0.to_string()
    }
}

uniffi::custom_type!(ByteBuf, Vec<u8>);
impl UniffiCustomTypeConverter for ByteBuf {
    type Builtin = Vec<u8>;
    fn into_custom(val: Self::Builtin) -> uniffi::Result<Self> {
        Ok(ByteBuf::from(val))
    }

    fn from_custom(obj: Self) -> Self::Builtin {
        obj.into_vec()
    }
}

#[derive(CandidType, Deserialize, Record)]
pub struct IntBytePair {
    pub first: u64,
    pub second: u8,
}

#[derive(CandidType, Deserialize, Record)]
pub struct IntDoublePair {
    pub first: u64,
    pub second: f64,
}

#[derive(CandidType, Deserialize, Record)]
pub struct IntPair {
    pub first: u64,
    pub second: u64,
}

#[derive(CandidType, Deserialize, Record)]
pub struct StringPair {
    pub first: String,
    pub second: String,
}

#[derive(Deserialize, CandidType, Record)]
pub struct KeyValuePair {
    pub key: String,
    pub value: String,
}

#[derive(CandidType, Deserialize, Record)]
pub struct PrincipalStringPair {
    pub first: Principal,
    pub second: String,
}

#[derive(Debug, Enum)]
pub enum FFIError {
    AgentError(String),
    CandidError(String),
    PrincipalError(String),
    UnknownError(String),
}

impl From<AgentError> for FFIError {
    fn from(err: AgentError) -> Self {
        FFIError::AgentError(err.to_string())
    }
}

impl From<CandidError> for FFIError {
    fn from(err: CandidError) -> Self {
        FFIError::CandidError(err.to_string())
    }
}

impl From<PrincipalError> for FFIError {
    fn from(err: PrincipalError) -> Self {
        FFIError::PrincipalError(err.to_string())
    }
}

impl std::fmt::Display for FFIError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            FFIError::AgentError(msg) => write!(f, "AgentError: {}", msg),
            FFIError::CandidError(msg) => write!(f, "CandidError: {}", msg),
            FFIError::PrincipalError(msg) => write!(f, "PrincipalError: {}", msg),
            FFIError::UnknownError(msg) => write!(f, "UnknownError: {}", msg),
        }
    }
}

cfg_if! {
    if #[cfg(target_os = "android")] {
        #[uniffi::export]
        pub fn init_rust_logger() {
            use android_logger::{Config, FilterBuilder};
            use log::{LevelFilter};
            android_logger::init_once(
                Config::default()
                    .with_max_level(LevelFilter::Trace) // limit log level
                    .with_tag("Rust Logger") // logs will show under mytag tag
                    .with_filter( // configure messages for specific crate
                          FilterBuilder::new()
                              .parse("debug,hello::crate=error")
                              .build())
            );
        }
    }
}
