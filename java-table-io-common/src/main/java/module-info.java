module dev.mars.jtable.io.common {
    requires dev.mars.jtable.core;
    requires java.sql;

    exports dev.mars.jtable.io.common.adapter;
    exports dev.mars.jtable.io.common.datasource;
    exports dev.mars.jtable.io.common.rest;
}
