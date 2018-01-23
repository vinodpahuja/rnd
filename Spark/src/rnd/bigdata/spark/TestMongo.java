package rnd.bigdata.spark;
import java.util.Arrays;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.VoidFunction;
import org.bson.Document;

import com.mongodb.spark.MongoSpark;
import com.mongodb.spark.rdd.api.java.JavaMongoRDD;

public class TestMongo {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		JavaMongoRDD<Document> rdd = getJavaMongoRDD("HeroOrder", "HeroOrderRec");
		System.out.println("full :" + rdd.count());

		JavaMongoRDD<Document> aggRDD = rdd//
				.withPipeline(Arrays.asList(Document.parse("{ $match: { creationDateMonth : 1 } }"), //
						Document.parse("{" + //
								"	$group: { " + //
								"		_id : { " + //
								"			year : '$creationDateYear' , " + //
								"			month : '$creationDateMonth', " + //
								"			userName : '$userName' " + //
								"		}, " + //
								"		year : { $first : '$creationDateYear' }, " + //
								"		month : { $first : '$creationDateMonth' }, " + //
								"		userName : { $first : '$userName' }, " + //
								"		count : { $sum : 1 }" + //
								"	}" + //
								"}")));
		System.out.println("aggr count :" + aggRDD.count());

		aggRDD.foreach(new VoidFunction<Document>() {
			@Override
			public void call(Document t) throws Exception {
				System.out.println(t);
			}
		});

	}

	public static JavaMongoRDD<Document> getJavaMongoRDD(String inCollName, String outCollName) {
		SparkConf conf = new SparkConf().//
				setAppName("rnd").//
				setMaster("local").//
				set("spark.mongodb.input.uri", "mongodb://127.0.0.1:27017/applicatedb." + inCollName).//
				set("spark.mongodb.output.uri", "mongodb://127.0.0.1/applicatedb." + outCollName);

		JavaSparkContext jsc = new JavaSparkContext(conf);
		JavaMongoRDD<Document> rdd = MongoSpark.load(jsc);
		return rdd;
	}

}
