/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package camelchat;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.dataformat.csv.CsvDataFormat;

abstract class ChatServerRouteBuilder extends RouteBuilder {
    public CsvDataFormat setCSVDelimiter(String delim) {
        CsvDataFormat csv = new CsvDataFormat();
        csv.setDelimiter(delim);
        return csv;
    }
}

/* Design Pattern: Strategy */
/* Builder of routes that handle input messages. */
class InputMessageRouteBuilder extends ChatServerRouteBuilder {
    @Override 
    public void configure() {
        CsvDataFormat csv = setCSVDelimiter("|");

        from("jms:queue:MessageInputQueue")
        .wireTap("direct:recordRoute")      // EIP: Wire Tap
        .process(new Processor(){           // EIP: Content-based Router
            public void process(Exchange e) throws Exception {
                // Route the message to corresponding topic, according to its header info. 
                String room = e.getIn().getHeader("Room", String.class);
                e.getIn().setHeader("CamelJmsDestinationName", "ChatRoom-" + room);
            }
        })
        .to("jms:topic:ChatRoom");  // this is a dummy name

        // The route that stores each message to a local csv file.
        from("direct:recordRoute")
        .process(new Processor(){   // EIP: Transform
            public void process(Exchange e) throws Exception {
                // Transform the body such that it can be converted to .csv format. 
                Message msg = e.getIn();
                String msgBody = msg.getBody(String.class).replace('\n', ';');
                String room = msg.getHeader("Room", String.class);
                
                Map<String, Object> mapBody = new HashMap<String, Object>();
                mapBody.put("UserName", msg.getHeader("UserName"));
                mapBody.put("Time", msg.getHeader("Time"));
                mapBody.put("Text", msgBody);
                
                msg.setBody(mapBody);
                msg.setHeader("CamelFileName", "room" + room + ".csv");
            }
        })
        .marshal(csv)
        .to("file:data?fileExist=Append");
    }
}

/* Builder of routes that handle a new receiver's request. */
class ReceiverRequestRouteBuilder extends ChatServerRouteBuilder {
    @Override 
    public void configure() {
        CsvDataFormat csv = setCSVDelimiter("|");

        // EIP: Request-Reply
        from("jms:queue:JoinRequestQueue?exchangePattern=InOut")    
        // Requests simply contain room number as message body
        .process(new Processor() {
            public void process(Exchange e) throws Exception {
                String room = e.getIn().getBody(String.class);
                File f = new File("./data/room" + room + ".csv");
                if (f.exists()) {
                    e.getOut().setBody(f);
                }
                else {
                    e.getOut().setBody(null);
                }
            }
        })
        .choice()   // EIP: Content Based Router
            .when(body().isNotNull())
                .unmarshal(csv);
    }
}


public class ChatServer {
    public static void main(String args[]) throws Exception {
        // create CamelContext
        CamelContext context = new DefaultCamelContext();

        // connect to ActiveMQ JMS broker listening on localhost on port 61616
        ConnectionFactory connectionFactory = 
            new ActiveMQConnectionFactory("tcp://localhost:61616");
            
        context.addComponent("jms",
            JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));
        
        // add routes to the camel context
        context.addRoutes(new InputMessageRouteBuilder());
        context.addRoutes(new ReceiverRequestRouteBuilder());

        // start the route and let it do its work
        context.start();
        Thread.sleep(5 * 60000);

        // stop the CamelContext
        context.stop();
    }
}
