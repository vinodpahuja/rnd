import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

public class TestStreaming {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		SparkConf conf = new SparkConf().setAppName("rnd").setMaster("local");

		JavaStreamingContext jssc = new JavaStreamingContext(conf, Durations.seconds(1));


	}
}
