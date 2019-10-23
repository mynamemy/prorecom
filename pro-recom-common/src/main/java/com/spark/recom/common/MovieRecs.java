package com.spark.recom.common;

import java.util.List;

/**
 *基于电影特征向量的电影（推荐）相似度列表
 * @author Administrator
 */
public class MovieRecs {
    private int mid;  //电影ID
    private List<Recommendation> recs;  //电影相似度的集合对象
    //构造器
    public MovieRecs(int mid, List<Recommendation> recs) {
        this.mid = mid;
        this.recs = recs;
    }

    public int getMid() {
        return mid;
    }
    public void setMid(int mid) {
        this.mid = mid;
    }
    public List<Recommendation> getRecs() {
        return recs;
    }
    public void setRecs(List<Recommendation> recs) {
        this.recs = recs;
    }
}
