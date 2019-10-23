package com.spark.recom.common;

/**
 * hadoop连接配置
 */
public class Constants {
    public static final String REDIS_SERVER = "bigdata78";  //Hadoop主机
    public static final String KAFKA_SERVER = "bigdata78";  //kafka主机
    public static final String KAFKA_ADDR = KAFKA_SERVER + ":9092";  // kafka主机+端口号
    public static final String KAFKA_TOPICS = "recom";    //kafka tiop
}
