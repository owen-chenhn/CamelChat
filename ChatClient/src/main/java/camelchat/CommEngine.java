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
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;

import org.apache.camel.component.jms.JmsComponent;

import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;

/* The communication layer of ChatClient application. */

class ChatMessageListener implements MessageListener {
    ChatSender sender;
    public ChatMessageListener(ChatSender s) { sender = s; }
	public void onMessage(Message m) {
		TextMessage msg = (TextMessage) m;
		String name = "", time = "", text = "";
		try {
			name = msg.getStringProperty("UserName");
			time = msg.getStringProperty("Time");
			text = msg.getText();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		sender.receiverMsg(name, time, text);
	}
}

/* Facade of communication layer. Also a Singleton class. */
public class CommEngine {
	private CamelContext camelContext;
	private ProducerTemplate producer;
	private SimpleDateFormat dateFormatter;
	private ConnectionFactory connectFactory;
	private Connection connection;
	private Session session;
	private ChatMessageListener listener;
    private MessageConsumer consumer;
    
    private CommEngine(ChatSender s) {
        listener = new ChatMessageListener(s);
		connectFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
		try {
			connection = connectFactory.createConnection();
			session = connection.createSession(false,
								Session.AUTO_ACKNOWLEDGE);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		consumer = null;
		
		camelContext = new DefaultCamelContext();
		// connect to ActiveMQ JMS broker listening on localhost on port 61616
        camelContext.addComponent("jms",
            JmsComponent.jmsComponentAutoAcknowledge(connectFactory));
        producer = camelContext.createProducerTemplate();
        dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:sss");
        
        try {
        	connection.start();
        	camelContext.start();
        }
        catch (Exception e) {
        	e.printStackTrace();
        }
    }
    /* Implementation of pattern Singleton. */
    private static CommEngine _instance = null;
	
	public static CommEngine getInstance(ChatSender s) {
		if (_instance == null) {
			_instance = new CommEngine(s);
		}
		return _instance;
	}

    public void stop() {
		try {
			connection.close();
        	camelContext.stop();
        }
        catch (Exception e) {
        	e.printStackTrace();
        }
    }
    
    /* Send request to the server to obtain the chat records. */
    public Object requestRecords(String room) {
        return producer.requestBody("jms:queue:JoinRequestQueue", room);
    }

    /* Set the message listener to listen to jms topic: ChatRoom-{RoomName} */
    public void setListener(String room) {
        try {
			if (consumer != null) { consumer.close(); }
			consumer = session.createConsumer(session.createTopic("ChatRoom-" + room));
			consumer.setMessageListener(listener);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    /* Send message to jms:queue:MessageInputQueue */
    public void sendMessage(String msg, Map<String, Object>msgHeaders) {
		msgHeaders.put("Time", dateFormatter.format(new Date()));
        producer.sendBodyAndHeaders("jms:queue:MessageInputQueue", msg, msgHeaders);
    }
}