/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.activemq.artemis.version.tests;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.artemis.version.base.ClientContainer;
import org.apache.activemq.artemis.version.base.ClientServerExchange;
import org.apache.activemq.artemis.version.base.ServerContainer;
import org.apache.activemq.artemis.version.base.VersionBaseTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractSimpleProtocolIDsTest extends VersionBaseTest {

   protected abstract ClientServerExchange newExchange() throws Exception;

   ClientServerExchange exchange;
   ServerContainer serverContainer;

   ClientContainer clientContainer;

   @Before
   public void setUp() throws Exception {

      exchange = newExchange();

      serverContainer = exchange.newServer(0);
      serverContainer.start();

      clientContainer = exchange.newClient();
   }

   @After
   public void tearDown() throws Exception {
      serverContainer.stop();
      clientContainer.close();
   }

   @Test
   public void testMessagePropertiesAreTransformedBetweenCoreAndHQProtocols() throws Exception {

      ConnectionFactory connectionFactory = clientContainer.getFactory();
      Connection connection = connectionFactory.createConnection();

      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

      // Create Queue
      String queueName = "test.hq.queue";

      serverContainer.createQueue(queueName);

      Queue queue =  session.createQueue(queueName);

      MessageProducer producer = session.createProducer(queue);
      MessageConsumer consumer = session.createConsumer(queue);


      // HornetQ Client Objects
      connection.start();

      // Check that HornetQ Properties are correctly converted to core properties.
      for (int i = 0; i < 50; i++) {
         TextMessage message = session.createTextMessage("message " + i);
         message.setStringProperty(clientContainer.get_HDR_DUPLICATE_DETECTION_ID(), "message " + i);
         producer.send(message);
      }

      for (int i = 50; i < 100; i++) {
         TextMessage message = session.createTextMessage("message " + i);
         message.setStringProperty(clientContainer.get_HDR_DUPLICATE_DETECTION_ID(), "message " + i);
         producer.send(queue, message);
      }


      for (int i = 0; i < 100; i++) {
         TextMessage message = (TextMessage)consumer.receive(5000);
         Assert.assertNotNull(message);
         Assert.assertEquals("message " + i, message.getStringProperty(clientContainer.get_HDR_DUPLICATE_DETECTION_ID()));
      }

      connection.close();
   }
}
