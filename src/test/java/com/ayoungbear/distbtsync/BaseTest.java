package com.ayoungbear.distbtsync;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 测试基类, 提供一些测试用的基础方法
 * 
 * @author yangzexiong
 */
public abstract class BaseTest {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @BeforeClass
    public static void baseSetUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void baseTearDownAfterClass() throws Exception {
    }

    @Before
    public void baseSetUp() throws Exception {
    }

    @After
    public void baseTearDown() throws Exception {
    }

    public static Thread run(Runnable runnable) {
        Thread t = new Thread(runnable);
        t.start();
        return t;
    }

    public static Thread run(Runnable runnable, String name) {
        Thread t = new Thread(runnable, name);
        t.start();
        return t;
    }

    public static void sleep(long time, TimeUnit timeUnit) {
        try {
            Thread.sleep(timeUnit.toMillis(time));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sleep(long seconds) {
        sleep(seconds, TimeUnit.SECONDS);
    }

    /**
     * 并发执行任务
     * @param nodeNum 节点数, 用线程模拟分布式服务
     * @param threadNum 每个节点执行线程数
     * @param tasks 执行的任务
     */
    protected void concurrentExecute(int nodeNum, int threadNum, Runnable... tasks) {
        logger.info("ConcurrentExecute use nodeNum={} threadNum={}", nodeNum, threadNum);
        CountDownLatch countDownLatch = new CountDownLatch(nodeNum);
        for (int n = 1; n <= nodeNum; n++) {
            run(() -> {
                CountDownLatch threadCountDownLatch = new CountDownLatch(threadNum);
                try {
                    for (int t = 1; t <= threadNum; t++) {
                        run(() -> {
                            try {
                                for (Runnable task : tasks) {
                                    task.run();
                                }
                            } catch (Exception e) {
                                logger.error("concurrentExecute error", e);
                            } finally {
                                try {
                                    threadCountDownLatch.countDown();
                                } catch (Exception e) {
                                    logger.error("Await error", e);
                                }
                            }
                        }, Thread.currentThread().getName() + "-runner-" + t);
                    }
                } finally {
                    try {
                        threadCountDownLatch.await();
                        countDownLatch.countDown();
                    } catch (Exception e) {
                        logger.error("Await error", e);
                    }
                }
            }, "node-" + n);
        }
        try {
            logger.info("Waiting for the end");
            countDownLatch.await();
        } catch (Exception e) {
            logger.error("Await error", e);
        }
    }

}
