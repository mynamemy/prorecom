package com.spark.recom.common;

import java.util.List;

/**
 *���ڵ�Ӱ���������ĵ�Ӱ���Ƽ������ƶ��б�
 * @author Administrator
 */
public class MovieRecs {
    private int mid;  //��ӰID
    private List<Recommendation> recs;  //��Ӱ���ƶȵļ��϶���
    //������
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
