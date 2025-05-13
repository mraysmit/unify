module dev.mars.jtable.io.files {
    requires dev.mars.jtable.core;
    requires dev.mars.jtable.io.common;
    requires com.fasterxml.jackson.databind;
    requires java.sql;
    requires org.slf4j;
    requires ch.qos.logback.classic;

    exports dev.mars.jtable.io.files.csv;
    exports dev.mars.jtable.io.files.jdbc;
    exports dev.mars.jtable.io.files.json;
    exports dev.mars.jtable.io.files.mapping;
    exports dev.mars.jtable.io.files.nosql;
    exports dev.mars.jtable.io.files.rest;
    exports dev.mars.jtable.io.files.xml;
}
