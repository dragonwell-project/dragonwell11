/*
 * @test
 * @library /lib/testlibrary
 * @summary test reuse WispUdpSocket buffer
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true ReuseUdpSocektBufTest
 */

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import static jdk.testlibrary.Asserts.assertTrue;


public class ReuseUdpSocektBufTest {

	static String msgs[] = {"Hello World", "Java", "Good Bye"};
	static int port;

	static boolean success = true;

	static class ServerThread extends Thread{
		DatagramSocket ds;
		public ServerThread() {
			try {
				ds = new DatagramSocket();
				port = ds.getLocalPort();
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage());
			}
		}

		public void run() {
			byte b[] = new byte[100];
			DatagramPacket dp = new DatagramPacket(b,b.length);
			while (true) {
				try {
					ds.receive(dp);
					String reply = new String(dp.getData(), dp.getOffset(), dp.getLength());
					ds.send(new DatagramPacket(reply.getBytes(),reply.length(),
							dp.getAddress(),dp.getPort()));
					if (reply.equals(msgs[msgs.length-1])) {
						break;
					}
				} catch (Exception e) {
					success = false;
				}
			}
			ds.close();
		}
	}

	public static void main(String args[]) throws Exception {
		ServerThread st = new ServerThread();
		st.start();
		DatagramSocket ds = new DatagramSocket();
		byte b[] = new byte[100];
		DatagramPacket dp = new DatagramPacket(b,b.length);
		for (int i = 0; i < msgs.length; i++) {
			ds.send(new DatagramPacket(msgs[i].getBytes(),msgs[i].length(),
					InetAddress.getLocalHost(),
					port));
			ds.receive(dp);
			if (!msgs[i].equals(new String(dp.getData(), dp.getOffset(), dp.getLength()))) {
				success = false;
			}
		}
		ds.close();
		assertTrue(success);
		System.out.println("Test Passed!!!");
	}
}
