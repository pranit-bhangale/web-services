package io.github.soap.banking.endpoint;

import io.github.soap.banking.service.AccountService;
import io.github.soap.dto.*;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
public class AccountEndpoint {

    private static final String NAMESPACE_URI = "http://www.github.com/io/github/soap/banking";
    private final AccountService service;

    public AccountEndpoint(AccountService service) {
        this.service = service;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "CreateAccountRequest")
    @ResponsePayload
    public CreateAccountResponse createNewAccount(@RequestPayload CreateAccountRequest request) {
        return this.service.createNewAccount(request);
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "CreditAccountRequest")
    @ResponsePayload
    public CreditAccountResponse creditAmount(@RequestPayload CreditAccountRequest request) {
        return this.service.creditAmount(request);
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "DebitAccountRequest")
    @ResponsePayload
    public DebitAccountResponse debitAmount(@RequestPayload DebitAccountRequest request) {
        return this.service.debitAmount(request);
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "FetchBalanceRequest")
    @ResponsePayload
    public FetchBalanceResponse fetchBalance(@RequestPayload FetchBalanceRequest request) {
        return this.service.fetchBalance(request);
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "GetAccountDetailsRequest")
    @ResponsePayload
    public GetAccountDetailsResponse getAccountDetails(@RequestPayload GetAccountDetailsRequest request) {
        return this.service.getAccountDetails(request);
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "GetTransactionHistoryRequest")
    @ResponsePayload
    public GetTransactionHistoryResponse getTransactionHistory(@RequestPayload GetTransactionHistoryRequest request) {
        return this.service.getTransactionHistory(request);
    }
}