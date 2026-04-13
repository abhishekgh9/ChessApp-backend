package com.chess.demo.service;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FidePlayerXmlParserTest {

    private final FidePlayerXmlParser parser = new FidePlayerXmlParser();

    @Test
    void parsesPlayerRowsFromXml() {
        String xml = """
                <players>
                  <player>
                    <fideid>1503014</fideid>
                    <name>Carlsen, Magnus</name>
                    <country>NOR</country>
                    <sex>M</sex>
                    <title>GM</title>
                    <birthday>1990</birthday>
                    <rating>2832</rating>
                    <games>9</games>
                    <k>10</k>
                    <flag></flag>
                  </player>
                  <player>
                    <fideid>8602980</fideid>
                    <name>Ju, Wenjun</name>
                    <country>CHN</country>
                    <sex>F</sex>
                    <title>GM</title>
                    <birthday>1991</birthday>
                    <rating>2542</rating>
                    <games>11</games>
                    <k>10</k>
                    <flag>inactive</flag>
                  </player>
                </players>
                """;

        List<FidePlayerXmlParser.FidePlayerXmlRecord> records = parser.parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))
        );

        assertEquals(2, records.size());
        assertEquals(1503014, records.get(0).fideId());
        assertEquals("Carlsen, Magnus", records.get(0).name());
        assertEquals("NOR", records.get(0).federation());
        assertEquals(2832, records.get(0).standardRating());
        assertFalse(records.get(0).standardInactive());
        assertEquals(8602980, records.get(1).fideId());
        assertEquals("F", records.get(1).sex());
        assertEquals(2542, records.get(1).standardRating());
        assertEquals(11, records.get(1).standardGames());
        assertEquals(10, records.get(1).standardK());
        assertEquals(true, records.get(1).standardInactive());
        assertFalse(records.get(1).rapidInactive());
    }

    @Test
    void streamsPlayerRowsToConsumer() {
        String xml = """
                <players>
                  <player>
                    <fideid>1503014</fideid>
                    <name>Carlsen, Magnus</name>
                    <std_rating>2832</std_rating>
                  </player>
                  <player>
                    <fideid>8602980</fideid>
                    <name>Ju, Wenjun</name>
                    <rapid_rating>2542</rapid_rating>
                  </player>
                </players>
                """;

        List<FidePlayerXmlParser.FidePlayerXmlRecord> records = new ArrayList<>();
        int count = parser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), records::add);

        assertEquals(2, count);
        assertEquals(2, records.size());
        assertEquals("Ju, Wenjun", records.get(1).name());
        assertEquals(2542, records.get(1).rapidRating());
    }
}
