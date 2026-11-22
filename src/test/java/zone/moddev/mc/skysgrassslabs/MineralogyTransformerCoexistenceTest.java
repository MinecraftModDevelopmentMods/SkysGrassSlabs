package zone.moddev.mc.skysgrassslabs;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Uses the pinned dependency's real scripts, in both transformation orders. */
class MineralogyTransformerCoexistenceTest {
    private static final String SKY = "zone/moddev/mc/skysgrassslabs/compat/LegacyWorldDataHook";
    private static final String MINERALOGY = "zone/moddev/mc/mineralogy/patching/LegacyWorldDataHook";

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void bothOrdersKeepOneExpandedTablePair(boolean skyFirst) throws Exception {
        ClassNode node = transformed("block_state_tables", "net/minecraft/util/datafix/fixes/BlockStateData", skyFirst);
        List<Integer> sizes = new ArrayList<>();
        for (MethodNode method : node.methods) {
            if (method.desc.equals("(ILjava/lang/String;[Ljava/lang/String;)V")) {
                assertTrue((method.access & Opcodes.ACC_PUBLIC) != 0);
            }
            if (!method.name.equals("<clinit>")) continue;
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction.getOpcode() == Opcodes.ANEWARRAY
                        && ((org.objectweb.asm.tree.TypeInsnNode) instruction).desc
                        .equals("com/mojang/serialization/Dynamic")) {
                    AbstractInsnNode previous = instruction.getPrevious();
                    while (previous.getOpcode() < 0) previous = previous.getPrevious();
                    assertInstanceOf(LdcInsnNode.class, previous,
                            "An expanded auxiliary table must not look like an unexpanded primary table");
                    sizes.add(((Number) ((LdcInsnNode) previous).cst).intValue());
                }
            }
        }
        assertEquals(List.of(65536, 4096), sizes);
        assertSkyRemainsIdempotent(node, "block_state_tables");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void bothOrdersKeepEachChunkPreparationAndFinalization(boolean skyFirst) throws Exception {
        ClassNode node = transformed("chunk_data", "net/minecraft/world/level/chunk/storage/ChunkStorage", skyFirst);
        assertEquals(1, calls(node, SKY, "prepareLegacyChunk"));
        assertEquals(1, calls(node, MINERALOGY, "prepareLegacyChunk"));
        int returns = 0;
        for (MethodNode method : node.methods) {
            if (!method.desc.contains("Ljava/util/function/Supplier;Lnet/minecraft/nbt/CompoundTag;")) continue;
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction.getOpcode() == Opcodes.ARETURN) returns++;
            }
        }
        assertTrue(returns > 0);
        assertEquals(returns, calls(node, SKY, "finalizeLegacyChunk"));
        assertEquals(returns, calls(node, MINERALOGY, "finalizeLegacyChunk"));
        assertSkyRemainsIdempotent(node, "chunk_data");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void bothOrdersKeepSkysNativeSnapshotReadAndWriteHooks(boolean skyFirst) throws Exception {
        ClassNode node = transformed("level_registry", "net/minecraftforge/common/ForgeHooks", skyFirst);
        assertEquals(1, calls(node, SKY, "captureLegacyLevelData"));
        assertEquals(1, calls(node, SKY, "writeLegacyLevelData"));
        // Mineralogy also captures the registry through its server-start listener.
        // Packaged first/reload checks verify its independent persisted sidecar.
        System.out.println("MINERALOGY_NATIVE_LEVEL_READER_CALLBACKS="
                + calls(node, MINERALOGY, "captureLegacyLevelData"));
        assertSkyRemainsIdempotent(node, "level_registry");
    }

    private static ClassNode transformed(String suffix, String className, boolean skyFirst) throws Exception {
        ClassNode node = new ClassNode();
        try (InputStream input = MineralogyTransformerCoexistenceTest.class.getClassLoader()
                .getResourceAsStream(className + ".class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        String sky = Files.readString(Path.of("src/main/resources/coremods/skysgrassslabs_legacy_world.js"));
        String mineralogy;
        try (ZipFile archive = new ZipFile(System.getProperty("skysgrassslabs.mineralogyJar"))) {
            mineralogy = new String(archive.getInputStream(archive.getEntry(
                    "coremods/mineralogy_legacy_world_fix.js")).readAllBytes(), StandardCharsets.UTF_8);
        }
        if (skyFirst) {
            apply(sky, "skysgrassslabs_legacy_" + suffix, node);
            apply(mineralogy, "mineralogy_legacy_" + suffix, node);
        } else {
            apply(mineralogy, "mineralogy_legacy_" + suffix, node);
            apply(sky, "skysgrassslabs_legacy_" + suffix, node);
        }
        return node;
    }

    private static void assertSkyRemainsIdempotent(ClassNode node, String suffix) throws Exception {
        byte[] original = bytes(node);
        apply(Files.readString(Path.of("src/main/resources/coremods/skysgrassslabs_legacy_world.js")),
                "skysgrassslabs_legacy_" + suffix, node);
        assertArrayEquals(original, bytes(node));
    }

    private static int calls(ClassNode node, String owner, String name) {
        int count = 0;
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call
                        && call.owner.equals(owner) && call.name.equals(name)) count++;
            }
        }
        return count;
    }

    private static void apply(String script, String transformer, ClassNode node) throws Exception {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        assertNotNull(engine);
        engine.eval(script);
        Map<?, ?> definitions = (Map<?, ?>) ((Invocable) engine).invokeFunction("initializeCoreMod");
        assertNotNull(definitions.get(transformer));
        ((Invocable) engine).invokeMethod(definitions.get(transformer), "transformer", node);
    }

    private static byte[] bytes(ClassNode node) {
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }
}
