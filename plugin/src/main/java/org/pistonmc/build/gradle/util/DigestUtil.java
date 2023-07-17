package org.pistonmc.build.gradle.util;

import java.security.MessageDigest;

public class DigestUtil {
    public static StringBuilder calculateHash(MessageDigest md) {
        StringBuilder hash = new StringBuilder();
        for (byte b : md.digest()) {
            String hex = Integer.toHexString(Byte.toUnsignedInt(b));
            if(hex.length() < 2) hash.append('0');
            hash.append(hex);
        }
        return hash;
    }
}