/*
 * @test
 * @summary Test Wisp engine's NIO support
 * @modules java.base/jdk.internal.misc
 * @requires os.family == "linux"
 * @requires os.arch != "riscv64"
 * @run main/othervm/timeout=10 -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.threadAsWisp.black=name:Local-Server TestIo
*/



import jdk.internal.misc.SharedSecrets;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Properties;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class TestIo {

    static int port = 28888;
    static String socketAddr = "127.0.0.1";
    static ServerSocket ss;
    public static void main(String[] args) {
        try {
            runServerThread();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("cannot create a server socket");
        }
        String result = http();

        System.out.println(result);

        if (!result.startsWith("HTTP") ||
                !result.contains("Content-") ||
                !result.contains("Server:"))
            throw new Error("protocol error");
    }


     /** An Example of Http Response
      * HTTP/1.0 200 OK
      * Content-Encoding: gzip
      * Accept-Ranges: bytes
      * Age: 482198
      * Cache-Control: max-age=604800
      * Content-Type: text/html; charset=UTF-8
      * Date: Wed, 31 Jul 2024 08:38:52 GMT
      * Etag: "3147526947"
      * Expires: Wed, 07 Aug 2024 08:38:52 GMT
      * Last-Modified: Thu, 17 Oct 2019 07:18:26 GMT
      * Server: ECAcc (sac/2533)
      * X-Cache: HIT
      * Content-Length: 648
      * Connection: close
      */
    private static void runServerThread() throws Exception {
        ss = new ServerSocket(port, 0, null);
        Thread serverThread = new Thread(()->{
            try {
                while (true) {
                    System.out.println("waiting......");
                    Socket s = ss.accept();
                    BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    String mess = br.readLine();
                    assert mess.contains("HEAD / HTTP/1.0");
                    String response = "HTTP/1.0 200 OK\r\n" +
                                            "Content-type:text/html; charset=UTF-8\r\n" +
                                            "Server: ECAcc (sac/2533)\r\n" + "\r\n";
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                    bw.write(response);
                    bw.flush();
                    s.close();
                    ss.close();
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "Local-Server");
        serverThread.start();
    }

    static String http() {

        try {
            SocketChannel ch = SocketChannel.open();
            ch.configureBlocking(false);

            connect(ch, socketAddr);
            ByteBuffer bb = ByteBuffer.allocate(1000);
            String request = "HEAD / HTTP/1.0\r\nHost:" + socketAddr + "\r\n\r\n";
            bb.put(request.getBytes());
            bb.flip();
            write(ch, bb);
            bb.clear();
            read(ch, bb);

            return new String(bb.array(), 0, bb.remaining());

        } catch (IOException e) {
            throw new Error(e);
        }
    }

    static int read(SocketChannel ch, ByteBuffer bb) throws IOException {
        int n, retry = 0;
        while ((n = ch.read(bb)) == 0) {
            if (retry++ > 3)
                throw new Error("busy loop"); // make sure we're not in a busy loop
            SharedSecrets.getWispEngineAccess().registerEvent(ch, SelectionKey.OP_READ);
            SharedSecrets.getWispEngineAccess().park(-1);
        }
        return n;
    }

    static int write(SocketChannel ch, ByteBuffer bb) throws IOException {
        int n, retry = 0;
        while ((n = ch.write(bb)) == 0) {
            if (retry++ > 3)    throw new Error("busy loop");
            SharedSecrets.getWispEngineAccess().registerEvent(ch, SelectionKey.OP_WRITE);
            SharedSecrets.getWispEngineAccess().park(-1);
        }
        return n;
    }

    static void connect(SocketChannel ch, String ip) throws IOException {
        ch.connect(new InetSocketAddress(ip, port));
        int retry = 0;
        while (!ch.finishConnect()) {
            if (retry++ > 3)    throw new Error("busy loop");
            SharedSecrets.getWispEngineAccess().registerEvent(ch, SelectionKey.OP_CONNECT);
            SharedSecrets.getWispEngineAccess().park(-1);
        }
    }
}
