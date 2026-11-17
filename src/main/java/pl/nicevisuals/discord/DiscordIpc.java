package pl.nicevisuals.discord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Minimal Discord Rich Presence client that talks to the locally running Discord app over its IPC
 * (named pipe on Windows, unix socket on Linux/macOS). No external libraries and no data leaves the PC
 * except what Discord itself shows on your profile.
 */
public final class DiscordIpc {
    private static final Logger LOG = LoggerFactory.getLogger("NiceVisuals/Discord");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Editable settings stored in config/nicevisuals-discord.json. */
    public static final class Config {
        public String clientId = "";
        public String clientName = "Fire Client";
        public String largeImageKey = "leaf";
        public String largeImageText = "Fire Client";
        public String buttonLabel = "Pobierz faze vials v";
        public String buttonUrl = "";
    }

    private interface Transport extends Closeable {
        void write(byte[] data) throws IOException;
        byte[] read(int n) throws IOException;
    }

    private static final Object LOCK = new Object();
    private static volatile boolean running;
    private static Thread worker;
    private static volatile Config cfg = new Config();
    private static volatile String details = "";
    private static volatile String state = "";
    private static volatile boolean showTime = true;
    private static volatile boolean dirty;
    private static boolean hookAdded;
    private static boolean warnedNoId;

    private DiscordIpc() {}

    public static Config config() { return cfg; }

    private static Path configFile() { return FabricLoader.getInstance().getConfigDir().resolve("nicevisuals-discord.json"); }

    private static Config loadConfig() {
        Path f = configFile();
        try {
            if (Files.exists(f)) {
                Config c = GSON.fromJson(Files.readString(f), Config.class);
                if (c != null) return c;
            } else {
                Files.writeString(f, GSON.toJson(new Config()));
            }
        } catch (Exception e) {
            LOG.warn("Could not read {}: {}", f, e.toString());
        }
        return new Config();
    }

    /** Starts the background connection (safe to call repeatedly). */
    public static void start() {
        synchronized (LOCK) {
            if (running) return;
            cfg = loadConfig();
            if (cfg.clientId == null || cfg.clientId.isBlank()) {
                if (!warnedNoId) {
                    LOG.warn("Discord RPC is idle: set \"clientId\" in {} (your Discord application ID).", configFile());
                    warnedNoId = true;
                }
                return;
            }
            running = true;
            dirty = true;
            if (!hookAdded) {
                Runtime.getRuntime().addShutdownHook(new Thread(DiscordIpc::stop, "NiceVisuals-DiscordRPC-stop"));
                hookAdded = true;
            }
            worker = new Thread(DiscordIpc::loop, "NiceVisuals-DiscordRPC");
            worker.setDaemon(true);
            worker.start();
        }
    }

