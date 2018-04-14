package com.kafkastream.service;

import com.kafkastream.model.Customer;
import com.kafkastream.model.Greetings;
import com.kafkastream.model.Order;
import com.kafkastream.stream.GenericStreams;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


@Service
@Slf4j
@EnableAutoConfiguration
public class EventsSender
{
    private Producer<String, String> kafkaProducer;

    private Properties properties;


    public EventsSender() throws UnknownHostException
    {
        this.properties = new Properties();
        properties.put("application.id", InetAddress.getLocalHost().getHostName());
        properties.put("bootstrap.servers", "localhost:9092");
        properties.put("acks", "all");
        properties.put("retries", 5);
        properties.put("batch.size", 16384);
        properties.put("linger.ms", 1);
        properties.put("buffer.memory", 33554432);

        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        kafkaProducer = new KafkaProducer<String, String>(properties);
    }

    public void sendGreetingsEvent(Greetings greetings) throws ExecutionException, InterruptedException
    {
        ProducerRecord<String, String> greetingsRecord = new ProducerRecord<>("greetings", greetings.getMessage(), greetings.toString());
        Future<RecordMetadata> future = kafkaProducer.send(greetingsRecord);
        System.out.println("Greetings record Sent. Greetings message: "+greetings.getMessage());
        System.out.println("Greetings future.get(): "+future.get());

    }


    public void sendCustomerEvent(Customer customer) throws ExecutionException, InterruptedException
    {
        ProducerRecord<String, String> customerRecord = new ProducerRecord<>("customer", customer.getCustomerId(), customer.toString());
        Future<RecordMetadata> future = kafkaProducer.send(customerRecord);
        System.out.println("Customer record sent. Customer Id: "+customer.getCustomerId());
        System.out.println("Customer future.get(): "+future.get());
    }

    public void sendOrderEvent(Order order) throws ExecutionException, InterruptedException
    {
        ProducerRecord<String, String> orderRecord = new ProducerRecord<>("order", order.getOrderId(), order.toString());
        Future<RecordMetadata> future = kafkaProducer.send(orderRecord);
        System.out.println("Customer order sent. Order Id: "+order.getOrderId());
        System.out.println("Order future.get(): "+future.get());
    }


}