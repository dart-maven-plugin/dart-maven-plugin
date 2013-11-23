package com.google.dart.util;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;

public class Pub {

    protected final HashMap data;
    protected final File yamlFile;

    public Pub(File file) throws FileNotFoundException {
        this.yamlFile = file;

        FileInputStream fileInputStream = new FileInputStream(file);
        Yaml yaml = new Yaml();
        this.data = (HashMap) yaml.load(fileInputStream);
    }

    public String getName() {
        return (String) data.get("name");
    }

    /**
     * The directory this pubspec is in.
     */
    public File getPath() {
        return yamlFile.getParentFile();
    }
}
