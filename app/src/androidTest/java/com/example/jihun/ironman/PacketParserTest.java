package com.example.jihun.ironman;

import com.example.jihun.ironman.arduino.PacketParser;

import junit.framework.TestCase;

public class PacketParserTest extends TestCase{
    private PacketParser parser = new PacketParser();

    public void testParsePacket() {
        String test1 = "hello#world#";
        boolean result = parser.pushPacketFragment(
                test1.getBytes(), test1.getBytes().length);
        assertTrue(result);
        assertEquals("hello", parser.popPacket());
        assertEquals("world", parser.popPacket());
        assertEquals("", parser.popPacket());
    }

    public void testFragmentPacket() {
        String[][] cases = {
                {"hello", ""}, // input, expected result
                {"#", "hello"},
                {"", ""},
                {"a series#of#packets#", "a series"},
                {"", "of"},
                {"", "packets"},
                {"", ""}
        };

        for(String[] entry : cases) {
            String test_case = entry[0];
            String expected = entry[1];
            parser.pushPacketFragment(test_case.getBytes(), test_case.getBytes().length);
            assertEquals(expected, parser.popPacket());
        }
    }
}
