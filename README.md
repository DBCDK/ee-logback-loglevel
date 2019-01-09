# Log Level - an admin EJB

The purpose is to allow for runtime change of log level on a web application.

This is an EJB that is simply depended upon like this:

        <dependency>
            <groupId>dk.dbc</groupId>
            <artifactId>ee-logback-level</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>

Then it registers a `@Stateless` jax-rs bean, that is located upon `@ApplicationPath`/`log-level`.

It server a web page (If you're allowed to access it), that gives a tree of the loggers initialized,
with drop-downs for each node, to set the given log-level.

## Access Control

Configuration of the bean is done with environment variables:

* ADMIN_IP a comma separated list of ip, ip-range or net of admin allowed hosts (only ipv4).

    This is functionally required, however it doesn't fail if it's present, but then all access is unauthorized.

* X_FORWARDED_FOR (optional) a comma separated list of ip, ip-range or net of trusted proxies (only ipv4)
  that can add `X-Forwarded-For` HTTP headers.

## Example

An example application is made in [example](example). It is start with `mvn verify -Drun (-Dport=xxxx)`.
Remember to build and install the EJB first.