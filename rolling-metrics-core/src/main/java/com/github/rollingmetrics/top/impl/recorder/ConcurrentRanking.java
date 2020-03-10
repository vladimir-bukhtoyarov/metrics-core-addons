/*
 *    Copyright 2020 Vladimir Bukhtoyarov
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

package com.github.rollingmetrics.top.impl.recorder;

import com.github.rollingmetrics.blocks.BufferedActor;
import com.github.rollingmetrics.top.Position;

import java.util.List;

/**
 *  Is not a part of public API, this class just used as building block for other QueryTop implementations.
 */
public class ConcurrentRanking {

    private final SingleThreadedRanking singleThreadedRanking;
    private final BufferedActor<ModifyRankingAction> bufferedActor;

    protected ConcurrentRanking(int maxSize, long threshold) {
        this.singleThreadedRanking = new SingleThreadedRanking(maxSize, threshold);
        this.bufferedActor = new BufferedActor<>(ModifyRankingAction::new, 256, 256);
    }

    void update(long weight, Object identity) {
        if (weight < getThreshold()) {
            // the measure should be skipped because it is lesser then threshold
            return;
        }

        ModifyRankingAction action = bufferedActor.getActionFromPool();
        action.weight = weight;
        action.identity = identity;
        bufferedActor.doExclusivelyOrSchedule(action);
    }

    public int getMaxSize() {
        return singleThreadedRanking.getMaxSize();
    }

    public long getThreshold() {
        return singleThreadedRanking.getThreshold();
    }

    public ConcurrentRanking createEmptyCopy() {
        return new ConcurrentRanking(getMaxSize(), singleThreadedRanking.getThreshold());
    }

    public void resetUnsafe() {
        bufferedActor.clear();
        singleThreadedRanking.reset();
    }

    public void addIntoUnsafe(SingleThreadedRanking collector) {
        bufferedActor.processAllScheduladActions();
        singleThreadedRanking.addInto(collector);
    }

    public List<Position> getPositionsInDescendingOrderUnsafe() {
        bufferedActor.processAllScheduladActions();
        return singleThreadedRanking.getPositionsInDescendingOrder();
    }

    public class ModifyRankingAction extends BufferedActor.ReusableActionContainer {
        long weight;
        Object identity;

        @Override
        protected void freeGarbage() {
            // help gc
            this.identity = null;
            // other fields are not need in cleaning because they are primitives
        }

        @Override
        protected void run() {
            singleThreadedRanking.update(weight, identity);
        }

    }

}
