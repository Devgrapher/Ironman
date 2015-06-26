package com.example.jihun.ironman;

import junit.framework.TestCase;

public class PacketProcessorTest extends TestCase{
    private ArduinoConnector.PacketProcessor packetProcessor
            = new ArduinoConnector.PacketProcessor();
    private static byte[] kEmtpyBytes = {};

    public void testParsePacket() {
        String test1 = "hello#world#";
        String result = packetProcessor.pushPacketFragment(
                test1.getBytes(), test1.getBytes().length);
        assertEquals("hello", result);
        assertEquals("world", packetProcessor.pushPacketFragment(kEmtpyBytes, 0));
        assertEquals("", packetProcessor.pushPacketFragment(kEmtpyBytes, 0));
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

        for(int i = 0; i < cases.length; ++i) {
            String test_case = cases[i][0];
            String expected = cases[i][1];
            String result = packetProcessor.pushPacketFragment(
                    test_case.getBytes(), test_case.getBytes().length);
            assertEquals(expected, result);
        }
    }
}
