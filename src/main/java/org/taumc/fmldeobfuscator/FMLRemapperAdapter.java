package org.taumc.fmldeobfuscator;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.MethodRemapper;
import org.objectweb.asm.commons.Remapper;

import java.util.Arrays;
import java.util.List;

public class FMLRemapperAdapter extends ClassRemapper {
    public FMLRemapperAdapter(ClassVisitor classVisitor) {
        super(classVisitor, FMLRemapper.INSTANCE);
    }

    // The metafactory fixing system is borrowed from 1.12's FMLRemappingAdapter under LGPL-2.1

    private static final List<Handle> META_FACTORIES = Arrays.asList(
            new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory",
                    "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
                    false),
            new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "altMetafactory",
                    "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;",
                    false)
    );

    @Override
    protected MethodVisitor createMethodRemapper(MethodVisitor mv)
    {
        return new StaticFixingMethodVisitor(mv, remapper);
    }

    private static class StaticFixingMethodVisitor extends MethodRemapper
    {

        public StaticFixingMethodVisitor(MethodVisitor mv, Remapper remapper)
        {
            super(mv, remapper);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs)
        {
            // Special case lambda metaFactory to get new name
            if (META_FACTORIES.contains(bsm))
            {
                String owner = Type.getReturnType(desc).getInternalName();
                String odesc = ((Type) bsmArgs[0]).getDescriptor(); // First constant argument is "samMethodType - Signature and return type of method to be implemented by the function object."
                name = remapper.mapMethodName(owner, name, odesc);
            }

            super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        }
    }
}
