package io.insideout.wordlift.org.apache.stanbol.enhancer.engines.freeling.impl;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FreelingClassLoader extends ClassLoader {

    private static FreelingClassLoader instance = null;

    private Map<String,Class<?>> classes;

    public static FreelingClassLoader getInstance() {
        if (null == instance) instance = new FreelingClassLoader();

        return instance;
    }

    public FreelingClassLoader() {
        super(FreelingClassLoader.class.getClassLoader());
        classes = new HashMap<String,Class<?>>();
    }

    public String toString() {
        return FreelingClassLoader.class.getName();
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {

        if (classes.containsKey(name)) {
            return classes.get(name);
        }

        String path = name.replace('.', File.separatorChar) + ".class";
        byte[] b = null;

        try {
            b = loadClassData(path);
        } catch (IOException e) {
            throw new ClassNotFoundException("Class not found at path: " + new File(name).getAbsolutePath(),
                    e);
        }

        Class<?> c = defineClass(name, b, 0, b.length);
        resolveClass(c);
        classes.put(name, c);

        return c;
    }

    private byte[] loadClassData(String name) throws IOException {
        File file = new File(name);
        int size = (int) file.length();
        byte buff[] = new byte[size];
        DataInputStream in = new DataInputStream(new FileInputStream(name));
        in.readFully(buff);
        in.close();

        return buff;
    }
}