    /** Clears the status and disconnects. */
    public static void stop() {
        Thread t;
        synchronized (LOCK) {
            if (!running) return;
            running = false;
            t = worker;
        }
        try { if (t != null) t.join(1500); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    public static boolean isRunning() { return running; }

    /** Called from the game thread with the current real values; only changes trigger an update. */
    public static void update(String newDetails, String newState, boolean time) {
        if (!newDetails.equals(details) || !newState.equals(state) || time != showTime) {
            details = newDetails;
            state = newState;
            showTime = time;
            dirty = true;
        }
    }

    // ---------------------------------------------------------------- connection loop

    private static void loop() {
        while (running) {
            Transport t = null;
            try {
                t = connect();
                if (t == null) { sleepWhileRunning(15000); continue; }
                handshake(t);
                LOG.info("Connected to Discord.");
                long lastSend = 0;
                dirty = true;
                while (running) {
                    long now = System.currentTimeMillis();
                    if (dirty || now - lastSend > 15000) {
                        dirty = false;
                        sendActivity(t, true);
                        lastSend = now;
                    }
                    Thread.sleep(250);
                }
            } catch (Exception e) {
                if (running) LOG.debug("Discord connection lost: {}", e.toString());
            } finally {
                if (t != null) {
                    try { sendActivity(t, false); } catch (Exception ignored) {}
                    try { t.close(); } catch (Exception ignored) {}
                }
            }
            if (running) sleepWhileRunning(15000);
        }
    }

    private static void sleepWhileRunning(long ms) {
        long end = System.currentTimeMillis() + ms;
        while (running && System.currentTimeMillis() < end) {
            try { Thread.sleep(250); } catch (InterruptedException e) { return; }
        }
    }

    // ---------------------------------------------------------------- protocol

    private static void handshake(Transport t) throws IOException {
        JsonObject hs = new JsonObject();
        hs.addProperty("v", 1);
        hs.addProperty("client_id", cfg.clientId.trim());
        writeFrame(t, 0, hs.toString());
        JsonObject reply = readReply(t);
        if (reply.has("evt") && "ERROR".equals(reply.get("evt").getAsString())) {
            throw new IOException("Discord refused the handshake: " + reply);
        }
    }

    private static void sendActivity(Transport t, boolean present) throws IOException {
        JsonObject args = new JsonObject();
        args.addProperty("pid", ProcessHandle.current().pid());
        if (present) {
            JsonObject a = new JsonObject();
            if (!details.isBlank()) a.addProperty("details", details);
            if (!state.isBlank()) a.addProperty("state", state);
            if (showTime) {
                JsonObject ts = new JsonObject();
                ts.addProperty("start", ProcessHandle.current().info().startInstant()
                        .map(java.time.Instant::getEpochSecond).orElse(System.currentTimeMillis() / 1000));
                a.add("timestamps", ts);
            }
            if (cfg.largeImageKey != null && !cfg.largeImageKey.isBlank()) {
                JsonObject assets = new JsonObject();
                assets.addProperty("large_image", cfg.largeImageKey.trim());
                if (cfg.largeImageText != null && !cfg.largeImageText.isBlank())
                    assets.addProperty("large_text", cfg.largeImageText);
                a.add("assets", assets);
            }
            String url = cfg.buttonUrl == null ? "" : cfg.buttonUrl.trim();
            String label = cfg.buttonLabel == null ? "" : cfg.buttonLabel.trim();
            if (!label.isEmpty() && (url.startsWith("https://") || url.startsWith("http://"))) {
                JsonObject b = new JsonObject();
                b.addProperty("label", label.length() > 32 ? label.substring(0, 32) : label);
                b.addProperty("url", url);
                JsonArray buttons = new JsonArray();
                buttons.add(b);
                a.add("buttons", buttons);
            }
            args.add("activity", a);
        }
        JsonObject cmd = new JsonObject();
        cmd.addProperty("cmd", "SET_ACTIVITY");
        cmd.add("args", args);
        cmd.addProperty("nonce", UUID.randomUUID().toString());
        writeFrame(t, 1, cmd.toString());
        readReply(t);
    }

    private static void writeFrame(Transport t, int op, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(8 + body.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(op).putInt(body.length).put(body);
        t.write(buf.array());
    }

    /** Reads frames until a normal (non-ping) one arrives; answers pings. */
    private static JsonObject readReply(Transport t) throws IOException {
        while (true) {
            ByteBuffer head = ByteBuffer.wrap(t.read(8)).order(ByteOrder.LITTLE_ENDIAN);
            int op = head.getInt(), len = head.getInt();
            if (len < 0 || len > 1 << 20) throw new IOException("Bad frame length " + len);
            String body = new String(t.read(len), StandardCharsets.UTF_8);
            if (op == 3) { writeFrame(t, 4, body); continue; }      // ping -> pong
            if (op == 2) throw new IOException("Discord closed the connection: " + body);
            try { return JsonParser.parseString(body).getAsJsonObject(); }
            catch (Exception e) { return new JsonObject(); }
        }
    }

    // ---------------------------------------------------------------- transports

    private static Transport connect() {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        for (int i = 0; i < 10; i++) {
            try {
                if (windows) return new PipeTransport(new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw"));
                for (String dir : unixDirs()) {
                    for (String sub : new String[]{"", "app/com.discordapp.Discord/", "snap.discord/"}) {
                        Path p = Path.of(dir, sub + "discord-ipc-" + i);
                        if (!Files.exists(p)) continue;
                        try {
                            SocketChannel ch = SocketChannel.open(StandardProtocolFamily.UNIX);
                            ch.connect(UnixDomainSocketAddress.of(p));
                            return new SocketTransport(ch);
                        } catch (IOException ignored) {}
                    }
                }
            } catch (IOException ignored) {}
        }
        return null;
    }

    private static String[] unixDirs() {
        String[] names = {"XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP"};
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String n : names) { String v = System.getenv(n); if (v != null && !v.isBlank()) out.add(v); }
        out.add("/tmp");
        return out.toArray(new String[0]);
    }

    private static final class PipeTransport implements Transport {
        private final RandomAccessFile f;
        PipeTransport(RandomAccessFile f) { this.f = f; }
        @Override public void write(byte[] d) throws IOException { f.write(d); }
        @Override public byte[] read(int n) throws IOException { byte[] b = new byte[n]; f.readFully(b); return b; }
        @Override public void close() throws IOException { f.close(); }
    }

    private static final class SocketTransport implements Transport {
        private final SocketChannel ch;
        private final InputStream in;
        private final OutputStream out;
        SocketTransport(SocketChannel ch) { this.ch = ch; this.in = Channels.newInputStream(ch); this.out = Channels.newOutputStream(ch); }
        @Override public void write(byte[] d) throws IOException { out.write(d); out.flush(); }
        @Override public byte[] read(int n) throws IOException {
            byte[] b = in.readNBytes(n);
            if (b.length < n) throw new IOException("Discord closed the connection");
            return b;
        }
        @Override public void close() throws IOException { ch.close(); }
    }
}
