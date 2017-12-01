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
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.version.base.ClientServerExchange;
import org.apache.activemq.artemis.version.base.util.SpawnVMSupport;
import org.apache.activemq.artemis.version.tests.base.IsolatedServerVersionBaseTest;
import org.apache.activemq.artemis.version.tests.serverContainer.ArtemisExternalSenderReceiver;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MeshTest extends IsolatedServerVersionBaseTest {

   // a maven plugin will setup this.
   String lib240 = System.getProperty("lib240");
   String libSnapshot = System.getProperty("libSnapshot");

   @Before
   public void checkLibs() {
      Assert.assertNotNull("You need to use maven to run this test", lib240);
      Assert.assertNotNull("You need to use maven to run this test", libSnapshot);

      // you could setup these manually on the ide. capture the value from a run and put on your IDE Run settings
      printLib("lib240", lib240);
      printLib("libSnapshot", libSnapshot);
   }

   private void printLib(String name, String value) {
      System.out.println(name + " ==================================================");
      System.out.println(lib240);
      System.out.println(name + " ==================================================");
   }

   @Override
   protected ClientServerExchange newExchange() throws Exception {
      return new ArtemisServerExchange(this.temporaryFolder.newFolder().toString());
   }

   @Test
   public void testSendAMQPReceiveCore() throws Exception {
      ConnectionFactory connectionFactory = new JmsConnectionFactory("amqp://localhost:61616");
      sendMessage(connectionFactory);

      receiveHornetQ();
   }

   @Test
   public void testSend240ReceiveSnapshot() throws Exception {
      runExternalClient(lib240, ArtemisExternalSenderReceiver.class.getName(), "send", queueName);
      runExternalClient(libSnapshot, ArtemisExternalSenderReceiver.class.getName(),"receive", queueName);
   }

   @Test
   public void testSendHornetQReceiveHornetQ() throws Exception {
      ConnectionFactory connectionFactory = clientContainer.getFactory();
      sendMessage(connectionFactory);

      receiveHornetQ();

   }

   @Test
   public void testSendArtemisReceiveHornetQ() throws Exception {
      ConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
      sendMessage(connectionFactory);

      receiveHornetQ();
   }

   @Test
   public void testSendArtemisReceiveArtemis() throws Exception {
      ConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
      for (int i = 0; i < 10; i++)
         sendMessage(connectionFactory);

      receive(connectionFactory);

   }

   private void sendMessage(ConnectionFactory connectionFactory) throws JMSException {
      System.out.println("Sending");
      Connection connection = connectionFactory.createConnection();
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      Queue queue = session.createQueue(queueName);
      MessageProducer producer = session.createProducer(queue);
      producer.send(session.createTextMessage("hello!"));
      connection.close();
   }

   private void receiveHornetQ() throws JMSException {
      ConnectionFactory hornetqFactory = clientContainer.getFactory();
      receive(hornetqFactory);
   }

   private void receive(ConnectionFactory hornetqFactory) throws JMSException {
      Connection hornetqconnection = hornetqFactory.createConnection();
      Session session = hornetqconnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      Queue queue = session.createQueue(queueName);
      MessageConsumer consumer = session.createConsumer(queue);
      hornetqconnection.start();
      TextMessage message = (TextMessage) consumer.receive(5000);
      Assert.assertEquals("hello!", message.getText());
   }

   private void runExternalClient(String lib, String className, String... args) throws Exception {
      Process process = SpawnVMSupport.spawnVM(null, lib, className, null, true, true, args[0], args);
      Assert.assertEquals(0, process.waitFor());
   }

}
