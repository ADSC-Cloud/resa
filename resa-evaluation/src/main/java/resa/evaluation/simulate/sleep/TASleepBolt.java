package resa.evaluation.simulate.sleep;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;

import java.util.Map;

/**
 * Created by ding on 14-1-27.
 */
public abstract class TASleepBolt extends BaseRichBolt {

    protected transient OutputCollector collector;
    private IntervalSupplier sleep;

    public TASleepBolt(IntervalSupplier sleep) {
        this.sleep = sleep;
    }

    public TASleepBolt() {
        this.sleep = () -> 0L;
    }

    protected void setIntervalSupplier(IntervalSupplier sleep) {
        this.sleep = sleep;
    }

    @Override
    public void prepare(Map map, TopologyContext context, OutputCollector outputCollector) {
        this.collector = outputCollector;
    }

    @Override
    public void execute(Tuple tuple) {
        long inter = this.sleep.get();
        if (inter > 0) {
            performSleep(inter);
        }
    }

    private void performSleep(long interval) {
        long stop = System.currentTimeMillis() + interval;
        do {
            for (int i = 0; i < 100; i++) {
                Math.sqrt(Math.random() * Integer.MAX_VALUE);
            }
        } while (System.currentTimeMillis() < stop);
    }
}
