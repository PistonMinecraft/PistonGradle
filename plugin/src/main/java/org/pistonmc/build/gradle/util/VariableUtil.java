package org.pistonmc.build.gradle.util;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;
import java.util.Map;

public class VariableUtil {
    public static List<String> replaceVariables(List<String> args, Map<String, String> variables, boolean dollarBegin) {
        return replaceVariables(args, variables, new ObjectArrayList<>(args.size()), dollarBegin);
    }

    public static List<String> replaceVariables(List<String> args, Map<String, String> variables, List<String> target) {
        return replaceVariables(args, variables, target, true);
    }

    public static Map<String, String> replaceVariables(Map<String, String> args, Map<String, String> variables, boolean dollarBegin) {
        return replaceVariables(args, variables, new Object2ObjectOpenHashMap<>(args.size()), dollarBegin);
    }

    public static Map<String, String> replaceVariables(Map<String, String> args, Map<String, String> variables, Map<String, String> target, boolean dollarBegin) {
        for (String k : args.keySet()) {
            target.put(k, replaceVariable(args.get(k), variables, dollarBegin));
        }
        return target;
    }

    public static List<String> replaceVariables(List<String> args, Map<String, String> variables, List<String> target, boolean dollarBegin) {
        for (String arg : args) {
            target.add(replaceVariable(arg, variables, dollarBegin));
        }
        return target;
    }

    public static String replaceVariable(String arg, Map<String, String> variables, boolean dollarBegin) {
        StringBuilder sb = new StringBuilder(arg.length());
        final int l = dollarBegin ? 2 : 1;
        int last = 0;
        for (int i = arg.indexOf(dollarBegin ? "${" : "{"); i >= 0; i = arg.indexOf(dollarBegin ? "${" : "{", i)) {
            int j = arg.indexOf('}', i + l);
            if (j > i + l) {
                String value = variables.get(arg.substring(i + l, j));
                if (value != null) {
                    sb.append(arg, last, i).append(value);
                    last = j + 1;
                }
                i = j + 1;
            } else i += l;
        }
        return sb.append(arg, last, arg.length()).toString();
    }
}