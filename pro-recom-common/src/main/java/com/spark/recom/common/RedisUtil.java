package com.spark.recom.common;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public final class RedisUtil {

    //Redis������ip��������
    private static String ADDR = Constants.REDIS_SERVER;

    //Redis�˿ں�
    private static int PORT = 6378;

    //��������  ��������
    private static String AUTH = "admin";
    /**
     * redis�������ӳ�
     */
    //��������ʵ���������Ŀ��Ĭ��ֵΪ8��
    //�����ֵΪ-1�����ʾ�����ƣ����pool�Ѿ�������maxActive��jedisʵ�������ʱpool��״̬Ϊexhausted(�ľ�)��
    private static int MAX_ACTIVE = 1024;

    //����һ��pool����ж��ٸ�״̬Ϊidle(���е�)��jedisʵ����Ĭ��ֵҲ��8��
    private static int MAX_IDLE = 200;

    //�ȴ��������ӵ����ʱ�䣬��λ���룬Ĭ��ֵΪ-1����ʾ������ʱ����������ȴ�ʱ�䣬��ֱ���׳�JedisConnectionException��
    private static int MAX_WAIT = 10000;   //���ȴ�ʱ��

    private static int TIMEOUT = 10000;   //���ʱʱ��

    private static JedisPool jedisPool = null;

    /**
     * ��ʼ��Redis���ӳ�
     */
    static {    //��̬�����ֻҪ�������ڵ���Ϳ�ʼ����ִ�� ִֻ��һ��   //��ִ�и���̬��������  �ڸ�����������
        try {
            JedisPoolConfig config = new JedisPoolConfig();  //���ö���
            config.setMaxIdle(MAX_IDLE);   //��������jedisʵ��
            jedisPool = new JedisPool(config, ADDR, PORT, TIMEOUT);    // ����   ����  �˿ں�   ���ʱʱ��
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ��ȡJedisʵ��   //����ģʽ
     * @return
     */
    public synchronized static Jedis getJedis() {   //ͬ����
        try {
            if (jedisPool != null) {
                Jedis resource = jedisPool.getResource();
                return resource;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * �ͷ�jedis��Դ
     * @param jedis
     */
    public static void returnResource(final Jedis jedis) {
        jedisPool.close();

    }
}
