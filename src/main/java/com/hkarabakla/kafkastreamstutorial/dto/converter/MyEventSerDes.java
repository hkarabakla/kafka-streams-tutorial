package com.hkarabakla.kafkastreamstutorial.dto.converter;

import com.hkarabakla.kafkastreamstutorial.dto.MyEvent;
import org.springframework.kafka.support.serializer.JsonSerde;

public class MyEventSerDes extends JsonSerde<MyEvent> {
}
