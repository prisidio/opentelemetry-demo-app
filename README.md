# opentelemetry observability test app
This repo contains Java application exposing APIs using Dropwizard, and a dockerfile to test metrics and traces management via [otel](https://github.com/open-telemetry).

## Aim is to test metrics and traces in
- Non-AWS environment
    - [otel-collector](https://github.com/open-telemetry/opentelemetry-collector) is used to get auto-generated web app metrics via [otel-java-instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation) agent
    - Metrics are sent to grafana/prometheus which is running via docker locally
    - Traces are sent to Jaeger which is running via docker locally
- AWS environment
    - [aws-otel-collector](https://github.com/aws-observability/aws-otel-collector) is used to get auto-generated web app metrics via [aws-otel-java-instrumentation](https://github.com/aws-observability/aws-otel-java-instrumentation) agent
    - Metrics are sent to [XRay](https://aws.amazon.com/xray/) AWS Service
    - Traces are sent to [AMP (Amazon managed prometheus)](https://aws.amazon.com/prometheus/)

## web-app
#### Two custom metrics are instrumented:
- A histogram for measuring API latency
- A counter to track API invocation
#### A custom span is instrumented:
- To measure random latency and wrap the error
#### API
- which can simply return a payload and make a call to external API (`/` or `/name=call`)
  - This is useful to generate auto instrumented http_client_duration metrics
- which can simply return a payload and make catch an exception (`/` or `/name=error`)
    - This is useful to wrap error message in trace
- which can simply return a payload (`/` or `/name=anythingelse`)
#### auto-instrumentation agent
- [otel-java-agent](https://github.com/open-telemetry/opentelemetry-java-instrumentation) and [aws-otel-java-agent](https://github.com/aws-observability/aws-otel-java-instrumentation) are downloaded in Dockerfile
  - Change the versions of hava agents in here when using different version of java agent
#### logging format
  - Print trace ID in logs
  
## What happens when stack is up
- Two web apps are deployed, one running with otel-java-agent and another with aws-otel-java-agent
- Traffic generator containers make API calls to generate metrics
- These web apps send traces and metrics to [otel-collector](https://github.com/open-telemetry/opentelemetry-collector) and [aws-otel-collector](https://github.com/aws-observability/aws-otel-collector) respectively (Update versions of collectors in docker-compose file when testing with alternate versions)
- [otel-collector](https://github.com/open-telemetry/opentelemetry-collector) is running
  - Exposes metrics via prometheus exporter (which is scraped by local prometheus, and visualized in Grafana)
  - Export traces to Jaeger
- [aws-otel-collector](https://github.com/aws-observability/aws-otel-collector) is running
- Exports metrics via [AMP](https://aws.amazon.com/prometheus/) remote_write (which can be viewed via AWS managed Grafana)
- Export traces to [XRay](https://aws.amazon.com/xray/)

## How to run
```
Note: To test with AWS components, un-comment below containers in docker-compose.yaml:
- web-app-using-aws-otel-collector-and-agent
- aws-otel-collector
- aws-traffic-generator
and ensure below ENV vars are set in aws-otel-collector
- AWS_REGION=us-east-1 #(any other as desired)
- AWS_ACCESS_KEY_ID=***
- AWS_SECRET_ACCESS_KEY=***
- PROMETHEUS_WRITE_ENDPOINT=https://aps-workspaces.us-east-1.amazonaws.com/workspaces/ws-XXX/api/v1/remote_write
```
Execute:
- docker-compose build
- docker-compose up
- To bring down the stack: docker-compose down

## Endpoints

#### Non-AWS
- [Metrics exposed via otel-collector](http://localhost:8899/metrics)
- [otel-collector metrics](http://localhost:8898/metrics)
- [Grafana Dashboard in local run](http://localhost:3000/d/LQU-Nbank/metrics?orgId=1&from=now-5m&to=now&var-query_param=Stranger) Username/Password: admin (Dashboard should be getting data in 5 min)
- [Jaeger](http://localhost:16686/)

#### AWS
- [Metrics exposed by aws-otel-collector](http://localhost:8889/metrics)
- [aws-otel-collector metrics](http://localhost:8888/metrics)
- AWS Grafana: AWS Console
- XRay: AWS Console
