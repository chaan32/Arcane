package com.arcane.worker.kafka.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
    private static final int LONG_JOB_MAX_POLL_INTERVAL_MS = 6 * 60 * 60 * 1000;

    @Bean
    public NewTopic rankingUpdateRequestedTopic(
            @Value("${arcane.kafka.topics.ranking-update-requested}") String topic
    ) {
        return TopicBuilder.name(topic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic rankingUpdateCompletedTopic(
            @Value("${arcane.kafka.topics.ranking-update-completed}") String topic
    ) {
        return TopicBuilder.name(topic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic rankingUpdateFailedTopic(
            @Value("${arcane.kafka.topics.ranking-update-failed}") String topic
    ) {
        return TopicBuilder.name(topic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic rankingUpdateDltTopic(
            @Value("${arcane.kafka.topics.ranking-update-dlt}") String topic
    ) {
        return TopicBuilder.name(topic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic datasetCollectRequestedTopic(
            @Value("${arcane.kafka.topics.dataset-collect-requested}") String topic
    ) {
        return TopicBuilder.name(topic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic datasetCollectCompletedTopic(
            @Value("${arcane.kafka.topics.dataset-collect-completed}") String topic
    ) {
        return TopicBuilder.name(topic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic datasetCollectFailedTopic(
            @Value("${arcane.kafka.topics.dataset-collect-failed}") String topic
    ) {
        return TopicBuilder.name(topic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic datasetCollectDltTopic(
            @Value("${arcane.kafka.topics.dataset-collect-dlt}") String topic
    ) {
        return TopicBuilder.name(topic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public ConsumerFactory<String, String> datasetCollectStringConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id}") String groupId
    ) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId + "-dataset");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, LONG_JOB_MAX_POLL_INTERVAL_MS);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean("datasetCollectKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> datasetCollectKafkaListenerContainerFactory(
            ConsumerFactory<String, String> datasetCollectStringConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(datasetCollectStringConsumerFactory);
        factory.setConcurrency(1);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.getContainerProperties().setSyncCommits(true);
        return factory;
    }

    @Bean
    public NewTopic championAnalysisRequestTopic(@Value("${arcane.kafka.topics.champion-analysis-requested}") String topic){
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic championAnalysisProgressTopic(@Value("${arcane.kafka.topics.champion-analysis-progress}") String topic) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic championAnalysisCompletedTopic(@Value("${arcane.kafka.topics.champion-analysis-completed}")String topic){
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic championAnalysisFailedTopic(@Value("${arcane.kafka.topics.champion-analysis-failed}") String topic) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic championAnalysisDltTopic(@Value("${arcane.kafka.topics.champion-analysis-dlt}") String topic) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic gameDataSyncRequestedTopic(
            @Value("${arcane.kafka.topics.game-data-sync-requested:arcane.game-data.sync.requested}") String topic
    ) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic gameDataSyncCompletedTopic(
            @Value("${arcane.kafka.topics.game-data-sync-completed:arcane.game-data.sync.completed}") String topic
    ) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic gameDataSyncFailedTopic(
            @Value("${arcane.kafka.topics.game-data-sync-failed:arcane.game-data.sync.failed}") String topic
    ) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic gameDataSyncDltTopic(
            @Value("${arcane.kafka.topics.game-data-sync-dlt:arcane.game-data.sync.dlt}") String topic
    ) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }

    @Bean
    public ConsumerFactory<String, String> championAnalysisStringConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers, @Value("${spring.kafka.consumer.group-id}") String groupId
    ){
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId+"-champion-analysis");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, LONG_JOB_MAX_POLL_INTERVAL_MS);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean("championAnalysisKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> championAnalysisKafkaListenerContainerFactory(
            @Qualifier("championAnalysisStringConsumerFactory")
            ConsumerFactory<String, String> championAnalysisStringConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(championAnalysisStringConsumerFactory);
        factory.setConcurrency(1);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.getContainerProperties().setSyncCommits(true);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, String> gameDataSyncStringConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id}") String groupId
    ) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId + "-game-data-sync");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, LONG_JOB_MAX_POLL_INTERVAL_MS);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean("gameDataSyncKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> gameDataSyncKafkaListenerContainerFactory(
            @Qualifier("gameDataSyncStringConsumerFactory")
            ConsumerFactory<String, String> gameDataSyncStringConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(gameDataSyncStringConsumerFactory);
        factory.setConcurrency(1);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.getContainerProperties().setSyncCommits(true);
        return factory;
    }
}
