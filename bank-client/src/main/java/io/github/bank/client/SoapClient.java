package io.github.bank.client;

import io.github.bank.dto.*;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import org.springframework.stereotype.Component;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class SoapClient {

    private static final String NAMESPACE = "http://www.github.com/io/github/soap/banking";
    private final WebServiceTemplate webServiceTemplate;

    public SoapClient(WebServiceTemplate webServiceTemplate) {
        this.webServiceTemplate = webServiceTemplate;
    }

    public CreateAccountResponse createAccount(String customerName, String email, String mobileNumber,
                                                AccountType accountType, BigDecimal initialBalance, String currency) {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setCustomerName(customerName);
        request.setEmail(email);
        request.setMobileNumber(mobileNumber);
        request.setAccountType(accountType);
        request.setInitialBalance(initialBalance);
        request.setCurrency(currency != null ? currency : "INR");

        return (CreateAccountResponse) webServiceTemplate.marshalSendAndReceive(
                request,
                new SoapActionCallback(NAMESPACE + "/CreateAccount")
        );
    }

    public CreditAccountResponse creditAccount(String accountNumber, BigDecimal amount,
                                                String transactionReference, String description) {
        CreditAccountRequest request = new CreditAccountRequest();
        request.setAccountNumber(accountNumber);
        request.setAmount(amount);
        request.setTransactionReference(transactionReference);
        request.setDescription(description);

        return (CreditAccountResponse) webServiceTemplate.marshalSendAndReceive(
                request,
                new SoapActionCallback(NAMESPACE + "/CreditAccount")
        );
    }

    public DebitAccountResponse debitAccount(String accountNumber, BigDecimal amount,
                                              String transactionReference, String description) {
        DebitAccountRequest request = new DebitAccountRequest();
        request.setAccountNumber(accountNumber);
        request.setAmount(amount);
        request.setTransactionReference(transactionReference);
        request.setDescription(description);

        return (DebitAccountResponse) webServiceTemplate.marshalSendAndReceive(
                request,
                new SoapActionCallback(NAMESPACE + "/DebitAccount")
        );
    }

    public FetchBalanceResponse fetchBalance(String accountNumber) {
        FetchBalanceRequest request = new FetchBalanceRequest();
        request.setAccountNumber(accountNumber);

        return (FetchBalanceResponse) webServiceTemplate.marshalSendAndReceive(
                request,
                new SoapActionCallback(NAMESPACE + "/FetchBalance")
        );
    }

    public GetAccountDetailsResponse getAccountDetails(String accountNumber) {
        GetAccountDetailsRequest request = new GetAccountDetailsRequest();
        request.setAccountNumber(accountNumber);

        return (GetAccountDetailsResponse) webServiceTemplate.marshalSendAndReceive(
                request,
                new SoapActionCallback(NAMESPACE + "/GetAccountDetails")
        );
    }

    public GetTransactionHistoryResponse getTransactionHistory(String accountNumber,
                                                                String fromDate,
                                                                String toDate,
                                                                TransactionType transactionType,
                                                                Integer pageNumber,
                                                                Integer pageSize) {
        GetTransactionHistoryRequest request = new GetTransactionHistoryRequest();
        request.setAccountNumber(accountNumber);

        if (fromDate != null && !fromDate.isEmpty()) {
            request.setFromDate(parseDateTime(fromDate));
        }
        if (toDate != null && !toDate.isEmpty()) {
            request.setToDate(parseDateTime(toDate));
        }
        if (transactionType != null) {
            request.setTransactionType(transactionType);
        }
        if (pageNumber != null) {
            request.setPageNumber(pageNumber);
        }
        if (pageSize != null) {
            request.setPageSize(pageSize);
        }

        return (GetTransactionHistoryResponse) webServiceTemplate.marshalSendAndReceive(
                request,
                new SoapActionCallback(NAMESPACE + "/GetTransactionHistory")
        );
    }

    private XMLGregorianCalendar parseDateTime(String dateTimeStr) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME);
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(dateTime.toString());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format. Use ISO format (yyyy-MM-ddTHH:mm:ss)", e);
        }
    }
}