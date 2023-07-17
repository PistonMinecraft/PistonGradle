package org.pistonmc.build.gradle.util;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;
import java.util.Map;

public class VariableUtil {
    public static List<String> replaceVariables(List<String> args, Map<String, String> variables) {
        return replaceVariables(args, variables, new ObjectArrayList<>(args.size()), true);
    }

    public static List<String> replaceVariables(List<String> args, Map<String, String> variables, boolean dollarBegin) {
        return replaceVariables(args, variables, new ObjectArrayList<>(args.size()), dollarBegin);
    }

    public static List<String> replaceVariables(List<String> args, Map<String, String> variables, List<String> target) {
        return replaceVariables(args, variables, target, true);
    }

    public static List<String> replaceVariables(List<String> args, Map<String, String> variables, List<String> target, boolean dollarBegin) {
        for (String arg : args) {
            target.add(replaceVariable(arg, variables, dollarBegin));
        }
        return target;
    }

    public static String replaceVariable(String arg, Map<String, String> variables, boolean dollarBegin) {
        StringBuilder sb = new StringBuilder(arg.length());
        int last = 0;
        for (int i = arg.indexOf(dollarBegin ? "${" : "{"); i >= 0; i = arg.indexOf(dollarBegin ? "${" : "{", last)) {
            int j = arg.indexOf('}', i + 2);
            if (j > i + 2) {
                String value = variables.get(arg.substring(i + 2, j));
                if (value != null) {
                    sb.append(arg, last, i).append(value);
                    last = j + 1;
                }
            }
        }
        return sb.append(arg, last, arg.length()).toString();
    }
}