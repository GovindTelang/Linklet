
package com.example.URLShortener.config;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    public static final String CLICK_EVENTS_TOPIC = "url-click-events";

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${KAFKA_SSL_TRUSTSTORE_CERTIFICATES:}")
    private String sslTruststoreCertificates;

    @Value("${KAFKA_SECURITY_PROTOCOL:PLAINTEXT}")
    private String securityProtocol;

    @Value("${KAFKA_SASL_MECHANISM:SCRAM-SHA-256}")
    private String saslMechanism;

    @Value("${KAFKA_SASL_USERNAME:}")
    private String saslUsername;

    @Value("${KAFKA_SASL_PASSWORD:}")
    private String saslPassword;

    @Bean
    public NewTopic clickEventsTopic() {
        return TopicBuilder.name(CLICK_EVENTS_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );
        props.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );
        props.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );
        props.put(ProducerConfig.ACKS_CONFIG, "1");

        addSecurityProperties(props);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );
        props.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "analytics-consumer-group"
        );
        props.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );
        props.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );
        props.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        addSecurityProperties(props);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
    kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(1);

        return factory;
    }
 
    private void addSecurityProperties(Map<String, Object> props) {
        props.put(
                CommonClientConfigs.SECURITY_PROTOCOL_CONFIG,
                securityProtocol
        );

        if (securityProtocol.startsWith("SASL_")) {
            if (saslUsername.isBlank() || saslPassword.isBlank()) {
                throw new IllegalStateException(
                        "KAFKA_SASL_USERNAME and KAFKA_SASL_PASSWORD "
                                + "must be configured for SASL authentication."
                );
            }

            props.put(SaslConfigs.SASL_MECHANISM, saslMechanism);
            props.put(
                    SaslConfigs.SASL_JAAS_CONFIG,
                    "org.apache.kafka.common.security.scram.ScramLoginModule "
                            + "required username=\""
                            + escapeJaas(saslUsername)
                            + "\" password=\""
                            + escapeJaas(saslPassword)
                            + "\";"
            );
        }

        if ("SASL_SSL".equalsIgnoreCase(securityProtocol)) {
            if (sslTruststoreCertificates.isBlank()) {
                throw new IllegalStateException(
                        "KAFKA_SSL_TRUSTSTORE_CERTIFICATES must be configured "
                                + "when using SASL_SSL."
                );
            }

            props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PEM");
            props.put(
                    SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG,
                    sslTruststoreCertificates.replace("\\n", "\n")
            );
        }
    }

    private String escapeJaas(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
