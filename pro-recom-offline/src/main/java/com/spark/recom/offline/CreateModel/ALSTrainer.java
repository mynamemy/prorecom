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
        SparkSession spark = SparkSession.builder()   //����spark
                .appName("RecommenderModel")
                .master("local[4]")
                .getOrCreate();
        //Spark��ȡ�ļ�
        JavaRDD<Rating> ratingsRDD = spark.read()
                .textFile("D:\\sample_movielens_data.txt")
                .javaRDD()
                .map(x -> OfflineRecommender.parseRating(x))  //���ļ��зֲ���������  �õ���Ӧ���ֶ� ����Rating����
                .cache();  //��rdd ���浽�ڴ�

        //����з����ݼ�,����ѵ�����Ͳ��Լ�
        JavaRDD<Rating>[] splits = ratingsRDD.randomSplit(new double[] {0.8, 0.2});  //ѵ����������ļ��еı���==0.8��0.2
        JavaRDD<Rating> trainingRDD = splits[0];  //ѵ����
        JavaRDD<Rating> testRDD = splits[1];   //���Լ�

        //ģ�Ͳ���ѡ��������Ų���
        adjustALSParam(trainingRDD, testRDD);

        spark.close();

    }

    private static void adjustALSParam(JavaRDD<Rating> trainingRDD, JavaRDD<Rating> testRDD) {
        List<Integer> ranks = (List<Integer>) Arrays.asList(20, 50, 100);  //rankҪʹ�õ���������Ҳ��ΪǱ����������
        List<Double> lambdas = (List<Double>)Arrays.asList(0.01, 0.1, 1.0); //lambda���򻯲���
        List<Integer> numIters = (List<Integer>)Arrays.asList(5, 10, 15); //��������
        //����ģ�����Գ�ʼֵ
        MatrixFactorizationModel bestModel = null;
        double bestRMSE = Double.MAX_VALUE;
        int bestRank = 0;
        double bestLambda = -1.0;
        int bestNumIter = -1;
        //Эͬ�����㷨��ʵ��
        for (int i = 0; i < ranks.size(); i++) {
            MatrixFactorizationModel model =
                    ALS.train(JavaRDD.toRDD(trainingRDD),  //ALS ������С���˷�  //ѵ��ģ��
                            ranks.get(i),
                            numIters.get(i),
                            lambdas.get(i));
            double rmse = ALSTrainer.getRMSE(model, testRDD); //����ƽ������������
            //�ж��Ƿ�Ϊ���ģ��
            if (rmse < bestRMSE) {
                bestModel = model;
                bestRMSE = rmse;
                bestRank = ranks.get(i);
                bestLambda = lambdas.get(i);
                bestNumIter = numIters.get(i);
            }
        }
        System.out.println("���ģ�ͣ�" + bestModel);
        System.out.println("��ѵ�RMSE��"+bestRMSE);
        System.out.println("��ѵ�Lambda��"+bestLambda);
        System.out.println("��ѵ�Rank��"+bestRank);
        System.out.println("��ѵĵ�������Numit��"+bestNumIter);

    }

    /**
     * ����ģ��model����data��ƽ�����������
     *
     * @param model
     * @param data
     * @param n
     * @return
     */
    public static double getRMSE(MatrixFactorizationModel model, JavaRDD<Rating> testRDD) {   // ����Ϊ ģ�� ���������

        //����Ԥ������
        JavaRDD<Tuple2<Integer, Integer>> userProducts = testRDD.map(x -> new Tuple2<>(x.user(), x.product()));
        JavaRDD<Rating> predictRating = model.predict(JavaPairRDD.fromJavaRDD(userProducts));

        //��Tuple2(Tuple2(uid��mid),Tuple2(Rating,Rating))��Ϊ���jionʵ��ֵ��Ԥ��ֵ
        JavaPairRDD<Tuple2<Integer, Integer>, Double> observed =   //ʵ��ֵ
                testRDD.mapToPair(x -> new Tuple2<> (new Tuple2<>(x.user(), x.product()), x.rating()));
        JavaPairRDD<Tuple2<Integer, Integer>, Double> predict =    //Ԥ��ֵ
                predictRating.mapToPair(x -> new Tuple2<> (new Tuple2<>(x.user(), x.product()), x.rating()));
        //ʵ����Ԥ��join
        double rmse = Math.sqrt(observed.join(predict).mapToDouble(x -> {  //sprt ƽ����   mapToDouble  RDDת��
            double err = x._2()._1() - x._2()._2();
            return err * err;
        }).mean());  //mean ƽ��ֵ


        return rmse;
    }
}
