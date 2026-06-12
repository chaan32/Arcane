package com.arcane.Arcane.common.Kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

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
    public NewTopic aiScoreRequestedTopic(
            @Value("${arcane.kafka.topics.ai-score-requested}") String topic
    ) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic aiScoreCompletedTopic(
            @Value("${arcane.kafka.topics.ai-score-completed}") String topic
    ) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic aiScoreFailedTopic(
            @Value("${arcane.kafka.topics.ai-score-failed}") String topic
    ) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }
}
