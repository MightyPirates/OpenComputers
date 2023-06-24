package com.typesafe.config.impl;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import org.luaj.vm2.ast.Str;

import java.util.List;

public final class OpenComputersConfigCommentManipulationHook {
    private OpenComputersConfigCommentManipulationHook() {

    }

    public static Config setComments(Config config, String path, List<String> comments) {
        return config.withValue(path, setComments(config.getValue(path), comments));
    }

    public static ConfigValue setComments(ConfigValue value, List<String> comments) {
        if (value.origin() instanceof SimpleConfigOrigin && value instanceof AbstractConfigValue) {
            return ((AbstractConfigValue) value).withOrigin(
                ((SimpleConfigOrigin) value.origin()).setComments(comments)
            );
        } else {
            return value;
        }
    }
}
