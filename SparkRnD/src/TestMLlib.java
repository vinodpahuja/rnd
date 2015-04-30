import java.util.List;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.mllib.clustering.KMeans;
import org.apache.spark.mllib.clustering.KMeansModel;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;

public class TestMLlib {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		SparkConf conf = new SparkConf().setAppName("rnd").setMaster("local");
		JavaSparkContext sc = new JavaSparkContext(conf);

		JavaRDD<String> stocks = sc.textFile("target/classes/data/stocks.csv");

		long totalStocks = stocks.count();

		System.out.println("Total Stocks" + totalStocks);

		JavaRDD<Vector> parsedData = stocks.map(new Function<String, Vector>() {
			public Vector call(String s) {
				String[] sarray = s.split(",");
				return Vectors.dense(new Double(sarray[sarray.length - 1]));
			}
		});
		parsedData.cache();

		int numClusters = 5;
		int numIterations = 1;
		KMeansModel clusters = KMeans.train(parsedData.rdd(), numClusters, numIterations);

		System.out.println("Cluster centers:");

		int i = 1;
		for (Vector center : clusters.clusterCenters()) {
			System.out.println(i++ + " =>" + center.size());
		}

		List<Integer> array = clusters.predict(parsedData).toArray();
		
		i = 1;
		for (Integer integer : array) {
			System.out.println(i++ + " =>" + integer);
		}

	}

}
