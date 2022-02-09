package com.example.dropwizard;

import com.example.dropwizard.configuration.AppConfiguration;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class DropWizardDriver extends Application<AppConfiguration> {
    private static final Logger log = LoggerFactory.getLogger(DropWizardDriver.class.getName());

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            String configPath = Objects.requireNonNull(DropWizardDriver.class.getClassLoader().getResource("config.yml")).getPath();
            new DropWizardDriver().run("server", configPath);
        } else {
            new DropWizardDriver().run(args);
        }
    }

    @Override
    public String getName() {
        return "hello-world";
    }

    @Override
    public void run(AppConfiguration configuration, Environment environment) {
        final HelloWorldResource resource = new HelloWorldResource(
                configuration.getTemplate(),
                configuration.getDefaultName()
        );
        environment.jersey().register(resource);
    }

    @Override
    public void initialize(Bootstrap<AppConfiguration> bootstrap) {
        // nothing to do yet
    }
}
