/*
 * Copyright 2005-2014 Red Hat, Inc.
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.apache.activemq.artemis.version.tests.serverContainer;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;

/**
 * This class will send or receive a message using a specific version of artemis
 * @author Clebert Suconic
 */
public class ArtemisExternalSenderReceiver {

   static String text = "H";

   public static void main(String arg[]) {
      try {
         if (arg[0].equals("send")) {
            send(arg[1]);
         } else
         if (arg[0].equals("receive")) {
            receive(arg[1]);
         }

         System.exit(0);
      } catch (Throwable e) {
         e.printStackTrace();
         System.exit(-1);
      }
   }

   public static void send(String queueName) throws Exception {
      System.out.println("Sending message");
      ConnectionFactory factory = new ActiveMQJMSConnectionFactory();
      Connection connection = factory.createConnection();
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      Queue queue = session.createQueue(queueName);
      MessageProducer producer = session.createProducer(queue);
      producer.send(session.createTextMessage(text));
      connection.close();
   }

   public static void receive(String queueName) throws Exception {
      ConnectionFactory factory = new ActiveMQJMSConnectionFactory();
      Connection connection = factory.createConnection();
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      Queue queue = session.createQueue(queueName);
      connection.start();
      MessageConsumer consumer = session.createConsumer(queue);
      TextMessage message = (TextMessage)consumer.receive(5000);
      System.out.println("Received " + message.getText());
      if (!message.getText().equals(text)) {
         System.err.println("received message as " + message.getText());
         System.exit(-1);
      }
      connection.close();
   }

}
