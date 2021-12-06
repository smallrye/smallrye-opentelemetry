# OpenTelemetry Jaeger Example

## With MP OpenTelemetry

- Run Jaeger: `docker-compose up -d`
- Run Jetty: `mvn jetty:run`
- Call REST Endpoint: `curl http://localhost:8080/numbers/generate`
- Check Jaeger: `http://localhost:16686`

# With Agent Instrumentation

- Run Jaeger: `docker-compose up -d`
- Run Jetty: `mvn jetty:run -Pjetty-agent`
- Call REST Endpoint: `curl http://localhost:8080/numbers/generate`
- Check Jaeger: `http://localhost:16686`
