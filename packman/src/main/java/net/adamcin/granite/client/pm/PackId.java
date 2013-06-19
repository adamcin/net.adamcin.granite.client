package net.adamcin.granite.client.pm;

import com.day.jcr.vault.maven.pack.PackageId;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class PackId {
    public static final String PROPERTIES_ENTRY = "META-INF/vault/properties.xml";
    public static final String PROP_GROUP = "group";
    public static final String PROP_NAME = "name";
    public static final String PROP_VERSION = "version";
    public static final String PROP_PATH = "path";

    private final String group;
    private final String name;
    private final String version;
    private final String installationPath;

    private PackId(final String group, final String name, final String version, final String installationPath) {
        this.group = group;
        this.name = name;
        this.version = version;
        this.installationPath = installationPath;
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getInstallationPath() {
        return installationPath;
    }

    public static PackId identifyPackage(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("file");
        }

        JarFile jar = new JarFile(file);
        JarEntry propsEntry = jar.getJarEntry(PROPERTIES_ENTRY);

        PackId id = null;
        if (propsEntry != null) {
            InputStream propsStream = null;
            try {
                propsStream = jar.getInputStream(propsEntry);
                Properties props = new Properties();
                props.loadFromXML(propsStream);

                id = identifyProperties(props);
            } finally {
                if (propsStream != null) {
                    propsStream.close();
                }
            }
        }

        if (id == null) {
            PackageId _id = new PackageId(PackageId.ETC_PACKAGES_PREFIX + file.getName());
            return new PackId(_id.getGroup(), _id.getName(), _id.getVersionString(), _id.getInstallationPath());
        } else {
            return id;
        }
    }

    public static PackId identifyProperties(final Properties props) {
        String group = props.getProperty(PROP_GROUP);
        String name = props.getProperty(PROP_NAME);
        String version = props.getProperty(PROP_VERSION);
        PackId id = createPackId(group, name, version);
        if (id != null) {
            return id;
        } else {
            String path = props.getProperty(PROP_PATH);
            if (path != null && path.startsWith(PackageId.ETC_PACKAGES_PREFIX)) {
                PackageId _id = new PackageId(path);
                return new PackId(_id.getGroup(), _id.getName(), _id.getVersionString(), _id.getInstallationPath());
            } else {
                return null;
            }
        }
    }

    public static PackId createPackId(final String group, final String name, final String version) {
        if (group != null && group.length() > 1 && name != null && name.length() > 1) {
            PackageId _id = new PackageId(group, name, version != null ? version : "");
            return new PackId(_id.getGroup(), _id.getName(), _id.getVersionString(), _id.getInstallationPath());
        } else {
            return null;
        }
    }
}
