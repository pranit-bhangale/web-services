package io.github.bank.controller;

import io.github.bank.client.SoapClient;
import io.github.bank.controller.request.CreateAccountRequestDto;
import io.github.bank.controller.request.TransactionRequestDto;
import io.github.bank.dto.CreateAccountResponse;
import io.github.bank.dto.CreditAccountResponse;
import io.github.bank.dto.DebitAccountResponse;
import io.github.bank.dto.FetchBalanceResponse;
import io.github.bank.dto.GetAccountDetailsResponse;
import io.github.bank.dto.GetTransactionHistoryResponse;
import io.github.bank.dto.TransactionType;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final SoapClient soapClient;

    public AccountController(SoapClient soapClient) {
        this.soapClient = soapClient;
    }

    @PostMapping
    public ResponseEntity<CreateAccountResponse> createAccount(@Valid @RequestBody CreateAccountRequestDto request) {
        CreateAccountResponse response = soapClient.createAccount(
                request.getCustomerName(), request.getEmail(), request.getMobileNumber(),
                request.getAccountType(), request.getInitialBalance(), request.getCurrency()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{accountNumber}/credit")
    public ResponseEntity<CreditAccountResponse> creditAccount(
            @PathVariable String accountNumber,
            @Valid @RequestBody TransactionRequestDto request
    ) {
        CreditAccountResponse response = soapClient.creditAccount(
                accountNumber, request.getAmount(),
                request.getTransactionReference(), request.getDescription()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{accountNumber}/debit")
    public ResponseEntity<DebitAccountResponse> debitAccount(
            @PathVariable String accountNumber,
            @Valid @RequestBody TransactionRequestDto request
    ) {
        DebitAccountResponse response = soapClient.debitAccount(
                accountNumber, request.getAmount(),
                request.getTransactionReference(), request.getDescription()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<FetchBalanceResponse> fetchBalance(@PathVariable String accountNumber) {
        FetchBalanceResponse response = soapClient.fetchBalance(accountNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<GetAccountDetailsResponse> getAccountDetails(@PathVariable String accountNumber) {
        GetAccountDetailsResponse response = soapClient.getAccountDetails(accountNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountNumber}/transactions")
    public ResponseEntity<GetTransactionHistoryResponse> getTransactionHistory(
            @PathVariable String accountNumber,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize) {

        GetTransactionHistoryResponse response = soapClient.getTransactionHistory(
                accountNumber, fromDate, toDate, transactionType, pageNumber, pageSize);
        return ResponseEntity.ok(response);
    }
}