use crate::sns_ledger::*;
use ic_agent::AgentError;

#[swift_bridge::bridge]
mod ffi {
    extern "Rust" {
        type ChangeArchiveOptions;
        type MetadataValue;
        #[swift_bridge(already_declared)]
        type Subaccount;
        #[swift_bridge(already_declared)]
        type Account;
        type ChangeFeeCollector;
        type FeatureFlags;
        type UpgradeArgs;
        type InitArgsArchiveOptions;
        #[swift_bridge(already_declared)]
        type InitArgs;
        type LedgerArg;
        type BlockIndex;
        type ArchiveInfo;
        type GetBlocksArgs;
        type Map;
        type Value;
        type Block;
        type BlockRange;
        type QueryBlockArchiveFn;
        type GetBlocksResponseArchivedBlocksItem;
        type GetBlocksResponse;
        type DataCertificate;
        type TxIndex;
        type GetTransactionsRequest;
        type Timestamp;
        #[swift_bridge(already_declared)]
        type Burn;
        #[swift_bridge(already_declared)]
        type Mint;
        #[swift_bridge(already_declared)]
        type Approve;
        #[swift_bridge(already_declared)]
        type Transfer;
        #[swift_bridge(already_declared)]
        type Transaction;
        type TransactionRange;
        type QueryArchiveFn;
        type GetTransactionsResponseArchivedTransactionsItem;
        type GetTransactionsResponse;
        #[swift_bridge(already_declared)]
        type Tokens;
        type StandardRecord;
        type TransferArg;
        #[swift_bridge(already_declared)]
        type TransferError;
        type TransferResult;
        type AllowanceArgs;
        type Allowance;
        type ApproveArgs;
        type ApproveError;
        type ApproveResult;
        type TransferFromArgs;
        type TransferFromError;
        type TransferFromResult;
        type GetArchivesArgs;
        type GetArchivesResultItem;
        type GetArchivesResult;
        type Icrc3Value;
        type GetBlocksResultBlocksItem;
        type GetBlocksResultArchivedBlocksItemCallback;
        type GetBlocksResultArchivedBlocksItem;
        type GetBlocksResult;
        type Icrc3DataCertificate;
        type Icrc3SupportedBlockTypesRetItem;
        #[swift_bridge(already_declared)]
        type AgentError;
        type MetadataEntry;
        type AccountResult;
        type Icrc3DataCertificateResult;
    }

    extern "Rust" {
        #[swift_bridge(already_declared)]
        type Service;
        async fn archives(&self) -> Result<Vec<ArchiveInfo>, AgentError>;
        async fn get_blocks(&self, arg0: GetBlocksArgs) -> Result<GetBlocksResponse, AgentError>;
        async fn get_data_certificate(&self) -> Result<DataCertificate, AgentError>;
        async fn get_transactions(
            &self,
            arg0: GetTransactionsRequest,
        ) -> Result<GetTransactionsResponse, AgentError>;
        async fn icrc_1_balance_of(&self, arg0: Account) -> Result<Tokens, AgentError>;
        async fn icrc_1_decimals(&self) -> Result<u8, AgentError>;
        async fn icrc_1_fee(&self) -> Result<Tokens, AgentError>;
        async fn icrc_1_metadata(&self) -> Result<Vec<MetadataEntry>, AgentError>;
        async fn icrc_1_minting_account(&self) -> Result<AccountResult, AgentError>;
        async fn icrc_1_name(&self) -> Result<String, AgentError>;
        async fn icrc_1_supported_standards(&self) -> Result<Vec<StandardRecord>, AgentError>;
        async fn icrc_1_symbol(&self) -> Result<String, AgentError>;
        async fn icrc_1_total_supply(&self) -> Result<Tokens, AgentError>;
        async fn icrc_1_transfer(&self, arg0: TransferArg) -> Result<TransferResult, AgentError>;
        async fn icrc_2_allowance(&self, arg0: AllowanceArgs) -> Result<Allowance, AgentError>;
        async fn icrc_2_approve(&self, arg0: ApproveArgs) -> Result<ApproveResult, AgentError>;
        async fn icrc_2_transfer_from(
            &self,
            arg0: TransferFromArgs,
        ) -> Result<TransferFromResult, AgentError>;
        async fn icrc_3_get_archives(
            &self,
            arg0: GetArchivesArgs,
        ) -> Result<GetArchivesResult, AgentError>;
        async fn icrc_3_get_blocks(
            &self,
            arg0: Vec<GetBlocksArgs>,
        ) -> Result<GetBlocksResult, AgentError>;
        async fn icrc_3_get_tip_certificate(
            &self,
        ) -> Result<Icrc3DataCertificateResult, AgentError>;
        async fn icrc_3_supported_block_types(
            &self,
        ) -> Result<Vec<Icrc3SupportedBlockTypesRetItem>, AgentError>;
    }
}
