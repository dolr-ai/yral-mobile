use candid::Error as CandidError;
use candid::{CandidType, Deserialize, Nat, Principal};
use ic_agent::export::PrincipalError;
use ic_agent::AgentError;
use serde_bytes::ByteBuf;
use std::str::FromStr;
use uniffi::{Enum, Record};

uniffi::custom_type!(Principal, String, {
    remote,
    try_lift: |val| Ok(Principal::from_text(val)?),
    lower: |obj| obj.to_text(),
});

uniffi::custom_type!(Nat, String, {
    remote,
    try_lift: |val| Ok(Nat::from_str(&val)?),
    lower: |obj| obj.0.to_string(),
});

uniffi::custom_type!(ByteBuf, Vec<u8>, {
    remote,
    try_lift: |val| Ok(ByteBuf::from(val)),
    lower: |obj| obj.into_vec(),
});

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
