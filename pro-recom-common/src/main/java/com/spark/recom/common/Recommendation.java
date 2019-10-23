package com.spark.recom.common;

/**
 *  一个基本的推荐对象
 *   @author Administrator
 */
public class Recommendation implements Comparable<Recommendation>{
    private int mid;   //电影id
    private double rating;  //电影评分等级

    public Recommendation(int mid, double rating) {
        this.mid = mid;
        this.rating = rating;
    }

    public int getMid() {
        return mid;
    }
    public void setMid(int mid) {
        this.mid = mid;
    }
    public double getRating() {
        return rating;
    }
    public void setRating(double rating) {
        this.rating = rating;
    }

    @Override
    public int compareTo(Recommendation rec) {
        return this.rating > rec.getRating() ? -1 : 1;
    }

    @Override
    public String toString() {
        return "{" + this.mid + "," + this.rating + "}";
    }
}
