package com.evaitcs.advanced.io;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Demo 5 — THE REACTOR: one thread serving MANY connections with a Selector.
 * (README section "Selectors: the one-thread-many-connections trick".)
 *
 * The problem: a server with one thread per client runs out of threads long before it runs out of
 * work — every idle client still pins a platform thread and its stack (module 06). The reactor
 * answers differently: ONE thread asks the OS "which of these connections are ready RIGHT NOW?"
 * (Selector.select) and services only those. An idle connection costs almost nothing.
 *
 * This demo proves it live and DETERMINISTICALLY: a ServerSocketChannel + a Selector run on one
 * thread and echo TWO loopback clients. Everything is on the loopback interface with an ephemeral
 * port, every client read is bounded (SO_TIMEOUT), the reactor uses a bounded select timeout, the
 * reactor thread is joined, and all channels are closed — nothing can hang.
 *
 * The three NIO pillars meet here: BUFFERS carry the bytes, CHANNELS are the connections, and the
 * SELECTOR multiplexes them onto one thread. This is the machinery under Tomcat's NIO connector and
 * Netty — and, alongside virtual threads (module 06), one of the two modern answers to "many clients."
 */
public class SelectorEchoDemo {

    public static void main(String[] args) throws Exception {
        banner("1. The select loop, narrated (the reactor's heartbeat)");
        System.out.println("  step 1: register a ServerSocketChannel with a Selector for OP_ACCEPT.");
        System.out.println("  step 2: ONE thread calls selector.select(timeout) and BLOCKS until a channel is ready.");
        System.out.println("  step 3: select returns the READY keys; the thread loops over them and REMOVES each.");
        System.out.println("  step 4: acceptable key -> accept() a SocketChannel, set it non-blocking, register OP_READ.");
        System.out.println("  step 5: readable key -> channel.read(buffer); when a full line arrives, echo it back.");
        System.out.println("  step 6: loop back to select. One thread, many connections, none wasted on idle clients.");
        System.out.println("  interestOps = what you ASK to be told about; readyOps = what is ACTUALLY ready now.");

        try (Reactor reactor = new Reactor()) {
            int port = reactor.port();
            banner("2. Live: one reactor thread echoes two loopback clients");
            System.out.println("  server bound to loopback port " + port + " (ephemeral); reactor thread is in select().");

            String m1 = "transfer 250.00 to Dana";
            String m2 = "balance check account 42";
            String[] got = new String[2];
            CountDownLatch bothEchoed = new CountDownLatch(2);

            Thread c1 = new Thread(() -> {
                try {
                    got[0] = sendAndReceive(port, m1);
                } catch (IOException e) {
                    got[0] = "ERROR: " + e;
                } finally {
                    bothEchoed.countDown();
                }
            }, "client-1");
            Thread c2 = new Thread(() -> {
                try {
                    got[1] = sendAndReceive(port, m2);
                } catch (IOException e) {
                    got[1] = "ERROR: " + e;
                } finally {
                    bothEchoed.countDown();
                }
            }, "client-2");

            c1.start();
            c2.start();
            boolean done = bothEchoed.await(5, TimeUnit.SECONDS);   // generous bound; loopback is instant
            c1.join(2000);
            c2.join(2000);

            System.out.println("  client-1 sent \"" + m1 + "\" -> got \"" + got[0] + "\"  match=" + m1.equals(got[0]));
            System.out.println("  client-2 sent \"" + m2 + "\" -> got \"" + got[1] + "\"  match=" + m2.equals(got[1]));
            System.out.println("  both echoed within the bound? " + done);
            System.out.println("  verdict: ONE thread (reactor-thread) served BOTH connections — no thread-per-client.");
        }

        banner("3. Why this is the whole game");
        System.out.println("  A thread parked in select() costs the OS almost nothing, so one thread can watch");
        System.out.println("  thousands of sockets. That is the reactor pattern: don't block a thread per connection;");
        System.out.println("  block ONE selector and dispatch only the ready ones.");
        System.out.println("  The other modern answer is virtual threads (module 06): keep thread-per-connection, but");
        System.out.println("  make the threads cheap. Reactor = few threads, event-driven. Virtual = many, cheap.");

        banner("4. In one sentence");
        System.out.println("  A Selector lets ONE thread ask the OS which connections are ready and serve only those,");
        System.out.println("  which is how a handful of threads handle ten thousand clients (Tomcat NIO, Netty).");
    }

