package com.spark.recom.common;

import java.util.List;

/**
 * 基于预测评分的推荐列表
 *  @author Administrator
 */
public class UserRecs {
    private int uid;  //电影id
    private List<Recommendation> recs;  //推荐电影集合

    public UserRecs(int uid, List<Recommendation> recs) {
        this.uid = uid;
        this.recs = recs;
    }

    public int getUid() {
        return uid;
    }
    public void setUid(int uid) {
        this.uid = uid;
    }
    public List<Recommendation> getRecs() {
        return recs;
    }
    public void setRecs(List<Recommendation> recs) {
        this.recs = recs;
    }
}
