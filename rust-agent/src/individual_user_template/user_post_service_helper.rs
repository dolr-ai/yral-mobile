use candid::{Principal, CandidType, Deserialize};
use ic_agent::export::PrincipalError;
use ic_agent::Agent;
use yral_canisters_client::user_post_service::UserPostServiceError;
use std::sync::Arc;
use crate::individual_user_template::individual_user_template_helper::*;
use crate::RUNTIME;
use yral_canisters_client::user_post_service::Result1;
use yral_canisters_client::user_post_service::Post;
use yral_canisters_client::user_post_service::PostStatus;
use std::time::SystemTime;
use ic_agent::identity::DelegatedIdentity;


pub struct ServiceCanistersDetails {
    pub user_post_cansiter_id: Principal,
    pub user_info_canister_id: Principal,
}

impl ServiceCanistersDetails {
    pub fn new() -> ServiceCanistersDetails {
        ServiceCanistersDetails {
            user_post_cansiter_id: yral_canisters_client::ic::USER_POST_SERVICE_ID,
            user_info_canister_id: yral_canisters_client::ic::USER_INFO_SERVICE_ID,
        }
    }

    pub fn get_user_post_service_canister_id(&self) -> Principal {
        self.user_post_cansiter_id
    }

    pub fn get_user_info_service_canister_id(&self) -> Principal {
        self.user_info_canister_id
    }
}

pub struct UserPostService {
    pub principal: Principal,
    pub agent: Arc<Agent>,
}

impl UserPostService {
    pub fn new(
        principal: Principal,
        identity: DelegatedIdentity,
    ) -> std::result::Result<UserPostService, PrincipalError> {
        let agent = Agent::builder()
            .with_url("https://ic0.app/")
            .with_identity(identity)
            .build()
            .expect("Failed to create agent");

        Ok(UserPostService {
            principal,
            agent: Arc::new(agent),
        })
    }

    pub async fn get_individual_post_details_by_id(
        &self,
        arg0: String,
    ) -> Result<Result1, String> {

        let agent = Arc::clone(&self.agent);
        let service = yral_canisters_client::user_post_service::UserPostService(
            yral_canisters_client::ic::USER_POST_SERVICE_ID,
            &agent,
        );
        service
            .get_individual_post_details_by_id(arg0)
            .await
            .map(|op: Result1| op)
            .map_err(|e| e.to_string())
    }

    pub async fn get_posts_of_this_user_profile_with_pagination_cursor(
        &self,
        arg0: Principal,
        arg1: u64,
        arg2: u64,
    ) -> Result<Vec<Post>, String> {
        let agent = Arc::clone(&self.agent);
        let service = yral_canisters_client::user_post_service::UserPostService(
            yral_canisters_client::ic::USER_POST_SERVICE_ID,
            &agent,
        );
        let details: Vec<Post> = service
            .get_posts_of_this_user_profile_with_pagination_cursor(arg0, arg1, arg2)
            .await
            .map_err(|e| e.to_string())?;
        
        Ok(details)
    }
}