package com.centurylink.mdw.plugin.designer.model;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.model.value.attribute.RuleSetVO;

public class Kotlin extends WorkflowAsset {
    public Kotlin() {
        super();
    }

    public Kotlin(RuleSetVO ruleSetVO, WorkflowPackage packageVersion) {
        super(ruleSetVO, packageVersion);
    }

    public Kotlin(Kotlin cloneFrom) {
        super(cloneFrom);
    }

    @Override
    public String getTitle() {
        return "Kotlin Source";
    }

    @Override
    public String getIcon() {
        return "kotlin.gif";
    }

    @Override
    public String getDefaultExtension() {
        return ".kt";
    }

    private static List<String> scriptLanguages;

    @Override
    public List<String> getLanguages() {
        if (scriptLanguages == null) {
            scriptLanguages = new ArrayList<>();
            scriptLanguages.add("Kotlin");
        }
        return scriptLanguages;
    }
}
