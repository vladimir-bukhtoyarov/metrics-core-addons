/*
 *    Copyright 2017 Vladimir Bukhtoyarov
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.github.rollingmetrics.hitratio.impl;

import com.github.rollingmetrics.hitratio.HitRatio;
import com.github.rollingmetrics.retention.RetentionPolicy;
import com.github.rollingmetrics.util.Ticker;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;

public class SnapshotCachingHitRatioTest {

    @Test
    public void testCaching() throws Exception {
        AtomicLong currentTimeMillis = new AtomicLong();
        Ticker ticker = Ticker.mock(currentTimeMillis);
        HitRatio ratio = RetentionPolicy.uniform()
                .withTicker(ticker)
                .withSnapshotCachingDuration(Duration.ofSeconds(1))
                .newHitRatio();

        ratio.update(50, 100);
        assertEquals(0.5d, ratio.getHitRatio(), 0.001);

        ratio.update(90, 100);
        assertEquals(0.5d, ratio.getHitRatio(), 0.001);

        currentTimeMillis.addAndGet(999);
        assertEquals(0.5d, ratio.getHitRatio(), 0.001);

        currentTimeMillis.addAndGet(1);
        assertEquals(0.7d, ratio.getHitRatio(), 0.001);
    }

}