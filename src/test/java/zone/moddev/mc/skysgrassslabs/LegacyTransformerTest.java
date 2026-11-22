package zone.moddev.mc.skysgrassslabs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class LegacyTransformerTest {
    private static final String HOOK =
            "zone/moddev/mc/skysgrassslabs/compat/LegacyWorldDataHook";

    @Test
    void everyInjectedCallResolvesToACompiledHookMethod() throws Exception {
        ClassNode hooks = nativeClass(HOOK);
        for (String[] target : List.of(
                new String[] {"skysgrassslabs_legacy_level_registry", "net/minecraftforge/common/ForgeHooks"},
                new String[] {"skysgrassslabs_legacy_chunk_data", "net/minecraft/world/level/chunk/storage/ChunkStorage"})) {
            ClassNode node = nativeClass(target[1]);
            transform(target[0], node);
            for (MethodNode method : node.methods) {
                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof MethodInsnNode call && call.owner.equals(HOOK)) {
                        assertTrue(hooks.methods.stream().anyMatch(hook ->
                                        hook.name.equals(call.name) && hook.desc.equals(call.desc)
                                                && (hook.access & Opcodes.ACC_STATIC) != 0),
                                "Injected callback has no matching static method: " + call.name + call.desc);
                    }
                }
            }
        }
    }

    @Test
    void expandsNativeTablesOnceAndExposesRegistration() throws Exception {
        ClassNode node = nativeClass("net/minecraft/util/datafix/fixes/BlockStateData");
        transform("skysgrassslabs_legacy_block_state_tables", node);
        List<Integer> sizes = new ArrayList<>();
        boolean registrationExposed = false;
        for (MethodNode method : node.methods) {
            if (method.desc.equals("(ILjava/lang/String;[Ljava/lang/String;)V")) {
                registrationExposed = (method.access & Opcodes.ACC_PUBLIC) != 0;
            }
            if (!method.name.equals("<clinit>")) continue;
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction.getOpcode() == Opcodes.ANEWARRAY) {
                    AbstractInsnNode previous = instruction.getPrevious();
                    while (previous.getOpcode() < 0) previous = previous.getPrevious();
                    if (previous instanceof LdcInsnNode) {
                        sizes.add(((Number) ((LdcInsnNode) previous).cst).intValue());
                    }
                }
            }
        }
        assertEquals(List.of(65536, 4096), sizes);
        assertTrue(registrationExposed);
        byte[] first = bytes(node);
        transform("skysgrassslabs_legacy_block_state_tables", node);
        assertArrayEquals(first, bytes(node));
    }

    @Test
    void acceptsAndNormalizesPreviouslyExpandedAuxiliaryTable() throws Exception {
        ClassNode node = nativeClass("net/minecraft/util/datafix/fixes/BlockStateData");
        transform("skysgrassslabs_legacy_block_state_tables", node);
        for (MethodNode method : node.methods) {
            if (!method.name.equals("<clinit>")) continue;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof LdcInsnNode
                        && Integer.valueOf(4096).equals(((LdcInsnNode) instruction).cst)) {
                    method.instructions.set(instruction, new IntInsnNode(Opcodes.SIPUSH, 4096));
                }
            }
        }
        transform("skysgrassslabs_legacy_block_state_tables", node);
        byte[] normalized = bytes(node);
        transform("skysgrassslabs_legacy_block_state_tables", node);
        assertArrayEquals(normalized, bytes(node));
    }

    @Test
    void wrapsEveryNativeChunkReturnWithoutDuplicatingHooks() throws Exception {
        ClassNode node = nativeClass("net/minecraft/world/level/chunk/storage/ChunkStorage");
        transform("skysgrassslabs_legacy_chunk_data", node);
        int preparations = 0;
        int finalizations = 0;
        int returns = 0;
        for (MethodNode method : node.methods) {
            boolean patched = false;
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (!call.owner.equals(HOOK)) continue;
                    patched = true;
                    if (call.name.equals("prepareLegacyChunk")) ++preparations;
                    if (call.name.equals("finalizeLegacyChunk")) ++finalizations;
                }
            }
            if (patched) {
                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction.getOpcode() == Opcodes.ARETURN) ++returns;
                }
            }
        }
        assertEquals(1, preparations);
        assertTrue(returns > 0);
        assertEquals(returns, finalizations);
        byte[] first = bytes(node);
        transform("skysgrassslabs_legacy_chunk_data", node);
        assertArrayEquals(first, bytes(node));
    }

    @Test
    void wrapsBothNativeLevelMethodsExactlyOnce() throws Exception {
        ClassNode node = nativeClass("net/minecraftforge/common/ForgeHooks");
        transform("skysgrassslabs_legacy_level_registry", node);
        int readers = 0;
        int writers = 0;
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call && call.owner.equals(HOOK)) {
                    if (call.name.equals("captureLegacyLevelData")) readers++;
                    if (call.name.equals("writeLegacyLevelData")) writers++;
                }
            }
        }
        assertEquals(1, readers);
        assertEquals(1, writers);
        byte[] first = bytes(node);
        transform("skysgrassslabs_legacy_level_registry", node);
        assertArrayEquals(first, bytes(node));
    }

    @Test
    void refusesMissingOrDuplicatedLevelHooks() throws Exception {
        ClassNode missing = nativeClass("net/minecraftforge/common/ForgeHooks");
        missing.methods.removeIf(method -> method.name.equals("writeAdditionalLevelSaveData"));
        assertThrows(Exception.class, () -> transform("skysgrassslabs_legacy_level_registry", missing));

        ClassNode duplicated = nativeClass("net/minecraftforge/common/ForgeHooks");
        transform("skysgrassslabs_legacy_level_registry", duplicated);
        for (MethodNode method : duplicated.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof MethodInsnNode call && call.owner.equals(HOOK)) {
                    method.instructions.insertBefore(call, new MethodInsnNode(
                            call.getOpcode(), call.owner, call.name, call.desc, call.itf));
                }
            }
        }
        assertThrows(Exception.class, () -> transform("skysgrassslabs_legacy_level_registry", duplicated));
    }

    @Test
    void rejectsPartialPatchesAndUnexpectedNativeStructures() throws Exception {
        ClassNode partial = nativeClass("net/minecraft/world/level/chunk/storage/ChunkStorage");
        transform("skysgrassslabs_legacy_chunk_data", partial);
        for (MethodNode method : partial.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof MethodInsnNode
                        && ((MethodInsnNode) instruction).owner.equals(HOOK)
                        && ((MethodInsnNode) instruction).name.equals("prepareLegacyChunk")) {
                    method.instructions.remove(instruction);
                }
            }
        }
        assertThrows(Exception.class, () -> transform("skysgrassslabs_legacy_chunk_data", partial));
        assertThrows(Exception.class, () -> transform("skysgrassslabs_legacy_chunk_data", new ClassNode()));
        assertThrows(Exception.class, () -> transform("skysgrassslabs_legacy_block_state_tables", new ClassNode()));
    }

    private static void transform(String name, ClassNode node) throws Exception {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        assertNotNull(engine, "Forge's coremod script engine is missing from the test runtime");
        engine.eval(Files.readString(Path.of("src/main/resources/coremods/skysgrassslabs_legacy_world.js")));
        Map<?, ?> definitions = (Map<?, ?>) ((Invocable) engine).invokeFunction("initializeCoreMod");
        ((Invocable) engine).invokeMethod(definitions.get(name), "transformer", node);
    }

    private static ClassNode nativeClass(String name) throws Exception {
        try (InputStream input = LegacyTransformerTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull(input, "The mapped native class is missing: " + name);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }

    private static byte[] bytes(ClassNode node) {
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }
}
