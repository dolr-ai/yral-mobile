use std::str::FromStr;

use crate::individual_user_template::*;
use crate::uni_ffi_helpers::*;
use candid::Nat;
use candid::{self, ser, CandidType, Decode, Deserialize, Encode, Principal};
use ic_agent::export::PrincipalError;
use ic_agent::identity::DelegatedIdentity;
use ic_agent::identity::Delegation;
use ic_agent::identity::Secp256k1Identity;
use ic_agent::identity::SignedDelegation;
use ic_agent::AgentError;
use ic_agent::Identity;
use k256::elliptic_curve::sec1::ToEncodedPoint;
use k256::elliptic_curve::JwkEcKey;
use k256::Secp256k1;
use serde_bytes::ByteBuf;
use std::time::UNIX_EPOCH;
use tokio::time::Duration;
use yral_canisters_common::utils::profile::propic_from_principal as inner_propic_from_principal;
use yral_canisters_common::Canisters;
use yral_metadata_client::DeviceRegistrationToken;
use yral_metadata_client::MetadataClient;
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
    JwkEcKey::from_str(&json_string)
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
    DelegatedIdentityWire::from_bytes(data).map_err(|e| e.to_string())
}

pub fn delegated_identity_from_bytes(
    data: &[u8],
) -> std::result::Result<DelegatedIdentity, String> {
    let wire = DelegatedIdentityWire::from_bytes(data)?;
    let to_secret = k256::SecretKey::from_jwk(&wire.to_secret)
        .map_err(|e| format!("Failed to parse secret key: {:?}", e))?;
    let to_identity = ic_agent::identity::Secp256k1Identity::from_private_key(to_secret);
    let delegated_identity = ic_agent::identity::DelegatedIdentity::new(
        wire.from_key,
        Box::new(to_identity),
        wire.delegation_chain,
    ).map_err(|e| format!("Failed to create delegated identity: {:?}", e))?;
    Ok(delegated_identity)
}

pub fn delegate_identity_with_max_age_public(
    parent_wire: DelegatedIdentityWire,
    new_pub_jwk_json: Vec<u8>,
    max_age_seconds: u64,
) -> std::result::Result<DelegatedIdentityWire, String> {
    let new_jwk: JwkEcKey = serde_json::from_slice(&new_pub_jwk_json)
        .map_err(|e| format!("Failed to parse new JWK JSON: {e}"))?;

    let to_identity =
        Secp256k1Identity::from_private_key(new_jwk.to_secret_key().map_err(|e| e.to_string())?);

    let existing_delegated =
        DelegatedIdentity::try_from(parent_wire.clone()).map_err(|e| e.to_string())?;

    let now = std::time::SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map_err(|_| "System clock error (before UNIX epoch)".to_string())?;
    let expiry = now + Duration::from_secs(max_age_seconds);
    let expiry_ns = expiry.as_nanos() as u64;

    let delegation = Delegation {
        pubkey: to_identity.public_key().unwrap(),
        expiration: expiry_ns,
        targets: None,
    };

    let signed_delegation_signature = existing_delegated
        .sign_delegation(&delegation)
        .map_err(|e| format!("Failed to sign delegation: {e:?}"))?;

    let signed_delegation = SignedDelegation {
        delegation,
        signature: signed_delegation_signature.signature.unwrap(),
    };

    let mut new_chain = existing_delegated.delegation_chain();

    new_chain.push(signed_delegation);

    Ok(DelegatedIdentityWire {
        from_key: signed_delegation_signature.public_key.unwrap(),
        to_secret: new_jwk,
        delegation_chain: new_chain,
    })
}

#[uniffi::export]
pub fn delegated_identity_wire_to_json(data: &[u8]) -> String {
    let wire = delegated_identity_wire_from_bytes(data).unwrap();
    serde_json::to_string(&wire).unwrap()
}

#[derive(uniffi::Object)]
pub struct CanistersWrapper {
    inner: Canisters<true>,
}

#[uniffi::export]
impl CanistersWrapper {
    pub fn get_canister_principal(&self) -> Principal {
        self.inner.user_canister()
    }

    pub fn get_canister_principal_string(&self) -> String {
        self.inner.user_canister().to_string()
    }

    pub fn get_user_principal(&self) -> Principal {
        self.inner.user_principal()
    }

    pub fn get_user_principal_string(&self) -> String {
        self.inner.user_principal().to_string()
    }
}

