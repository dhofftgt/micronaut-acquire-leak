package micronaut.acquire.leak;

import io.micronaut.http.client.netty.DefaultHttpClient;
import io.micronaut.http.client.netty.DefaultNettyHttpClientRegistry;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.netty.channel.nio.NioEventLoop;
import io.netty.channel.pool.AbstractChannelPoolMap;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.FixedChannelPool;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;


@MicronautTest
class MicronautAcquireLeakTest {

    @Inject
    SampleHttpClient sampleHttpClient;

    @Inject
    DefaultNettyHttpClientRegistry httpClientRegistry;

    @Test
    void testAcquireChannelLeak() throws ExecutionException, InterruptedException {

        CountDownLatch nettyThreadBlocker = new CountDownLatch(1);

        List<CompletableFuture> futures = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            CompletableFuture resultFuture = sampleHttpClient.getSample().thenApply((s) -> {
                try {
                    nettyThreadBlocker.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return s;
            }).exceptionally((t) -> null);
            futures.add(resultFuture);
        }


        sleep(5000); // wait for timeouts

        nettyThreadBlocker.countDown();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).exceptionally((t) -> null).get();

        FixedChannelPool fixedChannelPool = getChannelPool("sample-http-client");

        while (getPendingTasks(fixedChannelPool) != 0) {
            sleep(1000);
        }

        Assertions.assertEquals(0, fixedChannelPool.acquiredChannelCount(), "Expected 0 acquired channels");

    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            //ignore
        }
    }

    public int getPendingTasks(FixedChannelPool fixedChannelPool) {
        NioEventLoop eventLoop = getEventLoop(fixedChannelPool);
        return eventLoop.pendingTasks();
    }

    public NioEventLoop getEventLoop(FixedChannelPool fixedChannelPool) {
        return ((NioEventLoop) getPrivateFieldValue(fixedChannelPool, FixedChannelPool.class, "executor"));
    }

    public FixedChannelPool getChannelPool(String clientId) {
        try {
            DefaultHttpClient defaultHttpClient = httpClientRegistry.getClient(new HttpClientAnnotationMetadata(clientId));
            Object connectionManager = getConnectionManager(defaultHttpClient);
            AbstractChannelPoolMap<Object, ChannelPool> poolMap = getPoolMap(connectionManager);
            Object requestKey = requestKey(defaultHttpClient, new URI("http://localhost:8080"));
            return (FixedChannelPool) poolMap.get(requestKey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Object requestKey(DefaultHttpClient httpClient, URI uri) {
        try {
            Class requestKey = Class.forName("io.micronaut.http.client.netty.DefaultHttpClient$RequestKey");
            Constructor constructor = requestKey.getDeclaredConstructor(DefaultHttpClient.class, URI.class);
            constructor.setAccessible(true);
            return constructor.newInstance(httpClient, uri);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public Object getConnectionManager(DefaultHttpClient httpClient) {
        return getPrivateFieldValue(httpClient, DefaultHttpClient.class, "connectionManager");
    }

    public AbstractChannelPoolMap<Object, ChannelPool> getPoolMap(Object connectionManager) {
        try {
            return (AbstractChannelPoolMap<Object, ChannelPool>) getPrivateFieldValue(connectionManager, Class.forName("io.micronaut.http.client.netty.ConnectionManager"), "poolMap");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public <T, F> Object getPrivateFieldValue(Object target, Class<T> targetClass, String fieldName) {
        try {
            Field privateField = targetClass.getDeclaredField(fieldName);
            privateField.setAccessible(true);
            return privateField.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
