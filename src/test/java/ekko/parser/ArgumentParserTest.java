package ekko.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Tests argument boundaries, ordering, empty values, and preservation of free text.
 */
class ArgumentParserTest {
    @Test
    void parse_noAllowedArguments_keepsAllText() {
        ParsedArguments parsed = ArgumentParser.parse("  lamp /price 50  ", Set.of());
        assertEquals("lamp /price 50", parsed.getDescription());
        assertFalse(parsed.containsArgument(ArgumentName.PRICE));
    }

    @Test
    void parse_blankInput_returnsEmptyDescriptionAndNoArguments() {
        for (String input : new String[] {"", " \t "}) {
            ParsedArguments parsed = ArgumentParser.parse(input, Set.of(ArgumentName.PRICE));
            assertEquals("", parsed.getDescription());
            assertFalse(parsed.containsArgument(ArgumentName.PRICE));
        }
    }

    @Test
    void parse_noRecognizedArguments_preservesDescription() {
        ParsedArguments parsed = ArgumentParser.parse(" lamp /about Java ", Set.of(ArgumentName.PRICE));
        assertEquals("lamp /about Java", parsed.getDescription());
        assertFalse(parsed.containsArgument(ArgumentName.PRICE));
    }

    @Test
    void parse_singleArgument_trimsValueAndKeepsInternalSpaces() {
        ParsedArguments parsed = ArgumentParser.parse(" vintage  lamp \t/price 50  coins ",
                Set.of(ArgumentName.PRICE));
        assertEquals("vintage  lamp", parsed.getDescription());
        assertEquals("50  coins", parsed.getArgument(ArgumentName.PRICE));
    }

    @Test
    void parse_multipleArguments_acceptsEitherOrder() {
        for (String input : new String[] {
                "lamp /low 10 /high 100",
                "lamp /high 100 /low 10"
        }) {
            ParsedArguments parsed = ArgumentParser.parse(input, Set.of(ArgumentName.LOW, ArgumentName.HIGH));
            assertEquals("lamp", parsed.getDescription());
            assertEquals("10", parsed.getArgument(ArgumentName.LOW));
            assertEquals("100", parsed.getArgument(ArgumentName.HIGH));
        }
    }

    @Test
    void parse_argumentAtStart_allowsEmptyDescription() {
        ParsedArguments parsed = ArgumentParser.parse("/price 50", Set.of(ArgumentName.PRICE));
        assertEquals("", parsed.getDescription());
        assertEquals("50", parsed.getArgument(ArgumentName.PRICE));
    }

    @Test
    void parse_emptyValues_remainPresent() {
        ParsedArguments parsed = ArgumentParser.parse("lamp /low /high", Set.of(ArgumentName.LOW, ArgumentName.HIGH));
        assertTrue(parsed.containsArgument(ArgumentName.LOW));
        assertTrue(parsed.containsArgument(ArgumentName.HIGH));
        assertEquals("", parsed.getArgument(ArgumentName.LOW));
        assertEquals("", parsed.getArgument(ArgumentName.HIGH));
    }

    @Test
    void parse_duplicateArgument_rejectsInput() {
        assertThrows(IllegalArgumentException.class,
                () -> ArgumentParser.parse("lamp /price 10 /price 20", Set.of(ArgumentName.PRICE)));
    }

    @Test
    void parse_duplicateEndingEmpty_rejectsInput() {
        assertThrows(IllegalArgumentException.class,
                () -> ArgumentParser.parse("lamp /price 10 /price", Set.of(ArgumentName.PRICE)));
    }

    @Test
    void parse_onlyArgumentName_keepsEmptyDescriptionAndValue() {
        ParsedArguments parsed = ArgumentParser.parse("/price", Set.of(ArgumentName.PRICE));
        assertEquals("", parsed.getDescription());
        assertTrue(parsed.containsArgument(ArgumentName.PRICE));
        assertEquals("", parsed.getArgument(ArgumentName.PRICE));
    }

    @Test
    void parse_punctuationAfterName_preservesPunctuationInValue() {
        ParsedArguments parsed = ArgumentParser.parse("lamp /low:10 /high-100",
                Set.of(ArgumentName.LOW, ArgumentName.HIGH));
        assertEquals("lamp", parsed.getDescription());
        assertEquals(":10", parsed.getArgument(ArgumentName.LOW));
        assertEquals("-100", parsed.getArgument(ArgumentName.HIGH));
    }

    @Test
    void parse_interleavedDuplicates_rejectsInput() {
        assertThrows(IllegalArgumentException.class, () -> ArgumentParser.parse(
                "lamp /low 10 /high 100 /low 20 /high",
                Set.of(ArgumentName.LOW, ArgumentName.HIGH)));
    }

    @Test
    void parse_unallowedArgumentInValue_preservesIt() {
        ParsedArguments parsed = ArgumentParser.parse("lamp /price 50 /end 2026-09-10", Set.of(ArgumentName.PRICE));
        assertEquals("50 /end 2026-09-10", parsed.getArgument(ArgumentName.PRICE));
        assertFalse(parsed.containsArgument(ArgumentName.END));
    }

    @Test
    void parse_embeddedOrLongerSlashWord_doesNotMatch() {
        for (String input : new String[] {"lamp/price 50", "lamp /pricetag 50", "lamp /price2 50",
                "lamp /price_tag", "lamp /PRICE 50", "https://site/price"}) {
            ParsedArguments parsed = ArgumentParser.parse(input, Set.of(ArgumentName.PRICE));
            assertEquals(input, parsed.getDescription());
            assertFalse(parsed.containsArgument(ArgumentName.PRICE), input);
        }
    }

    @Test
    void parse_allAllowedArguments_extractsEachWithoutChangingAllowedSet() {
        Set<ArgumentName> allowed = Set.of(ArgumentName.DESC, ArgumentName.PRICE, ArgumentName.END);
        ParsedArguments parsed = ArgumentParser.parse("lamp /desc nice item /price 50 /end 2026-09-10", allowed);
        assertEquals("nice item", parsed.getArgument(ArgumentName.DESC));
        assertEquals("50", parsed.getArgument(ArgumentName.PRICE));
        assertEquals("2026-09-10", parsed.getArgument(ArgumentName.END));
        assertEquals(3, allowed.size());
    }
}
