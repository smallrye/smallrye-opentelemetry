# RestEasy example deployed on Jetty

RestEasy OpenTelemetry example on Jetty.
The reported span data is logged to stdout and send to via OpenTelemetry protocol (OTLP) to
Jaeger server (`localhost:55680`).

## Run 

Start Jaeger server:
```bash
docker run --rm -it -p 16686:16686 -p 55680:55680  jaegertracing/opentelemetry-all-in-one:latest
```

Run the example:
```bash
OTEL_RESOURCE_ATTRIBUTES=service.name=resteasy OTEL_OTLP_ENDPOINT=localhost:55680 mvn jetty:run
```

Execute requests
```bash
curl localhost/hello
curl localhost/error
```
