/*
 * @test
 * @summary ensure nio program call SelectionKey.is{}able() and got correct result.
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true  TestWispSelectorReadyOps
*/

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class TestWispSelectorReadyOps {
    public static void main(String[] args) throws Exception {
        Selector selector = Selector.open();
        ServerSocketChannel sch = ServerSocketChannel.open();
        sch.configureBlocking(false);
        sch.bind(new InetSocketAddress(54442));
        sch.register(selector, sch.validOps());

        Socket so = new Socket("127.0.0.1", 54442);

        selector.selectNow();
        SelectionKey sk = (SelectionKey) selector.selectedKeys().toArray()[0];
        if (!sk.isAcceptable()) {
            throw new Error("fail");
        }

        SocketChannel sc = sch.accept();
        sc.configureBlocking(false);
        sc.register(selector, sc.validOps());

        so.getOutputStream().write("123".getBytes());

        selector.selectedKeys().clear();
        selector.selectNow();
        sk = (SelectionKey) selector.selectedKeys().toArray()[0];
        if (!sk.isReadable()) {
            throw new Error("fail");
        }
    }

}
