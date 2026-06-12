package com.arcane.worker.gamedata.service;

import com.arcane.worker.common.logging.WorkerLogSupport;
import com.arcane.worker.redis.RedisService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameDataSyncService {
    private static final String DDRAGON_BASE_URL = "https://ddragon.leagueoflegends.com";

    private final JdbcTemplate jdbcTemplate;
    private final RedisService redisService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public SyncResult syncLatestGameData() {
        ensureSchema();

        String version = fetchLatestVersion();
        log.info(logMessage(
                "GameDataSyncService.syncLatestGameData",
                "동기화 시작",
                "version=" + version
        ));

        int championCount = syncChampions(version);
        int summonerSpellCount = syncSummonerSpells(version);
        int runeCount = syncRunes(version);
        int itemCount = syncItems(version);

        redisService.storeCurrentPatchVersionAtRedis(version);

        log.info(logMessage(
                "GameDataSyncService.syncLatestGameData",
                "동기화 완료",
                "version=" + version
                        + " | champions=" + championCount
                        + " | items=" + itemCount
                        + " | summonerSpells=" + summonerSpellCount
                        + " | runes=" + runeCount
        ));

        return new SyncResult(
                version,
                championCount,
                itemCount,
                summonerSpellCount,
                runeCount
        );
    }

    private String fetchLatestVersion() {
        JsonNode versions = restTemplate.getForObject(
                DDRAGON_BASE_URL + "/api/versions.json",
                JsonNode.class
        );

        if (versions == null || !versions.isArray() || versions.isEmpty()) {
            throw new IllegalStateException("Data Dragon latest version response is empty.");
        }

        return versions.get(0).asText();
    }

    private int syncChampions(String version) {
        JsonNode championList = getJson(dataUrl(version, "champion.json"));
        JsonNode data = championList.path("data");
        if (!data.isObject()) {
            throw new IllegalStateException("Data Dragon champion data is empty. version=" + version);
        }

        jdbcTemplate.update(
                "UPDATE champion SET is_active = false, updated_at = ? WHERE version IS NULL OR version <> ?",
                LocalDateTime.now(),
                version
        );

        int count = 0;
        for (Iterator<Map.Entry<String, JsonNode>> it = data.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            JsonNode detail = getJson(dataUrl(version, "champion/" + entry.getKey() + ".json"))
                    .path("data")
                    .path(entry.getKey());

            if (detail.isMissingNode() || detail.isNull()) {
                continue;
            }

            upsertChampion(version, detail);
            replaceChampionSpells(version, detail);
            count++;
        }

        return count;
    }

    private void upsertChampion(String version, JsonNode champion) {
        JsonNode info = champion.path("info");
        JsonNode stats = champion.path("stats");
        JsonNode passive = champion.path("passive");

        jdbcTemplate.update("""
                        INSERT INTO champion (
                            id,
                            name_en,
                            name_ko,
                            title,
                            blurb,
                            image_full,
                            tags,
                            version,
                            is_active,
                            updated_at,
                            passive_name,
                            passive_description,
                            passive_image,
                            attack,
                            defense,
                            magic,
                            difficulty,
                            hp,
                            hp_per_level,
                            mp,
                            mp_per_level,
                            move_speed,
                            armor,
                            armor_per_level,
                            spell_block,
                            spell_block_per_level,
                            attack_range,
                            hp_regen,
                            hp_regen_per_level,
                            mp_regen,
                            mp_regen_per_level,
                            crit,
                            crit_per_level,
                            attack_damage,
                            attack_damage_per_level,
                            attack_speed_per_level,
                            attack_speed
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, true, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            name_en = VALUES(name_en),
                            name_ko = VALUES(name_ko),
                            title = VALUES(title),
                            blurb = VALUES(blurb),
                            image_full = VALUES(image_full),
                            tags = VALUES(tags),
                            version = VALUES(version),
                            is_active = true,
                            updated_at = VALUES(updated_at),
                            passive_name = VALUES(passive_name),
                            passive_description = VALUES(passive_description),
                            passive_image = VALUES(passive_image),
                            attack = VALUES(attack),
                            defense = VALUES(defense),
                            magic = VALUES(magic),
                            difficulty = VALUES(difficulty),
                            hp = VALUES(hp),
                            hp_per_level = VALUES(hp_per_level),
                            mp = VALUES(mp),
                            mp_per_level = VALUES(mp_per_level),
                            move_speed = VALUES(move_speed),
                            armor = VALUES(armor),
                            armor_per_level = VALUES(armor_per_level),
                            spell_block = VALUES(spell_block),
                            spell_block_per_level = VALUES(spell_block_per_level),
                            attack_range = VALUES(attack_range),
                            hp_regen = VALUES(hp_regen),
                            hp_regen_per_level = VALUES(hp_regen_per_level),
                            mp_regen = VALUES(mp_regen),
                            mp_regen_per_level = VALUES(mp_regen_per_level),
                            crit = VALUES(crit),
                            crit_per_level = VALUES(crit_per_level),
                            attack_damage = VALUES(attack_damage),
                            attack_damage_per_level = VALUES(attack_damage_per_level),
                            attack_speed_per_level = VALUES(attack_speed_per_level),
                            attack_speed = VALUES(attack_speed)
                        """,
                longValue(champion, "key"),
                text(champion, "id"),
                text(champion, "name"),
                text(champion, "title"),
                text(champion, "blurb"),
                text(champion.path("image"), "full"),
                joinTags(champion.path("tags")),
                version,
                LocalDateTime.now(),
                text(passive, "name"),
                text(passive, "description"),
                text(passive.path("image"), "full"),
                byteValue(info, "attack"),
                byteValue(info, "defense"),
                byteValue(info, "magic"),
                byteValue(info, "difficulty"),
                doubleValue(stats, "hp"),
                doubleValue(stats, "hpperlevel"),
                doubleValue(stats, "mp"),
                doubleValue(stats, "mpperlevel"),
                doubleValue(stats, "movespeed"),
                doubleValue(stats, "armor"),
                doubleValue(stats, "armorperlevel"),
                doubleValue(stats, "spellblock"),
                doubleValue(stats, "spellblockperlevel"),
                doubleValue(stats, "attackrange"),
                doubleValue(stats, "hpregen"),
                doubleValue(stats, "hpregenperlevel"),
                doubleValue(stats, "mpregen"),
                doubleValue(stats, "mpregenperlevel"),
                doubleValue(stats, "crit"),
                doubleValue(stats, "critperlevel"),
                doubleValue(stats, "attackdamage"),
                doubleValue(stats, "attackdamageperlevel"),
                doubleValue(stats, "attackspeedperlevel"),
                doubleValue(stats, "attackspeed")
        );
    }

    private void replaceChampionSpells(String version, JsonNode champion) {
        long championId = longValue(champion, "key");
        String[] spellKeys = {"Q", "W", "E", "R"};

        jdbcTemplate.update("DELETE FROM champion_spell WHERE champion_key = ?", championId);

        JsonNode spells = champion.path("spells");
        if (!spells.isArray()) {
            return;
        }

        for (int i = 0; i < spells.size() && i < spellKeys.length; i++) {
            JsonNode spell = spells.get(i);
            jdbcTemplate.update("""
                            INSERT INTO champion_spell (
                                champion_key,
                                spell_id,
                                name,
                                description,
                                spell_key,
                                image_full,
                                cooldown_burn,
                                cost_burn,
                                version,
                                is_active,
                                updated_at
                            )
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, true, ?)
                            """,
                    championId,
                    text(spell, "id"),
                    text(spell, "name"),
                    text(spell, "description"),
                    spellKeys[i],
                    text(spell.path("image"), "full"),
                    text(spell, "cooldownBurn"),
                    text(spell, "costBurn"),
                    version,
                    LocalDateTime.now()
            );
        }
    }

    private int syncSummonerSpells(String version) {
        JsonNode data = getJson(dataUrl(version, "summoner.json")).path("data");
        if (!data.isObject()) {
            throw new IllegalStateException("Data Dragon summoner spell data is empty. version=" + version);
        }

        jdbcTemplate.update(
                "UPDATE smmr_spell SET is_active = false, updated_at = ? WHERE version IS NULL OR version <> ?",
                LocalDateTime.now(),
                version
        );

        int count = 0;
        for (Iterator<Map.Entry<String, JsonNode>> it = data.fields(); it.hasNext(); ) {
            JsonNode spell = it.next().getValue();
            jdbcTemplate.update("""
                            INSERT INTO smmr_spell (
                                spell_id,
                                name,
                                description,
                                image_full,
                                version,
                                is_active,
                                updated_at
                            )
                            VALUES (?, ?, ?, ?, ?, true, ?)
                            ON DUPLICATE KEY UPDATE
                                name = VALUES(name),
                                description = VALUES(description),
                                image_full = VALUES(image_full),
                                version = VALUES(version),
                                is_active = true,
                                updated_at = VALUES(updated_at)
                            """,
                    longValue(spell, "key"),
                    text(spell, "name"),
                    text(spell, "description"),
                    DDRAGON_BASE_URL + "/cdn/" + version + "/img/spell/" + text(spell.path("image"), "full"),
                    version,
                    LocalDateTime.now()
            );
            count++;
        }

        return count;
    }

    private int syncRunes(String version) {
        JsonNode root = getJson(dataUrl(version, "runesReforged.json"));
        if (!root.isArray()) {
            throw new IllegalStateException("Data Dragon rune data is empty. version=" + version);
        }

        jdbcTemplate.update("DELETE FROM rune");
        jdbcTemplate.update("DELETE FROM rune_slot");
        jdbcTemplate.update("DELETE FROM rune_path");

        int runeCount = 0;
        for (JsonNode path : root) {
            jdbcTemplate.update("""
                            INSERT INTO rune_path (
                                id,
                                rune_key,
                                name,
                                icon,
                                version,
                                is_active,
                                updated_at
                            )
                            VALUES (?, ?, ?, ?, ?, true, ?)
                            """,
                    longValue(path, "id"),
                    text(path, "key"),
                    text(path, "name"),
                    DDRAGON_BASE_URL + "/cdn/img/" + text(path, "icon"),
                    version,
                    LocalDateTime.now()
            );

            JsonNode slots = path.path("slots");
            if (!slots.isArray()) {
                continue;
            }

            for (JsonNode slotNode : slots) {
                long slotId = insertRuneSlot(longValue(path, "id"));
                JsonNode runes = slotNode.path("runes");
                if (!runes.isArray()) {
                    continue;
                }

                for (JsonNode rune : runes) {
                    jdbcTemplate.update("""
                                    INSERT INTO rune (
                                        id,
                                        rune_key,
                                        name,
                                        icon,
                                        short_desc,
                                        version,
                                        is_active,
                                        updated_at,
                                        slot_id
                                    )
                                    VALUES (?, ?, ?, ?, ?, ?, true, ?, ?)
                                    """,
                            longValue(rune, "id"),
                            text(rune, "key"),
                            text(rune, "name"),
                            DDRAGON_BASE_URL + "/cdn/img/" + text(rune, "icon"),
                            text(rune, "shortDesc"),
                            version,
                            LocalDateTime.now(),
                            slotId
                    );
                    runeCount++;
                }
            }
        }

        return runeCount;
    }

    private long insertRuneSlot(long runePathId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO rune_slot (rune_path_id) VALUES (?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, runePathId);
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create rune_slot. runePathId=" + runePathId);
        }
        return key.longValue();
    }

    private int syncItems(String version) {
        JsonNode data = getJson(dataUrl(version, "item.json")).path("data");
        if (!data.isObject()) {
            throw new IllegalStateException("Data Dragon item data is empty. version=" + version);
        }

        jdbcTemplate.update(
                "UPDATE riot_item_metadata SET is_active = false, updated_at = ? WHERE version IS NULL OR version <> ?",
                LocalDateTime.now(),
                version
        );

        int count = 0;
        for (Iterator<Map.Entry<String, JsonNode>> it = data.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            JsonNode item = entry.getValue();

            jdbcTemplate.update("""
                            INSERT INTO riot_item_metadata (
                                item_id,
                                version,
                                name,
                                description,
                                image_full,
                                is_active,
                                updated_at
                            )
                            VALUES (?, ?, ?, ?, ?, true, ?)
                            ON DUPLICATE KEY UPDATE
                                name = VALUES(name),
                                description = VALUES(description),
                                image_full = VALUES(image_full),
                                is_active = true,
                                updated_at = VALUES(updated_at)
                            """,
                    Integer.parseInt(entry.getKey()),
                    version,
                    text(item, "name"),
                    text(item, "description"),
                    text(item.path("image"), "full"),
                    LocalDateTime.now()
            );
            count++;
        }

        return count;
    }

    private JsonNode getJson(String url) {
        JsonNode json = restTemplate.getForObject(url, JsonNode.class);
        if (json == null) {
            throw new IllegalStateException("Data Dragon response is empty. url=" + url);
        }
        return json;
    }

    private String dataUrl(String version, String path) {
        return DDRAGON_BASE_URL + "/cdn/" + version + "/data/ko_KR/" + path;
    }

    private void ensureSchema() {
        ensureColumn("match_info", "game_version", "VARCHAR(40) NULL");
        ensureColumn("champion", "version", "VARCHAR(30) NULL");
        ensureColumn("champion", "is_active", "BOOLEAN NOT NULL DEFAULT TRUE");
        ensureColumn("champion", "updated_at", "DATETIME(6) NULL");
        ensureColumn("champion_spell", "version", "VARCHAR(30) NULL");
        ensureColumn("champion_spell", "is_active", "BOOLEAN NOT NULL DEFAULT TRUE");
        ensureColumn("champion_spell", "updated_at", "DATETIME(6) NULL");
        ensureColumn("smmr_spell", "version", "VARCHAR(30) NULL");
        ensureColumn("smmr_spell", "is_active", "BOOLEAN NOT NULL DEFAULT TRUE");
        ensureColumn("smmr_spell", "updated_at", "DATETIME(6) NULL");
        ensureColumn("rune_path", "version", "VARCHAR(30) NULL");
        ensureColumn("rune_path", "is_active", "BOOLEAN NOT NULL DEFAULT TRUE");
        ensureColumn("rune_path", "updated_at", "DATETIME(6) NULL");
        ensureColumn("rune", "version", "VARCHAR(30) NULL");
        ensureColumn("rune", "is_active", "BOOLEAN NOT NULL DEFAULT TRUE");
        ensureColumn("rune", "updated_at", "DATETIME(6) NULL");
        createItemMetadataTable();
    }

    private void ensureColumn(String tableName, String columnName, String definition) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        } catch (DataAccessException ignored) {
            // 이미 존재하는 컬럼이면 그대로 진행한다.
        }
    }

    private void createItemMetadataTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS riot_item_metadata (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    item_id INT NOT NULL,
                    version VARCHAR(30) NOT NULL,
                    name VARCHAR(255) NOT NULL,
                    description LONGTEXT NULL,
                    image_full VARCHAR(255) NULL,
                    is_active BOOLEAN NOT NULL DEFAULT TRUE,
                    updated_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_riot_item_metadata_item_version (item_id, version),
                    INDEX idx_riot_item_metadata_item_id (item_id),
                    INDEX idx_riot_item_metadata_version_active (version, is_active)
                )
                """);
    }

    private String joinTags(JsonNode tags) {
        if (!tags.isArray()) {
            return "";
        }

        List<String> values = new ArrayList<>();
        for (JsonNode tag : tags) {
            values.add(tag.asText());
        }
        return String.join(",", values);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private long longValue(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null || value.isBlank()) {
            return 0L;
        }
        return Long.parseLong(value);
    }

    private Byte byteValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return (byte) value.asInt();
    }

    private Double doubleValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asDouble();
    }

    private String logMessage(String method, String status, String detail) {
        return WorkerLogSupport.log("게임 데이터 동기화", method, status, detail);
    }

    public record SyncResult(
            String version,
            Integer championCount,
            Integer itemCount,
            Integer summonerSpellCount,
            Integer runeCount
    ) {
    }
}
