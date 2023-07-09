package org.pistonmc.build.gradle.util;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;
import java.util.Map;

public class VariableUtil {
    public static List<String> replaceVariables(List<String> args, Map<String, String> variables) {
        var ret = new ObjectArrayList<String>(args.size());
        for (String arg : args) {
            StringBuilder sb = new StringBuilder(arg.length());
            int last = 0;
            for (int i = arg.indexOf("${"); i >= 0; i = arg.indexOf("${", last)) {
                int j = arg.indexOf('}', i + 2);
                if (j > i + 2) {
                    String value = variables.get(arg.substring(i + 2, j));
                    if (value != null) {
                        sb.append(arg, last, i).append(value);
                        last = j + 1;
                    }
                }
            }
            ret.add(sb.append(arg, last, arg.length()).toString());
        }
        return ret;
    }
}