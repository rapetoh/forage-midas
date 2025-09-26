package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Incentive;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
public class TransactionService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final RestTemplate restTemplate = new RestTemplate();


    public TransactionService(UserRepository userRepository, TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public void processTransaction(Transaction transaction) {
        // 1. Lookup sender and recipient
        Optional<UserRecord> senderOpt = Optional.ofNullable(userRepository.findById(transaction.getSenderId()));
        Optional<UserRecord> recipientOpt = Optional.ofNullable(userRepository.findById(transaction.getRecipientId()));

        if (senderOpt.isEmpty() || recipientOpt.isEmpty()) {
            return; // invalid users → discard
        }

        UserRecord sender = senderOpt.get();
        UserRecord recipient = recipientOpt.get();

        // 2. Validate balance
        if (sender.getBalance() < transaction.getAmount()) {
            return; // insufficient funds → discard
        }

        // Call Incentive API
        ResponseEntity<Incentive> response =
                restTemplate.postForEntity("http://localhost:9090/incentive", transaction, Incentive.class);

        Incentive incentive = response.getBody();
        float incentiveAmount = (incentive != null) ? incentive.getAmount() : 0f;

// Apply incentive to recipient only
        recipient.setBalance(recipient.getBalance() + incentiveAmount);

// Save users
        userRepository.save(sender);
        userRepository.save(recipient);

// Record transaction
        TransactionRecord record = new TransactionRecord();
        record.setSender(sender);
        record.setRecipient(recipient);
        record.setAmount(transaction.getAmount());
        record.setIncentive(incentiveAmount);

        transactionRepository.save(record);

    }
}
