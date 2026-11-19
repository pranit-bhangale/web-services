package io.github.soap.banking.service;

import io.github.soap.dto.*;

public interface AccountService {

    CreateAccountResponse createNewAccount(CreateAccountRequest request);

    CreditAccountResponse creditAmount(CreditAccountRequest request);

    DebitAccountResponse debitAmount(DebitAccountRequest request);

    FetchBalanceResponse fetchBalance(FetchBalanceRequest request);

    GetAccountDetailsResponse getAccountDetails(GetAccountDetailsRequest request);

    GetTransactionHistoryResponse getTransactionHistory(GetTransactionHistoryRequest request);
}
