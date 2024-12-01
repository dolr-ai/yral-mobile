use crate::sns_swap::*;
use ic_agent::AgentError;

#[swift_bridge::bridge]
mod ffi {
    extern "Rust" {
        #[swift_bridge(already_declared)]
        type NeuronBasketConstructionParameters;
        #[swift_bridge(already_declared)]
        type LinearScalingCoefficient;
        #[swift_bridge(already_declared)]
        type IdealMatchedParticipationFunction;
        #[swift_bridge(already_declared)]
        type NeuronsFundParticipationConstraints;
        #[swift_bridge(already_declared)]
        type Countries;
        type ErrorRefundIcpRequest;
        #[swift_bridge(already_declared)]
        type SNSSwapResult_;
        type ErrorRefundIcpResponse;
        type FinalizeSwapArg;
        #[swift_bridge(already_declared)]
        type CanisterCallError;
        #[swift_bridge(already_declared)]
        type FailedUpdate;
        #[swift_bridge(already_declared)]
        type SetDappControllersResponse;
        type Possibility;
        type SetDappControllersCallResult;
        type SweepResult;
        #[swift_bridge(already_declared)]
        type GovernanceError;
        type Response;
        type Possibility1;
        type SettleCommunityFundParticipationResult;
        type Possibility2;
        type SettleNeuronsFundParticipationResult;
        type Possibility3;
        type SetModeCallResult;
        type FinalizeSwapResponse;
        type GetAutoFinalizationStatusArg;
        type GetAutoFinalizationStatusResponse;
        type GetBuyerStateRequest;
        type TransferableAmount;
        type BuyerState;
        type GetBuyerStateResponse;
        type GetBuyersTotalArg;
        type GetBuyersTotalResponse;
        type GetCanisterStatusArg;
        #[swift_bridge(already_declared)]
        type CanisterStatusType;
        #[swift_bridge(already_declared)]
        type DefiniteCanisterSettingsArgs;
        #[swift_bridge(already_declared)]
        type CanisterStatusResultV2;
        type GetDerivedStateArg;
        type GetDerivedStateResponse;
        type GetInitArg;
        type GetInitResponse;
        type GetLifecycleArg;
        type GetLifecycleResponse;
        type GetOpenTicketArg;
        type Icrc1Account;
        type Ticket;
        type Ok2;
        type Err1;
        type SNSSwapResult1;
        type GetOpenTicketResponse;
        type GetSaleParametersArg;
        type Params;
        type GetSaleParametersResponse;
        type GetStateArg;
        #[swift_bridge(already_declared)]
        type NeuronId;
        type NeuronAttributes;
        type Principals;
        type CfInvestment;
        type DirectInvestment;
        type Investor;
        type SnsNeuronRecipe;
        #[swift_bridge(already_declared)]
        type CfNeuron;
        #[swift_bridge(already_declared)]
        type CfParticipant;
        type Swap;
        type DerivedState;
        type GetStateResponse;
        type ListCommunityFundParticipantsRequest;
        type ListCommunityFundParticipantsResponse;
        type ListDirectParticipantsRequest;
        type Participant;
        type ListDirectParticipantsResponse;
        type ListSnsNeuronRecipesRequest;
        type ListSnsNeuronRecipesResponse;
        type NewSaleTicketRequest;
        type InvalidUserAmount;
        type Err2;
        type SNSSwapResult2;
        type NewSaleTicketResponse;
        type NotifyPaymentFailureArg;
        type RefreshBuyerTokensRequest;
        type RefreshBuyerTokensResponse;
        #[swift_bridge(already_declared)]
        type AgentError;
        #[swift_bridge(already_declared)]
        type PrincipalError;
    }

    extern "Rust" {
        #[swift_bridge(already_declared)]
        type Service;
        async fn error_refund_icp(
            &self,
            arg0: ErrorRefundIcpRequest,
        ) -> Result<ErrorRefundIcpResponse, AgentError>;
        async fn finalize_swap(
            &self,
            arg0: FinalizeSwapArg,
        ) -> Result<FinalizeSwapResponse, AgentError>;
        async fn get_auto_finalization_status(
            &self,
            arg0: GetAutoFinalizationStatusArg,
        ) -> Result<GetAutoFinalizationStatusResponse, AgentError>;
        async fn get_buyer_state(
            &self,
            arg0: GetBuyerStateRequest,
        ) -> Result<GetBuyerStateResponse, AgentError>;
        async fn get_buyers_total(
            &self,
            arg0: GetBuyersTotalArg,
        ) -> Result<GetBuyersTotalResponse, AgentError>;
        async fn get_canister_status(
            &self,
            arg0: GetCanisterStatusArg,
        ) -> Result<CanisterStatusResultV2, AgentError>;
        async fn get_derived_state(
            &self,
            arg0: GetDerivedStateArg,
        ) -> Result<GetDerivedStateResponse, AgentError>;
        async fn get_init(&self, arg0: GetInitArg) -> Result<GetInitResponse, AgentError>;
        async fn get_lifecycle(
            &self,
            arg0: GetLifecycleArg,
        ) -> Result<GetLifecycleResponse, AgentError>;
        async fn get_open_ticket(
            &self,
            arg0: GetOpenTicketArg,
        ) -> Result<GetOpenTicketResponse, AgentError>;
        async fn get_sale_parameters(
            &self,
            arg0: GetSaleParametersArg,
        ) -> Result<GetSaleParametersResponse, AgentError>;
        async fn get_state(&self, arg0: GetStateArg) -> Result<GetStateResponse, AgentError>;
        async fn list_community_fund_participants(
            &self,
            arg0: ListCommunityFundParticipantsRequest,
        ) -> Result<ListCommunityFundParticipantsResponse, AgentError>;
        async fn list_direct_participants(
            &self,
            arg0: ListDirectParticipantsRequest,
        ) -> Result<ListDirectParticipantsResponse, AgentError>;
        async fn list_sns_neuron_recipes(
            &self,
            arg0: ListSnsNeuronRecipesRequest,
        ) -> Result<ListSnsNeuronRecipesResponse, AgentError>;
        async fn new_sale_ticket(
            &self,
            arg0: NewSaleTicketRequest,
        ) -> Result<NewSaleTicketResponse, AgentError>;
        async fn notify_payment_failure(
            &self,
            arg0: NotifyPaymentFailureArg,
        ) -> Result<Ok2, AgentError>;
        async fn refresh_buyer_tokens(
            &self,
            arg0: RefreshBuyerTokensRequest,
        ) -> Result<RefreshBuyerTokensResponse, AgentError>;

    }
}
