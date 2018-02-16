package com.github.ustc_zzzz.cbm.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author ustc_zzzz
 */
public final class Rule
{
    private final boolean isAllowed;
    private final int rulePartLength;
    private final List<Pattern> ruleParts;

    Rule(boolean isAllowed, String[] ruleParts)
    {
        this.isAllowed = isAllowed;
        List<Pattern> patternList = new ArrayList<>();
        for (String rulePart : ruleParts)
        {
            patternList.add(wildcardToRegex(rulePart));
        }
        this.rulePartLength = patternList.size();
        this.ruleParts = Collections.unmodifiableList(patternList);
    }

    public boolean isAllowed()
    {
        return this.isAllowed;
    }

    public boolean isDisallowed()
    {
        return !this.isAllowed();
    }

    public boolean matchRuleParts(String[] input)
    {
        if (this.rulePartLength <= input.length)
        {
            for (int i = 0; i < this.rulePartLength; ++i)
            {
                if (!this.ruleParts.get(i).matcher(input[i]).matches())
                {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static Pattern wildcardToRegex(String wildcardPattern)
    {
        return Pattern.compile('^' + Pattern.quote(wildcardPattern
                .replaceAll("\\\\", "\\\\\\\\"))
                .replaceAll("((^|[^\\\\]|\\\\[^\\\\])(\\\\\\\\\\\\\\\\)*)\\*", "$1\\\\E.*\\\\Q")
                .replaceAll("((^|[^\\\\]|\\\\[^\\\\])(\\\\\\\\\\\\\\\\)*)\\?", "$1\\\\E.\\\\Q")
                .replaceAll("\\\\\\\\\\\\\\\\", "\\\\\\\\\\\\")
                .replaceAll("\\\\\\\\(.)", "$1") + '$');
    }
}
