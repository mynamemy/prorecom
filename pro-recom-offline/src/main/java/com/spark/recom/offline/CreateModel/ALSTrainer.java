package com.spark.recom.offline.CreateModel;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.mllib.recommendation.MatrixFactorizationModel;
import org.apache.spark.mllib.recommendation.Rating;
import org.apache.spark.sql.SparkSession;
import scala.Tuple2;
import org.apache.spark.mllib.recommendation.ALS;

import java.util.Arrays;
import java.util.List;

public class ALSTrainer {
    public static void main(String[] args) {
        SparkSession spark = SparkSession.builder()   //连接spark
                .appName("RecommenderModel")
                .master("local[4]")
                .getOrCreate();
        //Spark读取文件
        JavaRDD<Rating> ratingsRDD = spark.read()
                .textFile("D:\\sample_movielens_data.txt")
                .javaRDD()
                .map(x -> OfflineRecommender.parseRating(x))  //对文件切分并放入数组  拿到相应的字段 返回Rating对象
                .cache();  //把rdd 缓存到内存

        //随机切分数据集,生成训练集和测试集
        JavaRDD<Rating>[] splits = ratingsRDD.randomSplit(new double[] {0.8, 0.2});  //训练与测试在文件中的比例==0.8：0.2
        JavaRDD<Rating> trainingRDD = splits[0];  //训练集
        JavaRDD<Rating> testRDD = splits[1];   //测试集

        //模型参数选择，输出最优参数
        adjustALSParam(trainingRDD, testRDD);

        spark.close();

    }

    private static void adjustALSParam(JavaRDD<Rating> trainingRDD, JavaRDD<Rating> testRDD) {
        List<Integer> ranks = (List<Integer>) Arrays.asList(20, 50, 100);  //rank要使用的特征数（也称为潜在因素数）
        List<Double> lambdas = (List<Double>)Arrays.asList(0.01, 0.1, 1.0); //lambda正则化参数
        List<Integer> numIters = (List<Integer>)Arrays.asList(5, 10, 15); //迭代次数
        //定义模型属性初始值
        MatrixFactorizationModel bestModel = null;
        double bestRMSE = Double.MAX_VALUE;
        int bestRank = 0;
        double bestLambda = -1.0;
        int bestNumIter = -1;
        //协同过滤算法的实现
        for (int i = 0; i < ranks.size(); i++) {
            MatrixFactorizationModel model =
                    ALS.train(JavaRDD.toRDD(trainingRDD),  //ALS 交替最小二乘法  //训练模型
                            ranks.get(i),
                            numIters.get(i),
                            lambdas.get(i));
            double rmse = ALSTrainer.getRMSE(model, testRDD); //调用平均均方根误差方法
            //判断是否为最佳模型
            if (rmse < bestRMSE) {
                bestModel = model;
                bestRMSE = rmse;
                bestRank = ranks.get(i);
                bestLambda = lambdas.get(i);
                bestNumIter = numIters.get(i);
            }
        }
        System.out.println("最佳模型：" + bestModel);
        System.out.println("最佳的RMSE："+bestRMSE);
        System.out.println("最佳的Lambda："+bestLambda);
        System.out.println("最佳的Rank："+bestRank);
        System.out.println("最佳的迭代次数Numit："+bestNumIter);

    }

    /**
     * 根据模型model计算data的平均均方根误差
     *
     * @param model
     * @param data
     * @param n
     * @return
     */
    public static double getRMSE(MatrixFactorizationModel model, JavaRDD<Rating> testRDD) {   // 参数为 模型 与测试数据

        //计算预测评分
        JavaRDD<Tuple2<Integer, Integer>> userProducts = testRDD.map(x -> new Tuple2<>(x.user(), x.product()));
        JavaRDD<Rating> predictRating = model.predict(JavaPairRDD.fromJavaRDD(userProducts));

        //以Tuple2(Tuple2(uid，mid),Tuple2(Rating,Rating))作为外键jion实际值和预测值
        JavaPairRDD<Tuple2<Integer, Integer>, Double> observed =   //实际值
                testRDD.mapToPair(x -> new Tuple2<> (new Tuple2<>(x.user(), x.product()), x.rating()));
        JavaPairRDD<Tuple2<Integer, Integer>, Double> predict =    //预测值
                predictRating.mapToPair(x -> new Tuple2<> (new Tuple2<>(x.user(), x.product()), x.rating()));
        //实际与预测join
        double rmse = Math.sqrt(observed.join(predict).mapToDouble(x -> {  //sprt 平方根   mapToDouble  RDD转换
            double err = x._2()._1() - x._2()._2();
            return err * err;
        }).mean());  //mean 平均值


        return rmse;
    }
}
