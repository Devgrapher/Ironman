package com.example.jihun.ironman;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.StringTokenizer;

/**
 * Created by Jihun on 2015-06-14.
 */
public class ArduinoConnector {
    protected Context application_context_;
    protected BluetoothSerial bluetooth_ = new BluetoothSerial();

    public ArduinoConnector(Context application_context) {
        application_context_ = application_context;
    }

    public void connect(BluetoothDevice device) {
        bluetooth_ = new BluetoothSerial();
        bluetooth_.askConnect(device, bluetooth_listener_);
    }

    public void send(String command) {
        bluetooth_.Write(command.getBytes());
    }

    public void destroy() {
        bluetooth_.cancel();
    }

    protected  BluetoothSerial.Listener bluetooth_listener_ = new BluetoothSerial.Listener() {
        private PacketProcessor packetProcessor_ = new PacketProcessor();

        @Override
        public void onConnect(BluetoothDevice device) {
            Toast.makeText(application_context_, "Connected", Toast.LENGTH_SHORT).show();
            String a = "hi";
            bluetooth_.Write(a.getBytes());
        }

        @Override
        public void onRead(BluetoothDevice device, byte[] data, int len) {
            String packet = packetProcessor_.pushPacketFragment(data, len);
            if (packet == null) {
                return;
            }

            Toast.makeText(application_context_, packet, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDisconnect(BluetoothDevice device) {
            Toast.makeText(application_context_, "Disconnected",
                    Toast.LENGTH_SHORT).show();
        }
    };

    // ----- Packet Format ------
    // #[length of body:int]#[body]
    // The whole packet is string data.
    // '#' is the delimiter.
    public static class PacketProcessor {
        private ByteArrayOutputStream fragment_buffer_ = new ByteArrayOutputStream();
        private int body_length_ = 0;
        private String body_;

        private final String kPacketDelimiter = "#";
        private final int kMinPacketLen = 5;
        private final int kMaxPacketLen = 50;

        // packet process result;
        public static final int kPacketPending = 1;
        public static final int kPacketInvalid = 2;
        public static final int kPacketOk = 3;

        public String pushPacketFragment(byte[] fragment, int len) {
            fragment_buffer_.write(fragment, 0, len);

            int result = parsePacket(fragment_buffer_.toByteArray());
            if (result == kPacketInvalid) {
                fragment_buffer_.reset();
                return null;
            } else if (result == kPacketOk) {
                fragment_buffer_.reset();
                return body_;
            }

            return null;
        }

        public int parsePacket(byte[] bytes) {
            if (bytes.length < kMinPacketLen) {
                return kPacketPending;
            }
            // check format.
            if (bytes[0] != '#' || bytes[3] != '#') {
                return kPacketInvalid;
            }

            String packet;
            try {
                packet = new String(bytes, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return  kPacketInvalid;
            }

            StringTokenizer tokenizer = new StringTokenizer(packet, kPacketDelimiter, false);

            // get body length
            if (!tokenizer.hasMoreTokens())
                return kPacketInvalid;
            String len = tokenizer.nextToken();
            if (len.isEmpty())
                return kPacketPending;
            try {
                body_length_ = Integer.parseInt(len);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return kPacketInvalid;
            }
            if (body_length_ > kMaxPacketLen)
                return kPacketInvalid;

            // get body.
            if (!tokenizer.hasMoreTokens())
                return kPacketInvalid;
            body_ = tokenizer.nextToken();
            if (body_.length() < body_length_)
                return kPacketPending;
            else if (body_.length() > body_length_) {
                body_ = body_.substring(0, body_length_);
            }

            return kPacketOk;
        }
    }
}
