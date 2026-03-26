package ru.yandex.practicum.aggregator.serialization;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import java.util.Map;

public class AvroDeserializer<T extends SpecificRecordBase> implements Deserializer<T> {

    private Class<T> targetType;

    public AvroDeserializer() {
        this.targetType = null;
    }

    public AvroDeserializer(Class<T> targetType) {
        this.targetType = targetType;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        if (targetType == null) {
            Object targetClass = configs.get("avro.deserializer.target.type");
            if (targetClass instanceof String) {
                try {
                    //noinspection unchecked
                    this.targetType = (Class<T>) Class.forName((String) targetClass);
                } catch (ClassNotFoundException e) {
                    throw new SerializationException("Failed to load Avro target type: " + targetClass, e);
                }
            }
        }
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        if (targetType == null) {
            throw new SerializationException("Target type not configured for AvroDeserializer");
        }
        try {
            DatumReader<T> reader = new SpecificDatumReader<>(targetType);
            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(data, null);
            return reader.read(null, decoder);
        } catch (Exception e) {
            throw new SerializationException("Error deserializing Avro message for topic: " + topic, e);
        }
    }

    @Override
    public void close() {

    }
}