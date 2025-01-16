use std::str::FromStr;

use crate::{individual_user_template::*, Err};
use candid::Nat;
use candid::{self, ser, CandidType, Decode, Deserialize, Encode, Principal};
use ic_agent::export::PrincipalError;
use ic_agent::identity::DelegatedIdentity;
use ic_agent::identity::Secp256k1Identity;
use ic_agent::AgentError;
use k256::elliptic_curve::JwkEcKey;
use serde_bytes::ByteBuf;
use yral_canisters_common::Canisters;
use yral_types::delegated_identity::DelegatedIdentityWire;

pub type Secp256k1Error = k256::elliptic_curve::Error;

pub fn get_secp256k1_identity(
    jwk_key: JwkEcKey,
) -> std::result::Result<Secp256k1Identity, Secp256k1Error> {
    match k256::SecretKey::from_jwk(&jwk_key) {
        Ok(key) => Ok(Secp256k1Identity::from_private_key(key)),
        Err(error) => Err(error),
    }
}

pub fn get_jwk_ec_key(json_string: String) -> std::result::Result<JwkEcKey, Secp256k1Error> {
    return JwkEcKey::from_str(&json_string);
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

pub fn delegated_identity_wire_from_bytes(
    data: &[u8],
) -> std::result::Result<DelegatedIdentityWire, String> {
    return DelegatedIdentityWire::from_bytes(data).map_err(|e| e.to_string());
}

pub fn delegated_identity_from_bytes(
    data: &[u8],
) -> std::result::Result<ic_agent::identity::DelegatedIdentity, String> {
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

impl CanistersWrapper {
    pub fn get_canister_principal( &self) -> Principal {
        return self.inner.user_canister();
    }
    
    pub fn get_canister_principal_string(&self) -> String {
        return self.inner.user_canister().to_string();
    }
    
    pub fn get_user_principal(&self) -> Principal {
        return self.inner.user_principal();
    }    
}

pub async fn authenticate_with_network(
    auth: DelegatedIdentityWire,
    referrer: Option<Principal>,
) -> std::result::Result<CanistersWrapper, String> {
    let canisters: Canisters<true> = Canisters::<true>::authenticate_with_network(auth, referrer)
        .await
        .map_err(|error| error.to_string())?;
    Ok(CanistersWrapper { inner: canisters })
}

pub fn extract_time_as_double(result: Result11) -> Option<u64> {
    match result {
        Result11::Ok(system_time) => Some(system_time.secs_since_epoch),
        Result11::Err(_) => None,
    }
}

pub fn get_principal(text: String) -> std::result::Result<Principal, PrincipalError> {
    Principal::from_text(text)
}
