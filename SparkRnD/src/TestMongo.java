import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SQLContext;
import org.bson.Document;

import com.mongodb.spark.MongoSpark;
import com.mongodb.spark.rdd.api.java.JavaMongoRDD;

public class TestMongo {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		SparkConf conf = new SparkConf().//
				setAppName("rnd").//
				setMaster("local").//
				set("spark.mongodb.input.uri",
						"mongodb://admin:ruchisoya2admin@205.147.97.175:18018/ruchisoya2db.Order").//
				set("spark.mongodb.output.uri",
						"mongodb://127.0.0.1/applicatedb.OrderSummary");

		JavaSparkContext jsc = new JavaSparkContext(conf);
		SQLContext sc = new SQLContext(jsc);

		JavaMongoRDD<Document> rdd = MongoSpark.load(jsc);

		System.out.println(rdd.count());



	}

}
