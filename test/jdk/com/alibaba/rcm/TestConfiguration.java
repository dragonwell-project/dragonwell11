/*
 * @test
 * @summary Test builder and update constraint
 * @library /lib/testlibrary
 * @modules java.base/com.alibaba.rcm.internal:+open
 * @run main TestConfiguration
 */

import demo.MyResourceContainer;
import demo.MyResourceFactory;
import demo.MyResourceType;

import java.util.Arrays;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static jdk.testlibrary.Asserts.assertTrue;

public class TestConfiguration {
    public static void main(String[] args) {

        MyResourceContainer mc = (MyResourceContainer) MyResourceFactory.INSTANCE.createContainer(Arrays.asList(
                MyResourceType.MY_RESOURCE1.newConstraint(),
                MyResourceType.MY_RESOURCE2.newConstraint()));

        assertTrue(iterator2Stream(mc.operations.iterator()).collect(Collectors.toSet())
                .equals(new HashSet<>(Arrays.asList("update " + MyResourceType.MY_RESOURCE1.toString(),
                                                    "update " + MyResourceType.MY_RESOURCE2.toString()))));

        mc.updateConstraint(MyResourceType.MY_RESOURCE2.newConstraint());

        assertTrue(iterator2Stream(mc.operations.iterator()).collect(Collectors.toSet())
                .equals(new HashSet<>(Arrays.asList("update " + MyResourceType.MY_RESOURCE1.toString(),
                                                    "update " + MyResourceType.MY_RESOURCE2.toString(),
                                                    "update " + MyResourceType.MY_RESOURCE2.toString()))));
    }

    private static <T> Stream<T> iterator2Stream(Iterator<T> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false);
    }
}
