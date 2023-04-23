package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.BalanceHistory;
import com.shepherdmoney.interviewproject.model.CreditCard;
import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.repository.BalanceHistoryRepository;
import com.shepherdmoney.interviewproject.repository.CreditCardRepository;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.vo.request.AddCreditCardToUserPayload;
import com.shepherdmoney.interviewproject.vo.request.UpdateBalancePayload;
import com.shepherdmoney.interviewproject.vo.response.CreditCardView;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
public class CreditCardController {

    // TODO: wire in CreditCard repository here (~1 line)
    @Autowired
    private CreditCardRepository creditCardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BalanceHistoryRepository balanceHistoryRepository;

    @PostMapping("/credit-card")
    public ResponseEntity<Integer> addCreditCardToUser(@RequestBody AddCreditCardToUserPayload payload) {
        // TODO: Create a credit card entity, and then associate that credit card with user with given userId
        //       Return 200 OK with the credit card id if the user exists and credit card is successfully associated with the user
        //       Return other appropriate response code for other exception cases
        //       Do not worry about validating the card number, assume card number could be any arbitrary format and length
        if (!userRepository.existsById(payload.getUserId())) {
            return ResponseEntity.badRequest().body(payload.getUserId());
        }
        CreditCard creditCard = new CreditCard();
        creditCard.setIssuanceBank(payload.getCardIssuanceBank());
        creditCard.setNumber(payload.getCardNumber());
        creditCard.setUserId(payload.getUserId());
        creditCard = creditCardRepository.save(creditCard);
        return ResponseEntity.ok(creditCard.getId());
    }

    @GetMapping("/credit-card:all")
    public ResponseEntity<List<CreditCardView>> getAllCardOfUser(@RequestParam int userId) {
        // TODO: return a list of all credit card associated with the given userId, using CreditCardView class
        //       if the user has no credit card, return empty list, never return null
        List<CreditCardView> creditCardViews = new ArrayList<>();
        List<CreditCard> creditCards = creditCardRepository.findAllByUserId(userId);
        if (!CollectionUtils.isEmpty(creditCards)) {
            for (CreditCard creditCard : creditCards) {
                CreditCardView creditCardView = new CreditCardView(creditCard.getIssuanceBank(), creditCard.getNumber());
                creditCardViews.add(creditCardView);
            }
        }
        return ResponseEntity.ok(creditCardViews);
    }

    @GetMapping("/credit-card:user-id")
    public ResponseEntity<Integer> getUserIdForCreditCard(@RequestParam String creditCardNumber) {
        // TODO: Given a credit card number, efficiently find whether there is a user associated with the credit card
        //       If so, return the user id in a 200 OK response. If no such user exists, return 400 Bad Request
        CreditCard creditCard = creditCardRepository.findByNumber(creditCardNumber);
        if (creditCard != null) {
            int userId = creditCard.getUserId();
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isPresent()) {
                return ResponseEntity.ok(userOptional.get().getId());
            }
        }
        return ResponseEntity.badRequest().body(-1);
    }

    @PostMapping("/credit-card:update-balance")
    public ResponseEntity<String> postMethodName(@RequestBody UpdateBalancePayload[] payload) {
        //TODO: Given a list of transactions, update credit cards' balance history.
        //      For example: if today is 4/12, a credit card's balanceHistory is [{date: 4/12, balance: 110}, {date: 4/10, balance: 100}],
        //      Given a transaction of {date: 4/10, amount: 10}, the new balanceHistory is
        //      [{date: 4/12, balance: 120}, {date: 4/11, balance: 110}, {date: 4/10, balance: 110}]
        //      Return 200 OK if update is done and successful, 400 Bad Request if the given card number
        //        is not associated with a card.
        if (payload.length != 0) {
            for (UpdateBalancePayload updateBalancePayload : payload) {
                CreditCard creditCard = creditCardRepository.findByNumber(updateBalancePayload.getCreditCardNumber());
                if (creditCard == null) {
                    return ResponseEntity.badRequest().body("card number not exist");
                }
                //change history
                List<BalanceHistory> balanceHistories = balanceHistoryRepository.findAllByCreditCardIdAndDateGreaterThanEqual(creditCard.getId(), updateBalancePayload.getTransactionTime());
                if (!CollectionUtils.isEmpty(balanceHistories)) {
                    balanceHistories.forEach(history -> history.setBalance(history.getBalance() + updateBalancePayload.getCurrentBalance()));
                } else {
                    BalanceHistory balanceHistory = new BalanceHistory();
                    balanceHistory.setBalance(updateBalancePayload.getCurrentBalance());
                    balanceHistory.setDate(updateBalancePayload.getTransactionTime());
                    balanceHistory.setCreditCardId(creditCard.getId());
                    balanceHistories.add(balanceHistory);
                }
                balanceHistoryRepository.saveAll(balanceHistories);

            }
        }
        return ResponseEntity.ok("success");
    }
    
}
