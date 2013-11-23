package com.google.dart.util;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileNotFoundException;

@Test
public class TestPub {

    @Test
    public void testReadYamlFile() throws FileNotFoundException {
        Pub thePub = new Pub(new File(getClass().getResource("pubspec.yaml").getFile()));
        Assert.assertEquals(thePub.getName(), "darterop");
    }

}
