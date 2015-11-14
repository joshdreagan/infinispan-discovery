/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.poc.greeter.impl;

import java.net.InetAddress;
import javax.jws.WebService;
import org.jboss.poc.greeter.Greeter;

@WebService(endpointInterface = "org.jboss.poc.greeter.Greeter",
            serviceName = "GreeterService",
            portName = "GreeterServicePort")
public class EnglishGreeter implements Greeter {

  @Override
  public String getGreeting(String name) {
    String greeting = null;
    try {
      greeting = String.format("Hello %s from %s!", name, InetAddress.getLocalHost().getHostAddress());
    } catch (Exception e) {
      greeting = String.format("Hello %s! I was unable to find my IP :(...", name); 
    }
    return greeting;
  }
}
