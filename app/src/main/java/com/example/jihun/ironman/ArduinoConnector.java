package com.example.jihun.ironman;

import android.bluetooth.BluetoothDevice;

import java.io.UnsupportedEncodingException;

/**
 * Communicate with Arduino Device using bluetooth.
 *
 * Responsible for managing packets and implementing the protocol.
 */
public class ArduinoConnector {
    private BluetoothSerial bluetooth_;
    private Listener listener_;

    public enum Reactions {
        ActivityDetected,
    }

    /**
     * Notifies the arduino connection status
     */
    public interface Listener {
        void onConnect(BluetoothDevice device);
        void onReaction(Reactions reaction, String data);
        void onDisconnect(BluetoothDevice device);
    }

    public ArduinoConnector(Listener listener) {
        listener_ = listener;
    }

    public void connect(BluetoothDevice device) {
        if (bluetooth_ != null) {
            disconnect();
        }
        bluetooth_ = new BluetoothSerial();
        bluetooth_.askConnect(device, bluetooth_listener_);
    }

    public boolean isConnected() {
        return bluetooth_.isConnected();
    }

    public void disconnect() {
        bluetooth_.cancel();
        bluetooth_ = null;
    }

    public void send(String command) {
        String packet = PacketProcessor.createPacket(command);
        bluetooth_.Write(packet.getBytes());
    }

    public void destroy() {
        if (bluetooth_ != null) {
            bluetooth_.cancel();
        }
    }

    protected  BluetoothSerial.Listener bluetooth_listener_ = new BluetoothSerial.Listener() {
        private PacketProcessor packetProcessor_ = new PacketProcessor();

        @Override
        public void onConnect(BluetoothDevice device) {
            String initial_msg = "hi";
            String packet = PacketProcessor.createPacket(initial_msg);
            bluetooth_.Write(packet.getBytes());
            listener_.onConnect(device);
        }

        @Override
        public void onRead(BluetoothDevice device, byte[] data, int len) {
            String packet = packetProcessor_.pushPacketFragment(data, len);
            if (packet.isEmpty()) {
                return;
            }

            PacketProcessor.ParseResult result = packetProcessor_.parsePacket(packet);
            if (result != null) {
                listener_.onReaction(result.reaction, result.param);
            }
        }

        @Override
        public void onDisconnect(BluetoothDevice device) {
            listener_.onDisconnect(device);
        }
    };

    /**
     *  Create and parse packet.
     *  Packet is a simple string that ends with '#'
     */
    public static class PacketProcessor {
        private String packet_ = "";
        private static final String kPacketDelimiter = "#";

        // The reasult after parsing packet body.
        public class ParseResult {
            public final Reactions reaction;
            public final String param;

            public ParseResult() {
                reaction = null;
                param = null;
            }

            public ParseResult(Reactions reaction, String param) {
                this.reaction = reaction;
                this.param = param;
            }
        }

        // Push a packet fragment into the packet buffer,
        // and return a complete packet if it's available.
        public String pushPacketFragment(byte[] fragment, int len) {
            try {
                packet_ += new String(fragment, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return "";
            }

            int idx = packet_.indexOf(kPacketDelimiter);
            if (idx == -1) {
                // pending...
                return "";
            }

            String parsed = packet_.substring(0, idx);
            if (packet_.length() > idx +1) {
                packet_ = packet_.substring(idx + 1);
            } else {
                packet_ = "";
            }
            return parsed;
        }

        // Create a new packet for sending.
        public static String createPacket(String command) {
            return command + kPacketDelimiter;
        }

        // Read packet body data.
        public ParseResult parsePacket(String packet) {
            ParseResult result = null;
            if (packet.equals("activity detected")) {
                result = new ParseResult(Reactions.ActivityDetected, "");
            }
            return result;
        }

    }
}
