package resa.evaluation.topology.fp;

import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import resa.topology.RedisQueueSpout;


/**
 * Created by ding on 14-6-5.
 */
public class SentenceSpout extends RedisQueueSpout implements Constant {

    public SentenceSpout(String host, int port, String queue) {
        super(host, port, queue);
    }

    @Override
    protected void emitData(Object data) {
        String text = (String) data;
        collector.emit(new Values(((String) data).substring(1), text.charAt(0) == '+'), "");
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields(SENTENCE_FIELD, IS_ADD_FIELD));
    }
}
