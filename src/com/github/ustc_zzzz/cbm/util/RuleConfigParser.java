package com.github.ustc_zzzz.cbm.util;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;

import java.io.*;
import java.util.List;
import java.util.Objects;

/**
 * @author ustc_zzzz
 */
public final class RuleConfigParser
{
    public List<RulePermissionPair> parse(File configurationFile, String defaultConfig) throws IOException
    {
        // noinspection ResultOfMethodCallIgnored
        Objects.requireNonNull(configurationFile).getParentFile().mkdirs();
        if (configurationFile.createNewFile())
        {
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(configurationFile), Charsets.UTF_8))
            {
                writer.write(defaultConfig);
            }
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(configurationFile), Charsets.UTF_8))
        {
            return this.parse(reader);
        }
    }

    public List<RulePermissionPair> parse(Reader reader) throws IOException
    {
        int lineNumber = 0;
        String permissionContext = "*";
        BufferedReader bufferedReader = new BufferedReader(reader);
        ImmutableList.Builder<RulePermissionPair> builder = ImmutableList.builder();
        for (String newLine = ""; Objects.nonNull(newLine); ++lineNumber, newLine = bufferedReader.readLine())
        {
            String line = trim(newLine);
            if (!line.isEmpty())
            {
                switch (line.charAt(0))
                {
                case '#':
                    break; // just comments
                case '[':
                {
                    int lastIndex = line.length() - 1;
                    if (line.charAt(lastIndex) != ']')
                    {
                        throw new UnsupportedLineException(lineNumber, line);
                    }
                    String context = trim(line.substring(1, lastIndex));
                    if (context.isEmpty())
                    {
                        throw new UnsupportedLineException(lineNumber, line);
                    }
                    permissionContext = context;
                    break;
                }
                case '+':
                {
                    String[] ruleParts = trim(line.substring(1)).split("\\s+");
                    builder.add(new RulePermissionPair(new Rule(true, ruleParts), permissionContext));
                    break;
                }
                case '-':
                {
                    String[] ruleParts = trim(line.substring(1)).split("\\s+");
                    builder.add(new RulePermissionPair(new Rule(false, ruleParts), permissionContext));
                    break;
                }
                default:
                    throw new UnsupportedLineException(lineNumber, line);
                }
            }
        }
        return builder.build();
    }

    private static String trim(String string)
    {
        return string.replaceAll("^\\s+", "").replaceAll("\\s+$", "");
    }

    private static final class UnsupportedLineException extends IOException
    {
        private UnsupportedLineException(int lineNumber, String line)
        {
            super("Unrecognized config at line " + lineNumber + ": " + line);
        }
    }
}
