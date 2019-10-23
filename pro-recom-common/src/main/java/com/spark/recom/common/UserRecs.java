package com.spark.recom.common;

import java.util.List;

/**
 * ����Ԥ�����ֵ��Ƽ��б�
 *  @author Administrator
 */
public class UserRecs {
    private int uid;  //��Ӱid
    private List<Recommendation> recs;  //�Ƽ���Ӱ����

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
