package org.taumc.fmldeobfuscator;

import com.google.common.hash.Hashing;
import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import cpw.mods.modlauncher.api.NamedPath;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.ModDirTransformerDiscoverer;
import net.minecraftforge.fml.loading.moddiscovery.AbstractJarFileModLocator;
import net.minecraftforge.fml.loading.moddiscovery.ModsFolderLocator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RuntimeDeobfLocator extends AbstractJarFileModLocator {
    private static final int DISK_VERSION = 1;
    private final Path modsToDeobfFolder = FMLPaths.GAMEDIR.get().resolve("mods");
    private final Path cachedDeobfFolder = FMLPaths.GAMEDIR.get().resolve(".taufuscator_v" + DISK_VERSION);
    public static final Logger LOGGER = LogManager.getLogger("Taufuscator");

    private static String computeHash(Path path) throws IOException {
        String hash = com.google.common.io.Files.asByteSource(path.toFile()).hash(Hashing.sha256()).toString();
        return hash.substring(0, Math.min(hash.length(), 32));
    }

    @Override
    public Stream<Path> scanCandidates() {
        if(FMLLoader.isProduction()) {
            LOGGER.warn("Taufuscator is useless in production, doing nothing.");
            return Stream.empty();
        }
        if(!Files.exists(modsToDeobfFolder)) {
            LamdbaExceptionUtils.uncheck(() -> Files.createDirectory(modsToDeobfFolder));
        }
        if(!Files.exists(cachedDeobfFolder)) {
            LamdbaExceptionUtils.uncheck(() -> Files.createDirectory(cachedDeobfFolder));
        }
        List<Path> pathList = LamdbaExceptionUtils.uncheck(() -> {
            try(Stream<Path> stream = Files.list(this.modsToDeobfFolder)) {
                return stream.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                        .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
                        .collect(Collectors.toList());
            }
        });
        List<Path> newMods = new ArrayList<>();
        for(Path path : pathList) {
            Path cached = null;
            try {
                String hash = computeHash(path);
                cached = cachedDeobfFolder.resolve(hash + ".jar");
                if(!Files.exists(cached)) {
                    LOGGER.info("Remapping {}...", path.getFileName().toString());
                    try(InputStream in = Files.newInputStream(path)) {
                        try(OutputStream out = Files.newOutputStream(cached, StandardOpenOption.CREATE)) {
                            ModRemapper.remapMod(in, out);
                        }
                    }
                }
            } catch(IOException | RuntimeException e) {
                LOGGER.error("Mod {} will not be remapped", path, e);
                if(cached != null) {
                    try { Files.deleteIfExists(cached); } catch(IOException ignored) {}
                }
                continue;
            }

            newMods.add(cached);
        }
        LOGGER.info("Found " + newMods.size() + " deobfuscated mods");
        return newMods.stream();
    }

    @Override
    public String name() {
        return "taufuscator";
    }

    @Override
    public void initArguments(Map<String, ?> arguments) {
        LOGGER.info("Preventing discovery of any mods in the mods folder");
        List<NamedPath> modList;
        try(Stream<Path> fileList = Files.list(modsToDeobfFolder)) {
            modList = fileList.map(path -> new NamedPath(path.getFileName().toString(), path)).collect(Collectors.toList());
        } catch(IOException e) {
            LOGGER.warn("Failed to read files from mod directory", e);
            return;
        }
        try {
            Field field = ModDirTransformerDiscoverer.class.getDeclaredField("found");
            field.setAccessible(true);
            ((List<NamedPath>)field.get(null)).addAll(modList);
        } catch(ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
