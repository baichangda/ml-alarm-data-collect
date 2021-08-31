package com.bcd.base.support_jdbc.sql;

import java.util.List;

public class UpdateSqlResult {
    private String sql;
    private List paramList;

    public UpdateSqlResult(String sql, List paramList) {
        this.sql = sql;
        this.paramList = paramList;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public List getParamList() {
        return paramList;
    }

    public void setParamList(List paramList) {
        this.paramList = paramList;
    }
}
