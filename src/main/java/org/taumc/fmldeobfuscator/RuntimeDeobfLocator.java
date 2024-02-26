package org.taumc.fmldeobfuscator;

import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.moddiscovery.AbstractJarFileModLocator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RuntimeDeobfLocator extends AbstractJarFileModLocator {
    private static final int DISK_VERSION = 1;
    private final Path modsToDeobfFolder = FMLPaths.GAMEDIR.get().resolve("mods.deobf");
    private final Path cachedDeobfFolder = FMLPaths.GAMEDIR.get().resolve(".taufuscator_v" + DISK_VERSION);
    public static final Logger LOGGER = LogManager.getLogger("Taufuscator");

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
            Path relative = modsToDeobfFolder.relativize(path);
            Path cached = cachedDeobfFolder.resolve(relative.getFileName().toString().toLowerCase(Locale.ROOT).replaceFirst("\\.jar$", "-deobf.jar"));
            if(!Files.exists(cached)) {
                LOGGER.info("Remapping {}...", path.getFileName().toString());
                try(InputStream in = Files.newInputStream(path)) {
                    try(OutputStream out = Files.newOutputStream(cached, StandardOpenOption.CREATE)) {
                        ModRemapper.remapMod(in, out);
                    }
                } catch(IOException | RuntimeException e) {
                    LOGGER.error("Mod {} will not be remapped", path, e);
                    try { Files.deleteIfExists(cached); } catch(IOException ignored) {}
                    continue;
                }
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

    }
}
