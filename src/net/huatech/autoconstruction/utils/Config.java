package net.huatech.autoconstruction.utils;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Map;

public class Config {
    private static Map<String, String> map;

    static {
        Yaml yaml = new Yaml();
        URL url = Config.class.getResource("/config.yml");
        try {
            map = (Map<String, String>) yaml.load(new FileInputStream(url.getFile()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static String getTomcatName() {
        return map.get("tomcat_name");
    }
}
