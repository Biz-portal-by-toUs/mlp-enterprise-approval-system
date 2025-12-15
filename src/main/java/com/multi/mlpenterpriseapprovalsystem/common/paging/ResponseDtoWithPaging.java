package com.multi.mlpenterpriseapprovalsystem.common.paging;

public class ResponseDtoWithPaging {

    private Object data;
    private SelectCriteria pageInfo;

    public ResponseDtoWithPaging() {
    }

    public ResponseDtoWithPaging(Object data, SelectCriteria pageInfo) {
        this.data = data;
        this.pageInfo = pageInfo;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public SelectCriteria getPageInfo() {
        return pageInfo;
    }

    public void setPageInfo(SelectCriteria pageInfo) {
        this.pageInfo = pageInfo;
    }

    @Override
    public String toString() {
        return "ResponseDtoWithPaging{" +
                "data=" + data +
                ", pageInfo=" + pageInfo +
                '}';
    }
}
