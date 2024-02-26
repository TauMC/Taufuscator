package org.taumc.fmldeobfuscator;

import cpw.mods.modlauncher.api.INameMappingService;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.commons.Remapper;

import java.util.function.BiFunction;

public class FMLRemapper extends Remapper {
    public static final FMLRemapper INSTANCE = new FMLRemapper();
    private static final BiFunction<INameMappingService.Domain, String, String> REMAPPER = FMLLoader.getNameFunction("srg").orElse((d, n) -> n);

    @Override
    public String mapFieldName(String owner, String name, String descriptor) {
        return REMAPPER.apply(INameMappingService.Domain.FIELD, name);
    }

    @Override
    public String mapMethodName(String owner, String name, String descriptor) {
        String proposedMethodName = REMAPPER.apply(INameMappingService.Domain.METHOD, name);
        if(name.startsWith("f_") && proposedMethodName.equals(name)) {
            // Probably a record field, try using the f_ name
            proposedMethodName = REMAPPER.apply(INameMappingService.Domain.FIELD, name);
        }
        return proposedMethodName;
    }

    @Override
    public String map(String internalName) {
        return REMAPPER.apply(INameMappingService.Domain.CLASS, internalName);
    }
}
