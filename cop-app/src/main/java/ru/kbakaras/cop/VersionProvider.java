package ru.kbakaras.cop;

import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class VersionProvider implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() {

        try (InputStream is = getClass().getClassLoader().getResourceAsStream("version.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                return new String[]{props.getProperty("version", "unknown")};
            }

        } catch (IOException ignored) {
        }

        return new String[]{"unknown"};
    }

}
