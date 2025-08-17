module dev.mars.jtable.io.common {
    requires dev.mars.jtable.core;
    requires java.sql;
    requires org.slf4j;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;

    exports dev.mars.jtable.io.common.adapter;
    exports dev.mars.jtable.io.common.datasource;
    exports dev.mars.jtable.io.common.mapping;

    opens dev.mars.jtable.io.common.mapping to com.fasterxml.jackson.databind;
}
