package com.chess.demo.service;

import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class FidePlayerXmlParser {

    public List<FidePlayerXmlRecord> parse(InputStream inputStream) {
        List<FidePlayerXmlRecord> records = new ArrayList<>();
        parse(inputStream, records::add);
        return records;
    }

    public int parse(InputStream inputStream, Consumer<FidePlayerXmlRecord> consumer) {
        try {
            XMLInputFactory factory = XMLInputFactory.newFactory();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

            XMLStreamReader reader = factory.createXMLStreamReader(inputStream);
            int recordCount = 0;

            while (reader.hasNext()) {
                if (reader.next() == XMLStreamConstants.START_ELEMENT && looksLikePlayerElement(reader.getLocalName())) {
                    FidePlayerXmlRecord record = parsePlayer(reader);
                    if (record != null) {
                        consumer.accept(record);
                        recordCount++;
                    }
                }
            }

            reader.close();
            return recordCount;
        } catch (Exception exception) {
            throw new IllegalArgumentException("fide_xml_parse_failed", exception);
        }
    }

    private boolean looksLikePlayerElement(String localName) {
        String normalized = normalizeTag(localName);
        return normalized.equals("player")
                || normalized.equals("plr")
                || normalized.equals("row")
                || normalized.equals("playerrow");
    }

    private FidePlayerXmlRecord parsePlayer(XMLStreamReader reader) throws XMLStreamException {
        Map<String, String> values = new LinkedHashMap<>();
        String elementName = normalizeTag(reader.getLocalName());

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String childName = normalizeTag(reader.getLocalName());
                values.put(childName, reader.getElementText().trim());
            } else if (event == XMLStreamConstants.END_ELEMENT && normalizeTag(reader.getLocalName()).equals(elementName)) {
                break;
            }
        }

        Integer fideId = intValue(values, "fideid", "fide_id", "id", "fide");
        String name = stringValue(values, "name", "player");
        Integer standardRating = intValue(values, "std_rating", "srtng", "standard_rating");
        Integer rapidRating = intValue(values, "rapid_rating", "rrtng", "rpd_rating");
        Integer blitzRating = intValue(values, "blitz_rating", "brtng", "blz_rating");
        Integer legacyRating = intValue(values, "rating");

        if (standardRating == null && rapidRating == null && blitzRating == null) {
            standardRating = legacyRating;
        }

        if (fideId == null || name == null || (standardRating == null && rapidRating == null && blitzRating == null)) {
            return null;
        }

        return new FidePlayerXmlRecord(
                fideId,
                name,
                stringValue(values, "title", "w_title", "o_title"),
                stringValue(values, "country", "fed", "federation"),
                stringValue(values, "sex", "gender"),
                intValue(values, "birthday", "birth_year", "birthyear", "year", "born", "bday"),
                standardRating,
                rapidRating,
                blitzRating,
                intValue(values, "sgm", "standard_games", "games", "games_played"),
                intValue(values, "rgm", "rapid_games"),
                intValue(values, "bgm", "blitz_games"),
                intValue(values, "sk", "standard_k", "k", "kfactor", "k_factor"),
                intValue(values, "rk", "rapid_k"),
                intValue(values, "bk", "blitz_k"),
                inactiveFor(values, "standard"),
                inactiveFor(values, "rapid"),
                inactiveFor(values, "blitz")
        );
    }

    private String normalizeTag(String tagName) {
        return tagName == null ? "" : tagName.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private String stringValue(Map<String, String> values, String... keys) {
        for (String key : keys) {
            String value = values.get(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private Integer intValue(Map<String, String> values, String... keys) {
        String value = stringValue(values, keys);
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("[^0-9-]", "");
        if (digits.isBlank() || "-".equals(digits)) {
            return null;
        }
        return Integer.parseInt(digits);
    }

    private boolean booleanValue(Map<String, String> values, String... keys) {
        String value = stringValue(values, keys);
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("inactive")
                || normalized.equals("i")
                || normalized.equals("true")
                || normalized.equals("1");
    }

    private boolean inactiveFor(Map<String, String> values, String timeControl) {
        String flag = stringValue(values, "flag", "inactive");
        if (flag == null || flag.isBlank()) {
            return false;
        }
        String normalized = flag.trim().toLowerCase(Locale.ROOT);
        if (booleanValue(values, "inactive")) {
            return true;
        }
        return switch (timeControl) {
            case "rapid" -> normalized.contains("r");
            case "blitz" -> normalized.contains("b");
            default -> normalized.equals("i") || normalized.contains("inactive");
        };
    }

    public record FidePlayerXmlRecord(
            Integer fideId,
            String name,
            String title,
            String federation,
            String sex,
            Integer birthYear,
            Integer standardRating,
            Integer rapidRating,
            Integer blitzRating,
            Integer standardGames,
            Integer rapidGames,
            Integer blitzGames,
            Integer standardK,
            Integer rapidK,
            Integer blitzK,
            boolean standardInactive,
            boolean rapidInactive,
            boolean blitzInactive
    ) {
    }
}
