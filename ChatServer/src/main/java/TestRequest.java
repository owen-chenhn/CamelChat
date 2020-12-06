import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;

import java.util.List;

import javax.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.component.jms.JmsComponent;

public class TestRequest {
    public static void main(String args[]) throws Exception {
        // create CamelContext
        CamelContext context = new DefaultCamelContext();

        // connect to ActiveMQ JMS broker listening on localhost on port 61616
        ConnectionFactory connectionFactory = 
            new ActiveMQConnectionFactory("tcp://localhost:61616");
            
        context.addComponent("jms",
            JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));
        
        // add routes to the camel context
        context.addRoutes(new RouteBuilder(){
            public void configure() {
                from("direct:test-in")
                .inOut()
                .to("jms:queue:JoinRequestQueue")
                .split(body())
                .process(new Processor() {
                    public void process(Exchange e) throws Exception {
                        Message in = e.getIn();
                        List<String> data = (List<String>) in.getBody();
                        String name = data.get(0);
                        String time = data.get(1);
                        String msg = data.get(2);
                        in.setBody(name+" ("+time+"): \n"+msg+"\n\n", String.class);
                    }
                })
                .to("file:data/test?fileExist=Append");
            }
        });

        // start the route and let it do its work
        context.start();

        //ConsumerTemplate consumer = context.createConsumerTemplate();
        //Object body = consumer.receiveBody("file:data?fileName=room01.csv&noop=true");

        ProducerTemplate producer = context.createProducerTemplate();
        producer.sendBody("direct:test-in", "01");
        Thread.sleep(5000);

        // stop the CamelContext
        context.stop();
    }
}