package rnd.bigdata.spark.ml;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.mllib.recommendation.ALS;
import org.apache.spark.mllib.recommendation.MatrixFactorizationModel;
import org.apache.spark.mllib.recommendation.Rating;
import org.bson.Document;

import rnd.bigdata.spark.TestMongo;
import scala.Tuple2;

import com.mongodb.spark.rdd.api.java.JavaMongoRDD;

@SuppressWarnings({ "serial", "rawtypes", "resource" })
public class SalesRecEngineMongo {

	public static void main(String[] args) {

		// Turn off unnecessary logging
		java.util.logging.Logger.getGlobal().setLevel(java.util.logging.Level.OFF);
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);

		// Read order file. format - user, product, quantity, amount
		JavaMongoRDD<Document> orderRDD = TestMongo.getJavaMongoRDD("HeroOrder", "HeroOrderRec");

		// Convert into rating file, format - user, product, rating (count)
		final JavaMongoRDD<Document> ratingRDD = orderRDD//
				.withPipeline(Arrays.asList(//
						Document.parse("{ " + //
								"	$match : { creationDateMonth : 1 } }"), //
						Document.parse("{" + //
								"	$group : { " + //
								"		_id : { " + //
								"			user : '$userName', " + //
								"			product : '$productCode' " + //
								"		}, " + //
								"		user : { $first : '$userName' }, " + //
								"		product : { $first : '$productCode' }, " + //
								// " rating : { $sum : 1 }" + // count
								// " rating : { $sum : '$quantity' }" + // Volume
								"		rating : { $sum : '$amount' }" + // Value
								"	}" + //
								"}")));

		final Map[] userMap = createDistinctMap(ratingRDD, "user");
		final Map[] productMap = createDistinctMap(ratingRDD, "product");

		// Map file to Ratings(user,item,rating) tuples
		JavaRDD<Rating> ratings = ratingRDD.map(new Function<Document, Rating>() {
			public Rating call(Document d) {
				return new Rating((int) userMap[0].get(d.getString("user")), (int) productMap[0].get(d.getString("product")), ((Number) d.get("rating")).doubleValue());
			}
		});

		// Build the recommendation model using ALS

		int rank = 10; // 10 latent factors
		int numIterations = 10; // number of iterations

		MatrixFactorizationModel model = ALS.train(JavaRDD.toRDD(ratings), rank, numIterations);
		// ALS.trainImplicit(arg0, arg1, arg2)

		// Create user-item tuples from ratings
		JavaRDD<Tuple2<Object, Object>> userProducts = ratings.map(new Function<Rating, Tuple2<Object, Object>>() {
			public Tuple2<Object, Object> call(Rating r) {
				return new Tuple2<Object, Object>(r.user(), r.product());
			}
		});

		// Predict the ratings of the items not rated by user for the user
		JavaRDD<Rating> recomondations = model.predict(userProducts.rdd()).toJavaRDD().distinct();

		// Sort the recommendations by rating in descending order
		recomondations = recomondations.sortBy(new Function<Rating, Double>() {
			@Override
			public Double call(Rating v1) throws Exception {
				return v1.rating();
			}

		}, false, 1);

		JavaRDD<Rating> topRecomondations = new JavaSparkContext(recomondations.context()).parallelize(recomondations.take(10));

		// Print the top recommendations for user 1.
		topRecomondations.foreach(new VoidFunction<Rating>() {
			@Override
			public void call(Rating rating) throws Exception {
				String str = "User : " + userMap[1].get(rating.user()) + //
				" Product : " + productMap[1].get(rating.product()) + //
				" Rating : " + rating.rating();
				System.out.println(str);
			}
		});

	}

	private static Map[] createDistinctMap(JavaMongoRDD<Document> ratingRDD, final String key) {

		final Map<String, Integer> map = new HashMap<String, Integer>();
		final Map<Integer, String> reverseMap = new HashMap<Integer, String>();

		ratingRDD.map(new Function<Document, String>() {
			public String call(Document v1) throws Exception {
				return v1.getString(key);
			};
		}).distinct().collect().forEach(new Consumer<String>() {
			
			int sequence = 0;
			public void accept(String user) {
				map.put(user, sequence);
				reverseMap.put(sequence++, user);
			};
		});

		return new Map[] { map, reverseMap };
	}
}