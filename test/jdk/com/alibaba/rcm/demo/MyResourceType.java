package demo;

import com.alibaba.rcm.ResourceType;

public class MyResourceType extends ResourceType {
    public final static ResourceType MY_RESOURCE1 = new MyResourceType("MY_RESOURCE1");
    public final static ResourceType MY_RESOURCE2 = new MyResourceType("MY_RESOURCE2");

    public MyResourceType(String name) {
        super(name);
    }
}
