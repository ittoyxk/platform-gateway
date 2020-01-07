package net.commchina.platform.gateway.monitor;

import io.micrometer.core.instrument.Clock;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @description: callin-core
 * @author: hengxiaokang
 * @time 2020/1/7 13:02
 */

@Configuration
public class MicrometerConfiguration {

    @Value("${spring.application.name}")
    private String application;


    @Bean
    public PrometheusMeterRegistry prometheusMeterRegistry(PrometheusConfig prometheusConfig, CollectorRegistry collectorRegistry, Clock clock)
    {
        PrometheusMeterRegistry prometheusMeterRegistry = new PrometheusMeterRegistry(prometheusConfig, collectorRegistry, clock);
        return prometheusMeterRegistry;
    }

    @Bean()
    MeterRegistryCustomizer meterRegistryCustomizer(PrometheusMeterRegistry meterRegistry)
    {
        return meterRegistry1 -> {
            meterRegistry.config()
                    .commonTags("application", application);
        };
    }
}
