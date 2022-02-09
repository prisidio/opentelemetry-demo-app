package com.example.dropwizard;

import com.codahale.metrics.annotation.Timed;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

@Path("/hello-world")
@Produces(MediaType.APPLICATION_JSON)
public class HelloWorldResource {
    private static final Logger log = LoggerFactory.getLogger(HelloWorldResource.class.getName());
    private final String template;
    private final String defaultName;
    private final AtomicLong counter;
    private final LongHistogram apiLatencyRecorder;
    private final LongCounter apiInvocationCounter;
    private final Tracer tracer;
    private final Client client;

    public HelloWorldResource(String template, String defaultName) {
        this.template = template;
        this.defaultName = defaultName;
        this.counter = new AtomicLong();
        Meter meter = GlobalOpenTelemetry.meterBuilder("somename").setInstrumentationVersion("1.0").build();
        tracer = GlobalOpenTelemetry.getTracer("somename", "1.0");
        this.apiLatencyRecorder = meter
                .histogramBuilder("api_latency")
                .ofLongs()
                .setDescription("API latency time in milliseconds.")
                .setUnit("ms")
                .build();
        this.apiInvocationCounter = meter
                .counterBuilder("api_invocation")
                .setDescription("API Invocation counter for testing exmplar")
                .build();
        this.client = ClientBuilder.newClient();
    }

    @GET
    @Timed
    public Saying sayHello(@QueryParam("name") Optional<String> name) {
        log.info("Serving request for [{}]", name.orElse(defaultName));
        SpanBuilder span = tracer.spanBuilder("random-sleep");
        long startTime = System.currentTimeMillis();
        try {
            final String value = String.format(template, name.orElse(defaultName));

            Span s = span.startSpan();
            sleepToGenerateRandomSpanLengths();
            //Generate error to see exception information in tracing system
            if (name.isPresent() && "error".equals(name.get())) {
                generateError(s);
            }
            s.end();

            callAnotherApiToGenerateHttpClientMetrics(name);

            return new Saying(counter.incrementAndGet(), value);
        } finally {
            apiInvocationCounter.add(1, Attributes.of(AttributeKey.stringKey("api-param"), name.orElse(defaultName)), Context.root());
            apiLatencyRecorder.record(System.currentTimeMillis() - startTime,
                    Attributes.of(AttributeKey.stringKey("api-param"), name.orElse(defaultName)),
                    Context.root());
        }
    }

    private void callAnotherApiToGenerateHttpClientMetrics(Optional<String> name) {
        if (name.isPresent() && "call".equals(name.get()) && StringUtils.isNotBlank(System.getenv("EXTERNAL_ENDPOINT"))) {
            client.target(System.getenv("EXTERNAL_ENDPOINT")).request().get(String.class);
            log.info("External API called.");
        }
    }

    private void sleepToGenerateRandomSpanLengths() {
        Random rand = new Random();
        // Generate random integers in range 0 to 999
        int rand_int1 = rand.nextInt(1000);
        try {
            Thread.sleep(rand_int1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void generateError(Span s) {
        try {
            int i = 1 / 0;
        } catch (Exception e) {
            e.printStackTrace();
            s.recordException(e);
        }
    }
}
