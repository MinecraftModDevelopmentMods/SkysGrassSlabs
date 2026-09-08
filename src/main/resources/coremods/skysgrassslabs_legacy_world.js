var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
var IntInsnNode = Java.type('org.objectweb.asm.tree.IntInsnNode');
var LdcInsnNode = Java.type('org.objectweb.asm.tree.LdcInsnNode');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');

function previousOpcode(instruction) {
    var current = instruction.getPrevious();
    while (current !== null && current.getOpcode() < 0) {
        current = current.getPrevious();
    }
    return current;
}

function integerValue(instruction) {
    if (instruction instanceof IntInsnNode) {
        return instruction.operand;
    }
    if (instruction instanceof LdcInsnNode && typeof instruction.cst === 'number') {
        return instruction.cst;
    }
    return -1;
}

function initializeCoreMod() {
    return {
        'skysgrassslabs_legacy_block_state_tables': {
            'target': {
                'type': 'CLASS',
                'name': 'net.minecraft.util.datafix.fixes.BlockStateData'
            },
            'transformer': function(classNode) {
                var registerExposed = false;
                var arraysSeen = 0;
                for (var methodIndex = 0; methodIndex < classNode.methods.size(); ++methodIndex) {
                    var method = classNode.methods.get(methodIndex);
                    if (method.desc === '(ILjava/lang/String;[Ljava/lang/String;)V') {
                        method.access = (method.access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED))
                                | Opcodes.ACC_PUBLIC;
                        registerExposed = true;
                    }
                    if (method.name !== '<clinit>') {
                        continue;
                    }
                    for (var instruction = method.instructions.getFirst(); instruction !== null;
                            instruction = instruction.getNext()) {
                        if (instruction.getOpcode() !== Opcodes.ANEWARRAY
                                || instruction.desc !== 'com/mojang/serialization/Dynamic') {
                            continue;
                        }
                        var target = arraysSeen === 0 ? 65536 : arraysSeen === 1 ? 4096 : -1;
                        var vanilla = arraysSeen === 0 ? 4096 : arraysSeen === 1 ? 256 : -1;
                        var sizeInstruction = previousOpcode(instruction);
                        var current = integerValue(sizeInstruction);
                        if (target < 0 || current !== vanilla && current !== target) {
                            throw new Error("Sky's Grass Slabs found an unexpected Forge 52 "
                                    + "BlockStateData array layout");
                        }
                        if (current === vanilla) {
                            method.instructions.set(sizeInstruction, new LdcInsnNode(target));
                        }
                        ++arraysSeen;
                    }
                }
                if (!registerExposed || arraysSeen !== 2) {
                    throw new Error("Sky's Grass Slabs could not prepare Forge 52 BlockStateData");
                }
                return classNode;
            }
        },
        'skysgrassslabs_legacy_level_registry': {
            'target': {
                'type': 'CLASS',
                'name': 'net.minecraftforge.common.ForgeHooks'
            },
            'transformer': function(classNode) {
                var patched = false;
                for (var methodIndex = 0; methodIndex < classNode.methods.size(); ++methodIndex) {
                    var method = classNode.methods.get(methodIndex);
                    if (method.desc !== '(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;'
                            + 'Lnet/minecraft/world/level/storage/LevelStorageSource$LevelDirectory;)V') {
                        continue;
                    }
                    var prefix = new InsnList();
                    prefix.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    prefix.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    prefix.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            'zone/moddev/mc/skysgrassslabs/compat/LegacyWorldDataHook',
                            'captureLegacyLevelData',
                            '(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;'
                                    + 'Lnet/minecraft/world/level/storage/LevelStorageSource$LevelDirectory;)V',
                            false));
                    method.instructions.insert(prefix);
                    patched = true;
                }
                if (!patched) {
                    throw new Error("Sky's Grass Slabs could not patch Forge 52 level data");
                }
                return classNode;
            }
        },
        'skysgrassslabs_legacy_chunk_data': {
            'target': {
                'type': 'CLASS',
                'name': 'net.minecraft.world.level.chunk.storage.ChunkStorage'
            },
            'transformer': function(classNode) {
                var patched = false;
                for (var methodIndex = 0; methodIndex < classNode.methods.size(); ++methodIndex) {
                    var method = classNode.methods.get(methodIndex);
                    if (method.desc.indexOf('Ljava/util/function/Supplier;'
                            + 'Lnet/minecraft/nbt/CompoundTag;') < 0
                            || !method.desc.endsWith('Lnet/minecraft/nbt/CompoundTag;')) {
                        continue;
                    }
                    var prefix = new InsnList();
                    prefix.add(new VarInsnNode(Opcodes.ALOAD, 3));
                    prefix.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            'zone/moddev/mc/skysgrassslabs/compat/LegacyWorldDataHook',
                            'prepareLegacyChunk',
                            '(Lnet/minecraft/nbt/CompoundTag;)V',
                            false));
                    method.instructions.insert(prefix);
                    for (var instruction = method.instructions.getFirst(); instruction !== null;
                            instruction = instruction.getNext()) {
                        if (instruction.getOpcode() === Opcodes.ARETURN) {
                            method.instructions.insertBefore(instruction, new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    'zone/moddev/mc/skysgrassslabs/compat/LegacyWorldDataHook',
                                    'finalizeLegacyChunk',
                                    '(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;',
                                    false));
                        }
                    }
                    patched = true;
                }
                if (!patched) {
                    throw new Error("Sky's Grass Slabs could not patch Forge 52 ChunkStorage");
                }
                return classNode;
            }
        }
    };
}
