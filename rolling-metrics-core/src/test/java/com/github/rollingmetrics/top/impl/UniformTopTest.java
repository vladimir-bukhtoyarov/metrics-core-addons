/*
 *
 *  Copyright 2017 Vladimir Bukhtoyarov
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.github.rollingmetrics.top.impl;

import com.github.rollingmetrics.retention.RetentionPolicy;
import com.github.rollingmetrics.top.Top;
import com.github.rollingmetrics.top.TopTestData;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;


public class UniformTopTest {

    @Test
    public void testCommonAspects() {
        for (int i = 1; i <= 2; i++) {
            Top top = RetentionPolicy.uniform()
                    .withSnapshotCachingDuration(Duration.ZERO)
                    .newTopBuilder(i)
                    .withLatencyThreshold(Duration.ofMillis(100))
                    .withMaxLengthOfQueryDescription(1000)
                    .build();
            TopTestUtil.testCommonScenarios(i, top, Duration.ofMillis(100).toNanos(), 1000);
        }
    }

    @Test
    public void test_size_1() throws Exception {
        Top top = RetentionPolicy.uniform()
                .newTopBuilder(1)
                .build();

        TopTestUtil.assertEmpty(top);

        TopTestUtil.update(top, TopTestData.first);
        TopTestUtil.checkOrder(top, TopTestData.first);

        TopTestUtil.update(top, TopTestData.second);
        TopTestUtil.checkOrder(top, TopTestData.second);

        TopTestUtil.update(top, TopTestData.first);
        TopTestUtil.checkOrder(top, TopTestData.second);
    }

    @Test
    public void test_size_3() throws Exception {
        Top top = RetentionPolicy.uniform()
                .newTopBuilder(3)
                .build();

        TopTestUtil.assertEmpty(top);

        TopTestUtil.update(top, TopTestData.first);
        TopTestUtil.checkOrder(top, TopTestData.first);

        TopTestUtil.update(top, TopTestData.second);
        TopTestUtil.checkOrder(top, TopTestData.second, TopTestData.first);

        TopTestUtil.update(top, TopTestData.third);
        TopTestUtil.checkOrder(top, TopTestData.third, TopTestData.second, TopTestData.first);

        TopTestUtil.update(top, TopTestData.fourth);
        TopTestUtil.checkOrder(top, TopTestData.fourth, TopTestData.third, TopTestData.second);

        TopTestUtil.update(top, TopTestData.fifth);
        TopTestUtil.checkOrder(top, TopTestData.fifth, TopTestData.fourth, TopTestData.third);

        TopTestUtil.update(top, TopTestData.first);
        TopTestUtil.checkOrder(top, TopTestData.fifth, TopTestData.fourth, TopTestData.third);

        TopTestUtil.update(top, TopTestData.fifth);
        TopTestUtil.checkOrder(top, TopTestData.fifth, TopTestData.fourth, TopTestData.third);
    }

    @Test
    public void testToString() {
        for (int i = 1; i <= 2; i++) {
            Top top = RetentionPolicy.uniform().newTopBuilder(i).build();
            System.out.println(top);
        }
    }

    @Test(timeout = 32000)
    public void testThatConcurrentThreadsNotHung() throws InterruptedException {
        Top top = RetentionPolicy.uniform().newTopBuilder(1).build();
        TopTestUtil.runInParallel(top, TimeUnit.SECONDS.toMillis(30), 0, 10_000);
    }

}