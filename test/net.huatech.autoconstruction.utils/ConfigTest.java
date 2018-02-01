package net.huatech.autoconstruction.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConfigTest {

    @Test
    public void getTomcatName() {
        assertEquals("Tomcat 6.0.9", Config.getTomcatName());
    }
}