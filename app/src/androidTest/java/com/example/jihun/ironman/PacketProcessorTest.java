package com.example.jihun.ironman;

import junit.framework.TestCase;

import java.util.ArrayList;

/**
 * Created by Jihun on 2015-06-14.
 */
public class PacketProcessorTest extends TestCase{
    protected ArduinoConnector.PacketProcessor packetProcessor
            = new ArduinoConnector.PacketProcessor();

    public void testParsePacket() {
        String test1 = new String("#05#hello");
        String result = packetProcessor.pushPacketFragment(
                test1.getBytes(), test1.getBytes().length);
        assertEquals(result, "hello");
    }

    public void testInvalidCase() {
        ArrayList<String> cases = new ArrayList<String>();
        cases.add(new String("#dd#hello"));
        cases.add(new String("2345#help"));
        cases.add(new String("no format"));
        cases.add(new String(""));

        for(String data : cases){
            String result = packetProcessor.pushPacketFragment(
                    data.getBytes(), data.getBytes().length);
            assertNull(result);
        }
    }

    public void testPendingCase() {
        ArrayList<String> cases = new ArrayList<String>();
        cases.add(new String("#10#format"));
        cases.add(new String("#10#"));

        for(String data : cases){
            int result = packetProcessor.parsePacket(data.getBytes());
            assertEquals(result, ArduinoConnector.PacketProcessor.kPacketPending);
        }
    }
}
