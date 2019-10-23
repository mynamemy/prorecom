package com.spark.recom.common;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public final class RedisUtil {

    //Redis服务器ip或主机名
    private static String ADDR = Constants.REDIS_SERVER;

    //Redis端口号
    private static int PORT = 6378;

    //访问密码  主机密码
    private static String AUTH = "admin";
    /**
     * redis数据连接池
     */
    //可用连接实例的最大数目，默认值为8；
    //如果赋值为-1，则表示不限制；如果pool已经分配了maxActive个jedis实例，则此时pool的状态为exhausted(耗尽)。
    private static int MAX_ACTIVE = 1024;

    //控制一个pool最多有多少个状态为idle(空闲的)的jedis实例，默认值也是8。
    private static int MAX_IDLE = 200;

    //等待可用连接的最大时间，单位毫秒，默认值为-1，表示永不超时。如果超过等待时间，则直接抛出JedisConnectionException；
    private static int MAX_WAIT = 10000;   //最大等待时长

    private static int TIMEOUT = 10000;   //最大超时时长

    private static JedisPool jedisPool = null;

    /**
     * 初始化Redis连接池
     */
    static {    //静态代码块只要加载所在的类就开始进行执行 只执行一次   //先执行父静态代码块后子  在父构造器后子
        try {
            JedisPoolConfig config = new JedisPoolConfig();  //配置对象
            config.setMaxIdle(MAX_IDLE);   //设置最大的jedis实例
            jedisPool = new JedisPool(config, ADDR, PORT, TIMEOUT);    // 对象   主机  端口号   最大超时时长
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取Jedis实例   //单例模式
     * @return
     */
    public synchronized static Jedis getJedis() {   //同步锁
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
     * 释放jedis资源
     * @param jedis
     */
    public static void returnResource(final Jedis jedis) {
        jedisPool.close();

    }
}
