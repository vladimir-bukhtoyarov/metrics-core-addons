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

package com.github.rollingmetrics.histogram.hdr;

import com.github.rollingmetrics.histogram.hdr.impl.SnapshotCachingRollingHdrHistogram;
import com.github.rollingmetrics.util.Ticker;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

public class SnapshotCachingTest {

    @Test
    public void whenCachingDurationSpecifiedThenReservoirShouldBeDecoratedByProxy() {
        RollingHdrHistogram histogram = RollingHdrHistogram.builder()
                .withSnapshotCachingDuration(Duration.ofSeconds(5))
                .build();
        assertTrue(histogram instanceof SnapshotCachingRollingHdrHistogram);
    }

    @Test
    public void zeroDurationShouldNotLeadToCreateDecorator() {
        RollingHdrHistogram reservoir = RollingHdrHistogram.builder()
                .withSnapshotCachingDuration(Duration.ZERO)
                .build();
        assertFalse(reservoir instanceof SnapshotCachingRollingHdrHistogram);
    }

    @Test
    public void byDefaultCachingShouldBeTurnedOf() {
        RollingHdrHistogram reservoir = RollingHdrHistogram.builder()
                .build();
        assertFalse(reservoir instanceof SnapshotCachingRollingHdrHistogram);
    }

    @Test
    public void shouldCacheSnapshot() {
        AtomicLong time = new AtomicLong(System.currentTimeMillis());
        Ticker ticker = Ticker.mock(time);
        RollingHdrHistogram reservoir =
                RollingHdrHistogram.builder()
                .withTicker(ticker)
                .resetReservoirOnSnapshot()
                .withSnapshotCachingDuration(Duration.ofMillis(1000))
                .build();

        reservoir.update(10);
        reservoir.update(20);
        RollingSnapshot firstSnapshot = reservoir.getSnapshot();

        time.getAndAdd(900);
        reservoir.update(30);
        reservoir.update(40);
        RollingSnapshot firstCachedSnapshot = reservoir.getSnapshot();
        assertSame(firstSnapshot, firstCachedSnapshot);
        assertEquals(10, firstCachedSnapshot.getMin());
        assertEquals(20, firstCachedSnapshot.getMax());

        time.getAndAdd(99);
        reservoir.update(50);
        reservoir.update(60);
        RollingSnapshot secondCachedSnapshot = reservoir.getSnapshot();
        assertSame(firstSnapshot, secondCachedSnapshot);
        assertEquals(10, secondCachedSnapshot.getMin());
        assertEquals(20, secondCachedSnapshot.getMax());

        time.getAndAdd(1);
        reservoir.update(70);
        reservoir.update(80);
        RollingSnapshot firstNewSnapshot = reservoir.getSnapshot();
        assertNotSame(firstSnapshot, firstNewSnapshot);
        assertEquals(30, firstNewSnapshot.getMin());
        assertEquals(80, firstNewSnapshot.getMax());

        time.getAndAdd(1001);
        reservoir.update(90);
        reservoir.update(100);
        RollingSnapshot secondNewSnapshot = reservoir.getSnapshot();
        assertNotSame(firstNewSnapshot, secondNewSnapshot);
        assertEquals(90, secondNewSnapshot.getMin());
        assertEquals(100, secondNewSnapshot.getMax());
    }

}
