package camelinaction;

import java.util.Properties;

import org.apache.camel.component.infinispan.InfinispanConfiguration;
import org.apache.camel.component.infinispan.policy.InfinispanRoutePolicy;
import org.apache.camel.main.Main;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

public class ServerBar {

    private Main main;

    public static void main(String[] args) throws Exception {
        ServerBar bar = new ServerBar();
        bar.boot();
    }

    public void boot() throws Exception {
        // list of urls for the infinispan server
        // as we run in domain node we have two servers out of the box, and can therefore include both
        // that the client can load balance/failover to be highly available
        Properties props = new Properties();
        props.setProperty("infinispan.client.hotrod.server_list", "localhost:11222;localhost:11372");

        // create remote infinispan cache manager and start it
        RemoteCacheManager remote = new RemoteCacheManager(
            new ConfigurationBuilder().withProperties(props).build(),
            true
        );

        // setup Camel infinispan configuration to use the remote cache manager
        InfinispanConfiguration ic = new InfinispanConfiguration();
        ic.setCacheContainer(remote);

        // setup the hazelcast route policy
        InfinispanRoutePolicy routePolicy = new InfinispanRoutePolicy(ic);
        // the lock names must be same in the foo and bar server
        routePolicy.setLockMapName("myLock");
        routePolicy.setLockKey("myLockKey");
        routePolicy.setLockValue("myLockValue");

        main = new Main();
        // bind the hazelcast route policy to the name myPolicy which we refer to from the route
        main.bind("myPolicy", routePolicy);
        // add the route and and let the route be named BAR and use a little delay when processing the files
        main.addRouteBuilder(new FileConsumerRoute("BAR", 100));
        main.run();
    }

}
