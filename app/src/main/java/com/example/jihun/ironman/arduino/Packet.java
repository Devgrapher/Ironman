package com.example.jihun.ironman.arduino;

import com.example.jihun.ironman.arduino.PacketParser.Type;

public class Packet<T> {
    public Type type = Type.Error;
    public int id = -1;
    public T data = null;

    public Packet() {}

    public Packet(Type type) {
        this.type = type;
    }

    public Packet(Type type, T data) {
        this.type = type;
        this.data = data;
    }

    @Override
    public String toString() {
        return Packet.class.getName() + ": " + id + ": "+ type.toString() + ": " + data;
    }
}
