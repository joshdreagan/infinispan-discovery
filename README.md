Custom Camel Load Balancer With Inifinispan Discovery
=====================================================

This example shows how to register/discover/loadbalance JAX-WS services using Camel and Inifinispan.

It consists of 2 parts: A JAX-WS service bundled as a war to be deployed to JBoss WildFly (or any servlet container really), and a Camel route with a custom load-balancer implementation.

Requirements
------------

- [Apache Maven 3.x](http://maven.apache.org)
- [JBoss WildFly 9.0.x](http://wildfly.org)

Building Example
----------------

Run the Maven build

```
~$ cd $PROJECT_ROOT
~$ mvn clean install
```

Running Camel
-------------

```
~$ cd $PROJECT_ROOT/camel-gateway
~$ mvn -Djava.net.preferIPv4Stack=true camel:run
```

Setup JBoss WildFly
-------------------

Start the JBoss WildFly server

```
~$ $JBOSS_WILDFLY_HOME/bin/standalone.sh
```

Deploy the JAX-WS service

```
~$ cd $PROJECT_ROOT/greeter-impl
~$ mvn jboss-as:deploy
```

Testing
-------

Use SoapUI (or your favorite WS testing tool) to invoke the (now) load-balanced service located at [http://localhost:9000/gateway/GreeterService](http://localhost:9000/gateway/GreeterService). You can setup another JBoss WildFly instance (on a different machine, or the same one with a port offset) and see that it gets registered and added to the list of endpoints to load-balance to. No restart required!
