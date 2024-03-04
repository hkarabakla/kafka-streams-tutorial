package com.hkarabakla.kafkastreamstutorial.config;

import com.hkarabakla.kafkastreamstutorial.dto.Department;
import com.hkarabakla.kafkastreamstutorial.dto.JoinedValue;
import com.hkarabakla.kafkastreamstutorial.dto.MyEvent;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

@Configuration
public class KafkaStreamConfig {

    @Bean
    public Supplier<Message<MyEvent>> producer() {
        return () -> {
            Department department = Department.values()[new Random().nextInt(Department.values().length)];
            MyEvent myEvent = new MyEvent("Huseyin", department);
            return MessageBuilder.withPayload(myEvent)
                    .setHeader(KafkaHeaders.KEY, department.name())
                    .build();
        };

    }

    @Bean
    public Function<KStream<String, MyEvent>, KStream<String, String>> enhancer() {
        return input -> input
                 .peek((k, v) -> System.out.println("Enhancer " + k + " " + v))
                .mapValues(value -> value.name());
    }


    @Bean
    public Function<KStream<String, MyEvent>, KStream<String, String>> aggregate() {
        return input -> input
                .peek((key, value) -> System.out.println("Aggregate->" + key + " " + value))
                .groupByKey()
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(10)))
                .aggregate(() -> 0l,
                        (key, value, aggregate) -> aggregate + 1,
                        Materialized.with(Serdes.String(), Serdes.Long()))

                .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
                .toStream()
                .map((w, v) -> new KeyValue<>(w.key(), v.toString()))
                .peek((k, v) -> System.out.println("Aggregate " + k + " " + v));
    }

    @Bean
    public BiFunction<KStream<String, String>, KStream<String, String>, KStream<String, JoinedValue>> join() {
        return (input1, input2) -> input1.join(input2,
                        (value1, valu2) -> new JoinedValue(value1, valu2),
                        JoinWindows.ofTimeDifferenceWithNoGrace(Duration.of(10, ChronoUnit.SECONDS))
                        , StreamJoined.with(Serdes.String(), Serdes.String(), Serdes.String())

                )
                .peek((key, value) -> System.out.println("joined ->" + key + " " + value));

    }
}
