package com.chess.demo.service;

import com.chess.demo.entity.FidePlayer;
import com.chess.demo.repository.FidePlayerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Year;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Import(FideLeaderboardService.class)
class FideLeaderboardServiceTest {

    @Autowired
    private FidePlayerRepository fidePlayerRepository;

    @Autowired
    private FideLeaderboardService fideLeaderboardService;

    @Test
    void filtersByTimeControlCountryGenderAndDivision() {
        int juniorBoundaryBirthYear = Year.now().getValue() - 20;

        fidePlayerRepository.save(player(1, "Magnus Carlsen", "NOR", "M", 1990, 2830, 2820, 2860, false));
        fidePlayerRepository.save(player(2, "Ju Wenjun", "CHN", "F", 1991, 2550, 2542, 2490, false));
        fidePlayerRepository.save(player(3, "Divya Deshmukh", "IND", "F", juniorBoundaryBirthYear, 2470, 2395, 2380, false));
        fidePlayerRepository.save(player(4, "Praggnanandhaa R", "IND", "M", 2005, 2750, 2688, 2734, true));

        var rapidIndianWomen = fideLeaderboardService.getLeaderboard(
                null,
                "rapid",
                "IND",
                "female",
                "junior",
                0,
                10,
                true
        );

        assertEquals(1, rapidIndianWomen.totalEntries());
        assertEquals("Divya Deshmukh", rapidIndianWomen.entries().getFirst().name());
        assertEquals(2395, rapidIndianWomen.entries().getFirst().rating());
        assertEquals(1, rapidIndianWomen.entries().getFirst().rank());
    }

    @Test
    void openGenderQueryIncludesBothSexesAndAssignsRanksWithinPage() {
        fidePlayerRepository.save(player(10, "Player B", "USA", "F", 1987, 2400, 2350, 2300, false));
        fidePlayerRepository.save(player(11, "Player A", "USA", "M", 1985, 2500, 2450, 2400, false));

        var response = fideLeaderboardService.getLeaderboard(
                "player",
                "standard",
                null,
                "open",
                "open",
                0,
                10,
                true
        );

        assertEquals(2, response.totalEntries());
        assertEquals("Player A", response.entries().get(0).name());
        assertEquals("Player B", response.entries().get(1).name());
        assertEquals(1, response.entries().get(0).rank());
        assertEquals(2, response.entries().get(1).rank());
    }

    private FidePlayer player(int fideId,
                              String name,
                              String federation,
                              String sex,
                              int birthYear,
                              int standardRating,
                              int rapidRating,
                              int blitzRating,
                              boolean rapidInactive) {
        FidePlayer player = new FidePlayer();
        player.setFideId(fideId);
        player.setName(name);
        player.setFederation(federation);
        player.setSex(sex);
        player.setBirthYear(birthYear);
        player.setStandardRating(standardRating);
        player.setRapidRating(rapidRating);
        player.setBlitzRating(blitzRating);
        player.setStandardInactive(false);
        player.setRapidInactive(rapidInactive);
        player.setBlitzInactive(false);
        return player;
    }
}
