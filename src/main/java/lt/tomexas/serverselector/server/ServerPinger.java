package lt.tomexas.serverselector.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Checks whether a Minecraft server answers a Server List Ping. Blocking: call it off the main thread. */
public final class ServerPinger {

    private static final int STATUS_PACKET_ID = 0x00;
    // -1 asks for the status without claiming a client version
    private static final int ANY_PROTOCOL = -1;
    private static final int NEXT_STATE_STATUS = 1;
    // A status reply is a few KB at most (favicons included); anything larger is not a Minecraft server
    private static final int MAX_PACKET_LENGTH = 1 << 20;
    private static final int VARINT_MAX_BYTES = 5;
    private static final int SEGMENT_BITS = 0x7F;
    private static final int CONTINUE_BIT = 0x80;

    private ServerPinger() {
    }

    /** True if the server at host:port replies to a status request with a JSON status. */
    public static boolean isOnline(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);

            ByteArrayOutputStream handshake = new ByteArrayOutputStream();
            writeVarInt(handshake, STATUS_PACKET_ID);
            writeVarInt(handshake, ANY_PROTOCOL);
            writeString(handshake, host);
            handshake.write(port >>> 8 & 0xFF);
            handshake.write(port & 0xFF);
            writeVarInt(handshake, NEXT_STATE_STATUS);

            ByteArrayOutputStream request = new ByteArrayOutputStream();
            writeVarInt(request, STATUS_PACKET_ID);

            OutputStream out = socket.getOutputStream();
            writePacket(out, handshake);
            writePacket(out, request);
            out.flush();

            InputStream in = socket.getInputStream();
            int length = readVarInt(in);
            if (length <= 0 || length > MAX_PACKET_LENGTH) return false;
            byte[] packet = in.readNBytes(length);
            if (packet.length != length) return false;
            return isStatusResponse(packet);
        } catch (IOException | IllegalArgumentException e) {
            // Refused, timed out, unresolved host or a malformed reply: not reachable as a Minecraft server
            return false;
        }
    }

    private static boolean isStatusResponse(byte[] packet) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(packet));
        if (readVarInt(in) != STATUS_PACKET_ID) return false;
        int jsonLength = readVarInt(in);
        if (jsonLength <= 0) return false;
        byte[] json = in.readNBytes(jsonLength);
        if (json.length != jsonLength) return false;
        return new String(json, StandardCharsets.UTF_8).trim().startsWith("{");
    }

    private static void writePacket(OutputStream out, ByteArrayOutputStream packet) throws IOException {
        writeVarInt(out, packet.size());
        packet.writeTo(out);
    }

    private static void writeString(OutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    static void writeVarInt(OutputStream out, int value) throws IOException {
        while ((value & ~SEGMENT_BITS) != 0) {
            out.write(value & SEGMENT_BITS | CONTINUE_BIT);
            value >>>= 7;
        }
        out.write(value);
    }

    static int readVarInt(InputStream in) throws IOException {
        int value = 0;
        for (int i = 0; i < VARINT_MAX_BYTES; i++) {
            int b = in.read();
            if (b < 0) throw new EOFException("Stream ended inside a VarInt");
            value |= (b & SEGMENT_BITS) << (7 * i);
            if ((b & CONTINUE_BIT) == 0) return value;
        }
        throw new IOException("VarInt is longer than " + VARINT_MAX_BYTES + " bytes");
    }
}
