package resa.evaluation.topology.vld;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.opencv_features2d.KeyPoint;
import org.bytedeco.javacpp.opencv_nonfree.SIFT;
import resa.util.ConfigUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_highgui.cvDecodeImage;
import static resa.evaluation.topology.vld.Constant.*;

/**
 * Created by ding on 14-7-3.
 * This beta version is Modified by Tom Fu, on April 2016
 * We mainly re-design the topology to remove those broadcasting issue (all grouping), here for experimental purpose
 */
public class FeatureExtracterCharlie extends BaseRichBolt {

    private SIFT sift;
    private double[] buf;
    private OutputCollector collector;
    private int targetTaskNumber;
    private int groupNumber;

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        sift = new SIFT(500, 3, 0.05, 12, 1.6);
        buf = new double[128];
        this.collector = collector;
        targetTaskNumber = context.getComponentTasks("matcher").size();
        groupNumber = Math.min(ConfigUtil.getInt(stormConf, "vd-group.count", 1), targetTaskNumber);
    }

    @Override
    public void execute(Tuple input) {
        byte[] imgBytes = (byte[]) input.getValueByField(FIELD_IMG_BYTES);
        IplImage image = cvDecodeImage(cvMat(1, imgBytes.length, CV_8UC1, new BytePointer(imgBytes)));
        KeyPoint points = new KeyPoint();
        Mat featureDesc = new Mat();
        Mat matImg = new Mat(image);
        sift.detect(matImg, points);
        sift.compute(matImg, points, featureDesc);
        try {
            cvReleaseImage(image);
        } catch (Exception e) {
        }
        int rows = featureDesc.rows();
        int totalCount = 0;

        List<List<byte[]>> toSend = new ArrayList<>();
        for (int i = 0; i < groupNumber; i ++){
            List<byte[]> selected = new ArrayList<>();
            toSend.add(selected);
        }
        for (int i = 0; i < rows; i++) {
            featureDesc.rows(i).asCvMat().get(buf);
            // compress data
            byte[] siftFeat = new byte[buf.length];
            for (int j = 0; j < buf.length; j++) {
                siftFeat[j] = (byte) (((int) buf[j]) & 0xFF);
            }
            int tIndex = i % groupNumber;
            toSend.get(tIndex).add(siftFeat);
        }

        String frameId = input.getStringByField(FIELD_FRAME_ID);
        for (int i = 0; i < toSend.size(); i++) {
            collector.emit(STREAM_FEATURE_DESC, input, new Values(frameId, toSend.get(i), rows, groupNumber));
        }
        collector.ack(input);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declareStream(STREAM_FEATURE_DESC, new Fields(FIELD_FRAME_ID, FIELD_FEATURE_DESC, FIELD_FEATURE_CNT, SEND_GROUP_CNT));
    }
}