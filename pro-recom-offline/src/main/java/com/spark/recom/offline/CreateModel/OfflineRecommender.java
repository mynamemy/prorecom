package com.spark.recom.offline.CreateModel;

import com.spark.recom.common.MovieRecs;
import com.spark.recom.common.Recommendation;
import com.spark.recom.common.RedisUtil;
import com.spark.recom.common.UserRecs;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.mllib.recommendation.ALS;
import org.apache.spark.mllib.recommendation.MatrixFactorizationModel;
import org.apache.spark.mllib.recommendation.Rating;
import org.apache.spark.sql.SparkSession;
import org.jblas.DoubleMatrix;
import redis.clients.jedis.Jedis;
import scala.Tuple2;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class OfflineRecommender {
    public static void main(String[] args) {
        SparkSession spark = SparkSession.builder()
                .appName("RecommenderModel")
                .master("local[4]")
                .getOrCreate(); //
        JavaRDD<Rating> ratingsRDD = spark.read()//用spark读取文件
                .textFile("D:\\sample_movielens_data.txt")
                .javaRDD()
                .map(x -> parseRating(x)) //对文件切分并放入数组  拿到相应的字段 返回Rating对象
                .cache();
        //从ratings数据集中提取所有的uid和mid，并去重
        JavaRDD<Integer> userRDD = ratingsRDD.map(rating -> rating.user()).distinct(); //对用户提取并去重
        JavaRDD<Integer> movieRDD = ratingsRDD.map(rating -> rating.product()).distinct(); //对电影提取并去重

        //训练模型
        //rank要使用的特征数（也称为潜在因素数）, iterations als的迭代次数, lambda正则化参数
        MatrixFactorizationModel model = ALS.train(ratingsRDD.rdd(),50, 10, 0.1);

        //基于用户和电影特征，计算预测评分，得到用户的推荐列表
        //计算user和movies的笛卡尔积，得到一个空的评分矩阵
        JavaPairRDD<Integer, Integer> userMovies = userRDD.cartesian(movieRDD);

        //调用model的predict方法预测评分(uid,({mid,rating},{mid,rating},{mid,rating})
        JavaRDD<Rating> preRatings = model.predict(userMovies);
        JavaRDD<UserRecs> userRecs = preRatings
                //JavaPairRDD<Integer, Iterable<Tuple2<Integer, Double>>> groupByKey
                .filter(rating -> rating.rating() > 0)
                .mapToPair(rating -> new Tuple2<>(rating.user(),
                        new Tuple2<>(rating.product(), rating.rating())))
                .groupByKey()
                .map(t -> {
                    List<Recommendation> recs = new ArrayList<>();
//					Iterable<Tuple2<Integer, Double>> recs = t._2();
                    for (Tuple2<Integer, Double> rec : t._2()) {
                        recs.add(new Recommendation(rec._1(), rec._2()));
                    }
                    recs.sort(new Comparator<Recommendation>() {//推荐列表倒序排序
                        @Override
                        public int compare(Recommendation o1, Recommendation o2) {
                            return o1.getRating() > o2.getRating() ? -1 : 1;
                        }
                    });
                    if(recs.size() > 5)
                        recs = recs.subList(0, 5);//取出前五作为推荐
                    return new UserRecs(t._1(), recs);
                });

        userRecs.foreachPartition(userRecss -> {
            Jedis jedis = RedisUtil.getJedis();
            while(userRecss.hasNext()) {
                UserRecs recs = userRecss.next();
                jedis.set("U:" + String.valueOf(recs.getUid()), recs.getRecs().toString());
                Thread.sleep(500);
                System.out.println(recs.getUid() + "|||" + recs.getRecs().toString());
            }
        });

        //基于电影特征，计算相似度矩阵，得到电影的相似度列表
        JavaRDD<Tuple2<Integer, DoubleMatrix>> movieFeatures = model.productFeatures()
                .toJavaRDD()
                .map(feature -> new Tuple2<>((Integer)feature._1(), new DoubleMatrix(feature._2())));//Java的快速线性代数库,用于后面计算相似度
        //所有电影两两计算相似度（笛卡尔积），生成电影相似度矩阵
        JavaRDD<MovieRecs> movieRecs = movieFeatures
                .cartesian(movieFeatures)
                //Tuple2<Tuple2<Integer, DoubleMatrix>, Tuple2<Integer, DoubleMatrix>>
                .filter(v -> v._1()._1() != v._2()._1())
                .mapToPair(rating ->
                        new Tuple2<>(rating._1()._1(),
                                new Tuple2<>(rating._2()._1(),
                                        consinSim(rating._1()._2(), rating._2()._2()))))
                .filter(x -> x._2()._2() > 0.8)//过滤出相似度大于0.6的值
                .groupByKey()
                .map(t -> {
                    List<Recommendation> recs = new ArrayList<>();
                    for (Tuple2<Integer, Double> rec : t._2()) {
                        recs.add(new Recommendation(rec._1(), rec._2()));
                    }
                    recs.sort(new Comparator<Recommendation>() {//推荐列表倒序排序
                        @Override
                        public int compare(Recommendation o1, Recommendation o2) {
                            return o1.getRating() > o2.getRating() ? -1 : 1;
                        }
                    });
                    if(recs.size() > 5)
                        recs = recs.subList(0, 5);//取出前五作为推荐
                    return new MovieRecs(t._1(), recs);
                });
        movieRecs.foreachPartition(movieRecss -> {
            Jedis jedis = RedisUtil.getJedis();
            while(movieRecss.hasNext()) {
                MovieRecs recs = movieRecss.next();
                jedis.set("M:" + String.valueOf(recs.getMid()), recs.getRecs().toString());
                Thread.sleep(500);
                System.out.println(recs.getMid() + "///" + recs.getRecs().toString());
            }
        });

        spark.stop();

    }

    // 将数据解析为评分数据集
    public static Rating parseRating(String str) {
        String[] fields = str.split("::");
        if (fields.length < 3) {
            throw new IllegalArgumentException("Each line must contain 3 fields");
        }
        int userId = Integer.parseInt(fields[0]);
        int movieId = Integer.parseInt(fields[1]);
        double rating = Float.parseFloat(fields[2]);

        return new Rating(userId, movieId, rating);
    }

    public static double consinSim(DoubleMatrix movie1, DoubleMatrix movie2) {
        //分子是点乘，分母是模长的乘积
        double result = movie1.dot(movie2) / (movie1.norm2() * movie2.norm2());
        return result;
    }
}
