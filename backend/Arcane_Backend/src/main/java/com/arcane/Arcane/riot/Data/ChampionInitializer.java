package com.arcane.Arcane.riot.Data;

import com.arcane.Arcane.riot.Data.Champion.Champion;
import com.arcane.Arcane.riot.Data.Champion.ChampionService;
import com.arcane.Arcane.riot.Data.Rune.RuneService;
import com.arcane.Arcane.riot.Data.Rune.domain.Rune;
import com.arcane.Arcane.riot.Data.SummonerSpell.SummonerSpellService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChampionInitializer implements ApplicationRunner {
    private final ChampionService championService;
    private final RuneService runeService;
    private final SummonerSpellService summonerSpellService;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        importChampion();
        importRune();
        importSpell();
    }
    private void importChampion(){
        log.info("[Checking] Existing <Champion> Object In DB");
        Optional<Champion> champion = championService.getChampionById(1L);
        if (champion.isPresent()) {
            log.info("There are Champions in DB");
            return;
        }
        log.info("There are not Champion Objects in DB");

        championService.importChampions();
        log.info("Complete About Inserting Champion Objects");
    }

    private void importRune() throws IOException {
        log.info("[Checking] Existing <Rune> Object In DB");
        Optional<Rune> rune = runeService.findRuneById(1L);
        if (rune.isPresent()){
            log.info("There are Runes in DB");
            return;
        }
        log.info("There are not Rune Objects in DB");
        runeService.importRunes();

        log.info("Complete About Inserting Rune Objects");
    }

    private void importSpell() throws IOException {
        log.info("[Checking] Existing <Spell> Object In DB");

        if (!summonerSpellService.isSpellDbEmpty()){
            log.info("There are Spells in DB");
            return;
        }
        log.info("There are not Spell Objects in DB");
        summonerSpellService.importSummonerSpells();

        log.info("Complete About Inserting Spell Objects");
    }
}
