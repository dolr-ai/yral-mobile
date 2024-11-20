use std::str::FromStr;

use crate::individual_user_template::*;
use candid::{self, ser, CandidType, Decode, Deserialize, Encode, Principal};
use ic_agent::export::PrincipalError;
use ic_agent::AgentError;
use serde_bytes::ByteBuf;
use candid::Nat;
use k256::elliptic_curve::JwkEcKey;
use ic_agent::identity::Secp256k1Identity;
use yral_types::delegated_identity::DelegatedIdentityWire;
use ic_agent::identity::DelegatedIdentity;
use yral_canisters_common::Canisters;

pub fn get_secp256k1_identity(jwk_key: JwkEcKey) -> Option<Secp256k1Identity> {
    match k256::SecretKey::from_jwk(&jwk_key) {
        Ok(key) => Some(Secp256k1Identity::from_private_key(key)),
        Err(_) => None,
    }
}

pub fn get_jwk_ec_key(json_string: String) -> Option<JwkEcKey> {
    let jwk_ec_key = JwkEcKey::from_str(&json_string);
    match jwk_ec_key {
        Ok(key) => Some(key),
        Err(_) => None,
    }    
}

pub trait FromBytes {
    fn from_bytes(data: &[u8]) -> std::result::Result<Self, String>
    where 
        Self: Sized;
}

impl FromBytes for DelegatedIdentityWire {
    fn from_bytes(data: &[u8]) -> std::result::Result<Self, String> {
        serde_json::from_slice(data).map_err(|e| e.to_string())
    }
}

pub fn delegated_identity_wire_from_bytes(data: &[u8]) -> std::result::Result<DelegatedIdentityWire, String> {
    return DelegatedIdentityWire::from_bytes(data).map_err( |e| e.to_string());
}

pub fn delegated_identity_from_bytes(data: &[u8]) -> std::result::Result<ic_agent::identity::DelegatedIdentity, String> {
    let wire = DelegatedIdentityWire::from_bytes(data)?;
    let to_secret = k256::SecretKey::from_jwk(&wire.to_secret)
        .map_err(|e| format!("Failed to parse secret key: {:?}", e))?;
    let to_identity = ic_agent::identity::Secp256k1Identity::from_private_key(to_secret);
    let delegated_identity = ic_agent::identity::DelegatedIdentity::new(
        wire.from_key,
        Box::new(to_identity),
        wire.delegation_chain,
    );
    Ok(delegated_identity)
}

pub struct CanistersWrapper {
    inner: Canisters<true>,
}

pub async fn authenticate_with_network(
    auth: DelegatedIdentityWire,
    referrer: Option<Principal>,
) -> std::result::Result<CanistersWrapper, String> {
    let canisters: Canisters<true> = Canisters::<true>::authenticate_with_network(auth, referrer).await.map_err( |error| error.to_string())?;
    Ok(CanistersWrapper { inner: canisters })
}

pub fn get_canister_principal(wrapper: CanistersWrapper) -> Principal {
    return wrapper.inner.user_canister()
}

pub fn get_user_principal(wrapper: CanistersWrapper) -> Principal {
    return wrapper.inner.user_principal()
}

pub fn extract_time_as_double(result: Result11) -> Option<u64> {
    match result {
        Result11::Ok(system_time) => Some(system_time.secs_since_epoch),
        Result11::Err(_) => None,
    }
}