    /** Connects a blocking client Socket to the reactor, sends one line, returns the echoed line. */
    static String sendAndReceive(int port, String message) throws IOException {
        try (Socket client = new Socket()) {
            client.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 2000);
            client.setSoTimeout(2000);                   // bound the read so nothing can hang
            OutputStream out = client.getOutputStream();
            out.write((message + "\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
            return in.readLine();                        // the reactor echoes the line back
        }
    }

    /**
     * A minimal single-threaded reactor: one ServerSocketChannel + one Selector + one thread that
     * echoes newline-delimited messages from any number of clients. Package-private so IoNioTest can
     * drive it deterministically; AutoCloseable so try-with-resources stops and joins the thread.
     */
    static final class Reactor implements AutoCloseable {
        private final ServerSocketChannel server;
        private final Selector selector;
        private final Thread thread;
        private volatile boolean running = true;

        Reactor() throws IOException {
            this.selector = Selector.open();
            this.server = ServerSocketChannel.open();
            this.server.configureBlocking(false);        // non-blocking: accept() returns null if none pending
            this.server.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));  // ephemeral port
            this.server.register(selector, SelectionKey.OP_ACCEPT);
            this.thread = new Thread(this::loop, "reactor-thread");
            this.thread.setDaemon(true);                 // safety net: the JVM can always exit
            this.thread.start();
        }

        int port() throws IOException {
            return ((InetSocketAddress) server.getLocalAddress()).getPort();
        }

        private void loop() {
            ByteBuffer buf = ByteBuffer.allocate(256);   // one buffer, reused (single-threaded reactor)
            try {
                while (running) {
                    if (selector.select(200) == 0) {      // timed out with nothing ready
                        continue;                         // re-check `running`, then select again
                    }
                    Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                    while (it.hasNext()) {
                        SelectionKey key = it.next();
                        it.remove();                      // MUST remove, or the key stays 'selected' next round
                        if (!key.isValid()) {
                            continue;
                        }
                        if (key.isAcceptable()) {
                            accept();
                        } else if (key.isReadable()) {
                            echo(key, buf);
                        }
                    }
                }
            } catch (ClosedSelectorException e) {
                // shutting down: the selector was closed under us by close()
            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                }
            }
        }

        private void accept() throws IOException {
            SocketChannel client;
            while ((client = server.accept()) != null) {  // drain every pending connection
                client.configureBlocking(false);          // non-blocking so read() never stalls the thread
                client.register(selector, SelectionKey.OP_READ, new ByteArrayOutputStream());
            }
        }

        private void echo(SelectionKey key, ByteBuffer buf) throws IOException {
            SocketChannel ch = (SocketChannel) key.channel();
            ByteArrayOutputStream pending = (ByteArrayOutputStream) key.attachment();
            buf.clear();
            int n;
            try {
                n = ch.read(buf);
            } catch (IOException e) {
                key.cancel();                             // peer vanished: drop it and move on
                ch.close();
                return;
            }
            if (n == -1) {                                // client closed its output (EOF)
                key.cancel();
                ch.close();
                return;
            }
            buf.flip();                                   // FILL -> DRAIN before reading the bytes
            while (buf.hasRemaining()) {
                byte b = buf.get();
                if (b == '\n') {                          // newline-delimited framing
                    String line = pending.toString(StandardCharsets.UTF_8);
                    pending.reset();
                    ByteBuffer out = ByteBuffer.wrap((line + "\n").getBytes(StandardCharsets.UTF_8));
                    while (out.hasRemaining()) {
                        ch.write(out);                    // tiny echoes always fit the socket send buffer
                    }
                } else {
                    pending.write(b);                     // accumulate a partial line until '\n' arrives
                }
            }
        }

        @Override
        public void close() {
            running = false;
            selector.wakeup();                           // unblock select() so the loop can exit now
            try {
                thread.join(3000);                       // bounded join; the daemon flag is the safety net
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            try {
                selector.close();
            } catch (IOException ignored) {
                // closing a selector we own
            }
            try {
                server.close();
            } catch (IOException ignored) {
                // closing a server channel we own
            }
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
