module dev.mars.jtable.io.common {
    requires dev.mars.jtable.core;
    requires java.sql;
    requires org.slf4j;

    exports dev.mars.jtable.io.common.adapter;
    exports dev.mars.jtable.io.common.datasource;

}
