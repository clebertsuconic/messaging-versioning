/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.activemq.artemis.version.base.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class SpawnVMSupport {

   final static List<Process> processes = Collections.synchronizedList(new LinkedList<Process>());


   static {
      Runtime.getRuntime().addShutdownHook(new Thread() {
         public void run() {
            for (Process process : processes) {
               process.destroy();
            }
         }
      });

   }

   /**
    * @param lookupWord The word the process will print when it's ready
    * @param classpath The class path to start the VM
    * @param className The className
    * @param vmargs
    * @param logOutput
    * @param logErrorOutput
    * @param args
    * @return
    * @throws Exception
    */
   public static Process spawnVM(final String lookupWord,
                                 final String classpath,
                                 final String className,
                                 final String[] vmargs,
                                 final boolean logOutput,
                                 final boolean logErrorOutput,
                                 final String logName,
                                 final String... args) throws Exception {

      if (System.getProperty("ignoreStart") != null) {
         return null;
      }
      
      ProcessBuilder builder = new ProcessBuilder();
      final String javaPath = Paths.get(System.getProperty("java.home"), "bin", "java").toAbsolutePath().toString();
      builder.command(javaPath);

      List<String> commandList = builder.command();

      if (vmargs != null) {
         for (String arg : vmargs) {
            commandList.add(arg);
         }
      }

      commandList.add("-cp");
      commandList.add(classpath);

      commandList.add(className);
      for (String arg : args) {
         commandList.add(arg);
      }

      System.out.println("#### command:");
      for (String string : builder.command()) {
         System.out.print(string + " ");
      }
      System.out.println();

      Process process = builder.start();
      processes.add(process);

      ProcessLogger outputLogger = new ProcessLogger(logOutput, process.getInputStream(), logName);
      CountDownLatch latch = new CountDownLatch(1);
      outputLogger.setLookupWord(lookupWord, latch);
      outputLogger.start();

      // Adding a reader to System.err, so the VM won't hang on a System.err.println as identified on this forum thread:
      // http://www.jboss.org/index.html?module=bb&op=viewtopic&t=151815
      ProcessLogger errorLogger = new ProcessLogger(logErrorOutput, process.getErrorStream(), logName + "_err");
      errorLogger.start();

      if (lookupWord != null) {
         latch.await(60, TimeUnit.SECONDS);
      }

      return process;
   }

   /**
    * Redirect the input stream to a logger (as debug logs)
    */
   static class ProcessLogger extends Thread {

      private final InputStream is;

      private final String className;

      private final boolean print;

      private String lookup;
      private CountDownLatch latch;

      public void setLookupWord(String lookupWord, CountDownLatch latch) {
         this.lookup = lookupWord;
         this.latch = latch;

      }

      ProcessLogger(final boolean print, final InputStream is, final String className) throws ClassNotFoundException {
         this.is = is;
         this.print = print;
         this.className = className;
         setDaemon(true);
      }

      @Override
      public void run() {
         try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
               if (print) {
                  System.out.println(className + ":" + line);
               } else {
                  System.out.println("not printing");
               }
               if (lookup != null && lookup.equals(line)) {
                  latch.countDown();
               }
            }
         }
         catch (IOException ioe) {
            ioe.printStackTrace();
         }
      }
   }
}
