///  The whole step 2 in this project is about the changes I made here and that I also made in the application.yml
///



package com.jpmc.midascore;

import com.jpmc.midascore.component.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;


import com.jpmc.midascore.foundation.Transaction;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionListener {

    @Autowired
    private final TransactionService transactionService;

    public TransactionListener(TransactionService transactionService) {
        this.transactionService = transactionService;
    }


    @KafkaListener(topics = "${general.kafka-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(Transaction transaction) {
        transactionService.processTransaction(transaction);
    }
}
