import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.SQLContext;

public class TestSQL {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		SparkConf conf = new SparkConf().setAppName("rnd").setMaster("local");
		JavaSparkContext jsc = new JavaSparkContext(conf);
		SQLContext sc = new SQLContext(jsc);

		JavaRDD<String> cars = jsc.textFile("target/classes/data/cars.json");

		DataFrame carsJSON = sc.jsonRDD(cars);

		// DataFrame carsJSON = sc.jsonFile("target/classes/data/cars.json");

		carsJSON.show();

		carsJSON.groupBy("Cylinders").count().show();

	}
}
