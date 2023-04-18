package com.mao.graphqlgenerator.dto;

import java.io.Serializable;

public class TableUIInfo implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 表名
     */
    private String tableName;
    /**
     * 类名
     */
    private String schemaName;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String className) {
        this.schemaName = className;
    }

    public TableUIInfo(String tableName, String className) {
        this.tableName = tableName;
        this.schemaName = className;
    }

    public TableUIInfo() {
    }
}