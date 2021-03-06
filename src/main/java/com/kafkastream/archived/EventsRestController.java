package com.kafkastream.archived;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class EventsRestController
{
    /*
    private EventsSender eventsSender;

    private StateStoreService   stateStoreService;

    private Random random = new Random(1);

    @Autowired
    public EventsRestController(EventsSender    eventsSender,StateStoreService  stateStoreService)
    {
        this.eventsSender=eventsSender;
        this.stateStoreService=stateStoreService;
    }

    @GetMapping("/greetings")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Greetings sendGreetings() throws ExecutionException, InterruptedException
    {
        Random  random=new Random(1);
        String message = "Message "+random.nextInt(10000);
        Greetings greetings = new Greetings(message,getCurrentTime());
        eventsSender.sendGreetingsEvent(greetings);
        return greetings;
    }

    @GetMapping("/customers")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Customer sendCustomers() throws ExecutionException, InterruptedException
    {
        //Send Customer Event
        Customer customer=new Customer();
        customer.setCustomerId("CU"+random.nextInt(10000));
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john.doe@gmail.com");
        customer.setPhone("993-332-9832");
        eventsSender.sendCustomerEvent(customer);
        return customer;
    }

    @GetMapping("/orders")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Order sendOrders(String customerId) throws ExecutionException, InterruptedException
    {
        //Send Order Event
        Order order=new Order();
        order.setOrderId("ORD"+random.nextInt());
        if ((customerId != null))
        {
            order.setCustomerId(customerId);
        }
        else
        {
            order.setCustomerId("CU1001");
        }
        order.setOrderItemName("Reebok Shoes");
        order.setOrderPlace("NewYork,NY");
        order.setOrderPurchaseTime(getCurrentTime());
        eventsSender.sendOrderEvent(order);
        return order;
    }

    @GetMapping("/sendevents")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sendAllEvents() throws ExecutionException, InterruptedException
    {
        //sendGreetings();
        Customer customer=sendCustomers();
        sendOrders(customer.getCustomerId().toString());
    }

    @GetMapping("/customerOrders/{customerId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public List<CustomerOrderDTO> getCustomerOrderById(@PathVariable   String  customerId) throws Exception
    {
        //Get CustomerOrdersById
        return stateStoreService.getCustomerOrders(customerId);
    }


    private String getCurrentTime()
    {
        Calendar calendar=Calendar.getInstance(TimeZone.getDefault());
        return calendar.getTime().toString();
    }
*/
}
