package com.google.dart.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.util.Locale;

/**
 * @author Daniel Zwicker
 */
public class OsUtil {

    private static final String OS_NAME = System.getProperty("os.name")
        .toLowerCase(Locale.US);

    private static final String OS_ARCH = System.getProperty("os.arch")
        .toLowerCase(Locale.US);

    public static boolean isWindows() {
        return OS_NAME.indexOf("win") >= 0;
    }

    public static boolean isMac() {
        return OS_NAME.indexOf("mac") >= 0;

    }

    public static boolean isUnix() {
        return OS_NAME.indexOf("nix") >= 0 || OS_NAME.indexOf("nux") >= 0;
    }

    public static boolean isSolaris() {
        return OS_NAME.indexOf("sunos") >= 0;
    }

    public static boolean isArch64() {
        return OS_ARCH.contains("64");
    }
}
