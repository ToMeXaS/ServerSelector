package lt.tomexas.serverselector.server;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerPingerTest {

    private static final String STATUS_JSON =
            "{\"version\":{\"name\":\"26.3\",\"protocol\":772},\"players\":{\"max\":20,\"online\":0},\"description\":\"x\"}";

    @Test
    void varIntsRoundTrip() throws IOException {
        for (int value : new int[] {0, 1, 127, 128, 255, 25565, 2097151, -1}) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ServerPinger.writeVarInt(out, value);
            assertEquals(value, ServerPinger.readVarInt(new ByteArrayInputStream(out.toByteArray())), "value " + value);
        }
    }

    @Test
    void varIntsUseTheProtocolEncoding() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ServerPinger.writeVarInt(out, 25565);
        assertEquals(3, out.size());
        out.reset();
        ServerPinger.writeVarInt(out, -1);
        assertEquals(5, out.size());
    }

    @Test
    void aServerAnsweringTheStatusRequestIsOnline() throws Exception {
        AtomicReference<String> handshake = new AtomicReference<>();
        try (ServerSocket server = new ServerSocket(0)) {
            Thread thread = new Thread(() -> {
                try (Socket client = server.accept()) {
                    InputStream in = client.getInputStream();
                    handshake.set(readHandshake(in));
                    // Status request: length 1, packet id 0x00
                    assertEquals(1, ServerPinger.readVarInt(in));
                    assertEquals(0, ServerPinger.readVarInt(in));

                    byte[] json = STATUS_JSON.getBytes(StandardCharsets.UTF_8);
                    ByteArrayOutputStream packet = new ByteArrayOutputStream();
                    ServerPinger.writeVarInt(packet, 0);
                    ServerPinger.writeVarInt(packet, json.length);
                    packet.write(json);
                    OutputStream out = client.getOutputStream();
                    ServerPinger.writeVarInt(out, packet.size());
                    packet.writeTo(out);
                    out.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            thread.start();

            assertTrue(ServerPinger.isOnline("127.0.0.1", server.getLocalPort(), 2000));
            thread.join(2000);
            assertEquals("0 -1 127.0.0.1 " + server.getLocalPort() + " 1", handshake.get());
        }
    }

    @Test
    void nothingListeningIsOfflineQuickly() throws IOException {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        long start = System.nanoTime();
        assertFalse(ServerPinger.isOnline("127.0.0.1", port, 2000));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertTrue(elapsedMs < 2500, "took " + elapsedMs + " ms");
    }

    /** Reads a handshake packet as "id protocol host port nextState". */
    private static String readHandshake(InputStream in) throws IOException {
        int length = ServerPinger.readVarInt(in);
        DataInputStream body = new DataInputStream(new ByteArrayInputStream(in.readNBytes(length)));
        int id = ServerPinger.readVarInt(body);
        int protocol = ServerPinger.readVarInt(body);
        String host = new String(body.readNBytes(ServerPinger.readVarInt(body)), StandardCharsets.UTF_8);
        int port = body.readUnsignedShort();
        int nextState = ServerPinger.readVarInt(body);
        return id + " " + protocol + " " + host + " " + port + " " + nextState;
    }
}
