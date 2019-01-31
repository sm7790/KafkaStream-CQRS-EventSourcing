package com.kafkastream.events;

import com.kafkastream.constants.KafkaConstants;
import com.kafkastream.dto.CustomerOrderDTO;
import com.kafkastream.model.Customer;
import com.kafkastream.model.CustomerOrder;
import com.kafkastream.model.Greetings;
import com.kafkastream.model.Order;
import com.kafkastream.web.kafkarest.StateStoreRestService;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroDeserializer;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;

import java.util.*;
import java.util.concurrent.CountDownLatch;


public class EventsListener
{
    private static KafkaStreams streams;

    private static StreamsBuilder streamsBuilder;

    private static Properties properties;

    private static void setUp()
    {
        properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, KafkaConstants.APPLICATION_ID_CONFIG);
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConstants.BOOTSTRAP_SERVERS_CONFIG);
        properties.put(StreamsConfig.APPLICATION_SERVER_CONFIG, KafkaConstants.APPLICATION_SERVER_CONFIG);
        properties.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, KafkaConstants.COMMIT_INTERVAL_MS_CONFIG);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, KafkaConstants.AUTO_OFFSET_RESET_CONFIG);
        properties.put("schema.registry.url", KafkaConstants.SCHEMA_REGISTRY_URL);
        properties.put("acks", "all");
        properties.put("key.deserializer", Serdes.String().deserializer().getClass());
        properties.put("value.deserializer", SpecificAvroDeserializer.class);

        streamsBuilder = new StreamsBuilder();
    }

    public static void main(String[] args)
    {
        //Setup StreamsBuilder
        setUp();

        List<CustomerOrderDTO> customerOrderList;
        SpecificAvroSerde<Customer> customerSerde = createSerde(KafkaConstants.SCHEMA_REGISTRY_URL);
        SpecificAvroSerde<Order> orderSerde = createSerde(KafkaConstants.SCHEMA_REGISTRY_URL);
        SpecificAvroSerde<Greetings> greetingsSerde = createSerde(KafkaConstants.SCHEMA_REGISTRY_URL);
        SpecificAvroSerde<CustomerOrder> customerOrderSerde = createSerde(KafkaConstants.SCHEMA_REGISTRY_URL);

        StoreBuilder customerStateStore = Stores.keyValueStoreBuilder(Stores.persistentKeyValueStore(KafkaConstants.CUSTOMER_STORE_NAME), Serdes.String(), customerSerde).withLoggingEnabled(new HashMap<>());
        StoreBuilder orderStateStore = Stores.keyValueStoreBuilder(Stores.persistentKeyValueStore(KafkaConstants.ORDER_STORE_NAME), Serdes.String(), customerSerde).withLoggingEnabled(new HashMap<>());
        StoreBuilder greetingsStateStore = Stores.keyValueStoreBuilder(Stores.persistentKeyValueStore(KafkaConstants.GREETING_STORE_NAME), Serdes.String(), greetingsSerde).withLoggingEnabled(new HashMap<>());
        StoreBuilder customerOrderStateStore = Stores.keyValueStoreBuilder(Stores.persistentKeyValueStore(KafkaConstants.CUSTOMER_ORDER_STORE_NAME), Serdes.String(), customerSerde).withLoggingEnabled(new HashMap<>()).withCachingEnabled();

        KTable<String, Customer> customerKTable = streamsBuilder.table("customer", Materialized.<String, Customer, KeyValueStore<Bytes, byte[]>>as(customerStateStore.name())
                                                                .withKeySerde(Serdes.String())
                                                                .withValueSerde(customerSerde));

        KTable<String, Order> orderKTable = streamsBuilder.table("order", Materialized.<String, Order,
                KeyValueStore<Bytes, byte[]>>as(orderStateStore.name())
                                                                .withKeySerde(Serdes.String())
                                                                .withValueSerde(orderSerde));

        KTable<String, Greetings> greetingsKTable = streamsBuilder.table("greetings", Materialized.<String, Greetings,
                KeyValueStore<Bytes, byte[]>>as(greetingsStateStore.name())
                                                                    .withKeySerde(Serdes.String())
                                                                    .withValueSerde(greetingsSerde));


        KTable<String, CustomerOrder> customerOrderKTable = customerKTable.join(orderKTable, (customer, order) ->
        {
            if (customer != null && order != null)
            {
                CustomerOrder customerOrder = new CustomerOrder();
                customerOrder.setCustomerId(customer.getCustomerId());
                customerOrder.setFirstName(customer.getFirstName());
                customerOrder.setLastName(customer.getLastName());
                customerOrder.setEmail(customer.getEmail());
                customerOrder.setPhone(customer.getPhone());
                customerOrder.setOrderId(order.getOrderId());
                customerOrder.setOrderItemName(order.getOrderItemName());
                customerOrder.setOrderPlace(order.getOrderPlace());
                customerOrder.setOrderPurchaseTime(order.getOrderPurchaseTime());
                return customerOrder;
            }
            return null;
        }, Materialized.<String, CustomerOrder, KeyValueStore<Bytes, byte[]>>as(customerOrderStateStore.name()).withKeySerde(Serdes.String()).withValueSerde(customerOrderSerde));

        //Print customerOrderKTable
        customerOrderKTable.filter((key, value) ->
        {
            System.out.println("customerOrderKTable.key: " + key);
            System.out.println("customerOrderKTable.value: " + value);
            return true;
        });


        Topology topology = streamsBuilder.build();
        streams = new KafkaStreams(topology, properties);
        CountDownLatch latch = new CountDownLatch(1);
        // This is not part of Runtime.getRuntime() block
        try
        {
            streams.start();
            final HostInfo restEndpoint = new HostInfo(KafkaConstants.REST_PROXY_HOST, KafkaConstants.REST_PROXY_PORT);
            startRestProxy(streams, restEndpoint);
            latch.await();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        //Close Runtime
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook")
        {
            @Override
            public void run()
            {
                streams.close();
                latch.countDown();
            }
        });
        System.exit(0);
    }


    private static <VT extends SpecificRecord> SpecificAvroSerde<VT> createSerde(String schemaRegistryUrl)
    {

        SpecificAvroSerde<VT> serde = new SpecificAvroSerde<>();
        Map<String, String> serdeConfig = Collections.singletonMap(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        serde.configure(serdeConfig, false);
        return serde;
    }


    private static StateStoreRestService startRestProxy(final KafkaStreams streams, final HostInfo hostInfo) throws Exception
    {
        StateStoreRestService interactiveQueriesRestService = new StateStoreRestService(streams, hostInfo);
        interactiveQueriesRestService.start();
        return interactiveQueriesRestService;
    }

}