#[uniffi::export]
pub async fn authenticate_with_network(
    auth_data: Vec<u8>,
    referrer: Option<Principal>,
) -> std::result::Result<CanistersWrapper, FFIError> {
    RUNTIME.spawn(async move {
        let auth = delegated_identity_wire_from_bytes(&auth_data)
            .map_err(|e| FFIError::UnknownError(format!("Invalid: {:?}", e)))?;
        let canisters: Canisters<true> = Canisters::<true>::authenticate_with_network(auth, referrer)
            .await
            .map_err(|e| FFIError::AgentError(format!("Invalid: {:?}", e)))?;
        Ok(CanistersWrapper { inner: canisters })
    }).await.map_err(|e| FFIError::AgentError(format!("{:?}", e)))?
}

pub fn extract_time_as_double(result: Result11) -> Option<u64> {
    match result {
        Result11::Ok(system_time) => Some(system_time.secs_since_epoch),
        Result11::Err(_) => None,
    }
}

pub fn principal_to_string(principal: &Principal) -> String {
    principal.to_string()
}

pub fn get_principal(text: String) -> std::result::Result<Principal, PrincipalError> {
    Principal::from_text(text)
}

impl Result12 {
    pub fn is_ok(&self) -> bool {
        matches!(self, Result12::Ok(_))
    }

    pub fn is_err(&self) -> bool {
        matches!(self, Result12::Err(_))
    }

    pub fn ok_value(self) -> Option<Vec<PostDetailsForFrontend>> {
        match self {
            Result12::Ok(val) => Some(val),
            Result12::Err(_) => None,
        }
    }

    pub fn err_value(self) -> Option<GetPostsOfUserProfileError> {
        match self {
            Result12::Ok(_) => None,
            Result12::Err(err) => Some(err),
        }
    }
}

impl GetPostsOfUserProfileError {
    pub fn is_reached_end_of_items_list(&self) -> bool {
        matches!(self, GetPostsOfUserProfileError::ReachedEndOfItemsList)
    }

    pub fn is_invalid_bounds_passed(&self) -> bool {
        matches!(self, GetPostsOfUserProfileError::InvalidBoundsPassed)
    }

    pub fn is_exceeded_max_number_of_items_allowed_in_one_request(&self) -> bool {
        matches!(self, GetPostsOfUserProfileError::ExceededMaxNumberOfItemsAllowedInOneRequest)
    }
}

#[uniffi::export]
fn propic_from_principal(principal: Principal) -> String {
    inner_propic_from_principal(principal)
}

#[uniffi::export]
fn yral_auth_login_hint(data: &[u8]) -> std::result::Result<String, FFIError> {
    let identity = delegated_identity_from_bytes(data)
        .map_err(|e| FFIError::UnknownError(format!("Failed to parse identity: {:?}", e)))?;
    match yral_canisters_common::yral_auth_login_hint(&identity) {
        Ok(signature) => Ok(signature),
        Err(error) => Err(FFIError::UnknownError(format!(
            "Failed to create login hint: {:?}",
            error
        ))),
    }
}

#[uniffi::export]
pub async fn register_device(
    data: Vec<u8>,
    token: String,
) -> std::result::Result<(), FFIError> {
    RUNTIME.spawn(async move {
        let identity = delegated_identity_from_bytes(&data)
            .map_err(|e| FFIError::UnknownError(format!("Failed to parse identity: {:?}", e)))?;
        let client: MetadataClient<false> = MetadataClient::default();
        let registration_token = DeviceRegistrationToken { token };
        let res  = 
            client
                .register_device(&identity, registration_token)
                .await
                .map_err(|e| FFIError::AgentError(format!("Api Error: {:?}", e)))?;    
        Ok(res)
    }).await.map_err(|e| FFIError::AgentError(format!("{:?}", e)))?
}

#[uniffi::export]
pub async fn unregister_device(
    data: Vec<u8>,
    token: String,
) -> std::result::Result<(), FFIError> {
    RUNTIME.spawn(async move {
        let identity = delegated_identity_from_bytes(&data)
            .map_err(|e| FFIError::UnknownError(format!("Failed to parse identity: {:?}", e)))?;
        let client: MetadataClient<false> = MetadataClient::default();
        let registration_token = DeviceRegistrationToken { token };
        let res  = 
            client
                .unregister_device(&identity, registration_token)
                .await
                .map_err(|e| FFIError::AgentError(format!("Api Error: {:?}", e)))?;    
        Ok(res)
    }).await.map_err(|e| FFIError::AgentError(format!("{:?}", e)))?
}
