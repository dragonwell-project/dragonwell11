import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import jdk.crac.*;
import jdk.test.lib.crac.*;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Properties;
import java.util.Set;

/**
 * @test
 * @library /test/lib
 * @build TCPTransportTest
 * @run driver/timeout=180 jdk.test.lib.crac.CracTest
 */

public class TCPTransportTest extends CracLogger implements CracTest {
    private static final String MSG_CHECKPOINT = "Before checkpoint, there are some beans.";
    private static final String MSG_RESTORE = "After checkpoint, there are some beans.";
    private static final String JMX_REGISTRY_PORT = "com.sun.management.jmxremote.port";
    private static final String JMX_SERVER_PORT = "com.sun.management.jmxremote.rmi.port";

    @Override
    public void test() throws Exception {
        CracBuilder builder = new CracBuilder().allowSelfAttach(true).logToFile(true);
        CracProcess checkpointProcess = builder.startCheckpoint();
        checkpointProcess.waitForCheckpointed();
        checkpointProcess.fileOutputAnalyser().shouldContain(MSG_CHECKPOINT);

        CracProcess restoreProcess = builder.doRestore();
        Thread.sleep(5 * 1000L);
        restoreProcess.fileOutputAnalyser().shouldContain(MSG_RESTORE);
    }

    @Override
    public void exec() throws Exception {
        int port = findUnusedPort();
        startAgent(port);
        queryNames(port);
        writeLog(MSG_CHECKPOINT);

        Core.checkpointRestore();
        queryNames(port);
        writeLog(MSG_RESTORE);
    }

    private void startAgent(int port) throws IOException, AttachNotSupportedException {
        VirtualMachine virtualMachine = VirtualMachine.attach(String.valueOf(ProcessHandle.current().pid()));
        try {
            virtualMachine.startLocalManagementAgent();

            Properties properties = new Properties();
            properties.setProperty(JMX_REGISTRY_PORT, Integer.toString(port));
            properties.setProperty(JMX_SERVER_PORT, Integer.toString(port));
            properties.setProperty("com.sun.management.jmxremote.authenticate", "false");
            properties.setProperty("com.sun.management.jmxremote.ssl", "false");
            virtualMachine.startManagementAgent(properties);
        } finally {
            virtualMachine.detach();
        }
    }

    private static void queryNames(int serverPort) throws IOException {
        String host = "localhost";
        String url = "service:jmx:rmi:///jndi/rmi://" + host + ":" + serverPort + "/jmxrmi";
        JMXServiceURL serviceUrl = new JMXServiceURL(url);
        JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceUrl, null);
        try {
            MBeanServerConnection mbeanConn = jmxConnector.getMBeanServerConnection();
            mbeanConn.queryNames(null, null);
        } finally {
            jmxConnector.close();
        }
    }

    public static int findUnusedPort() throws IOException {
        int port;

        try (ServerSocket socket = new ServerSocket()) {
            socket.bind(new InetSocketAddress(0));
            port = socket.getLocalPort();
        }

        System.out.println("findUnusedPort port: " + port);
        return port;
    }
}
