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
                            throw new Error("Sky's Grass Slabs found an unexpected NeoForge 21.1 "
                                    + "BlockStateData array layout");
                        }
                        if (current === vanilla) {
                            method.instructions.set(sizeInstruction, new LdcInsnNode(target));
                        }
                        ++arraysSeen;
                    }
                }
                if (!registerExposed || arraysSeen !== 2) {
                    throw new Error("Sky's Grass Slabs could not prepare NeoForge 21.1 BlockStateData");
                }
                return classNode;
            }
        },
        'skysgrassslabs_legacy_level_registry': {
            'target': {
                'type': 'CLASS',
                'name': 'net.neoforged.neoforge.common.CommonHooks'
            },
            'transformer': function(classNode) {
                var readMethods = 0;
                var writeMethods = 0;
                var owner = 'zone/moddev/mc/skysgrassslabs/compat/LegacyWorldDataHook';
                for (var index = 0; index < classNode.methods.size(); ++index) {
                    var method = classNode.methods.get(index);
                    var reading = method.name === 'readAdditionalLevelSaveData' &&
                            method.desc === '(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/level/storage/LevelStorageSource$LevelDirectory;)V';
                    var writing = method.name === 'writeAdditionalLevelSaveData' &&
                            method.desc === '(Lnet/minecraft/world/level/storage/WorldData;' +
                                    'Lnet/minecraft/nbt/CompoundTag;)V';
                    if (!reading && !writing) continue;
                    if (reading) ++readMethods;
                    else ++writeMethods;
                    var callback = reading ? 'captureLegacyLevelData' : 'writeLegacyLevelData';
                    var existingCalls = 0;
                    for (var instruction = method.instructions.getFirst(); instruction !== null;
                            instruction = instruction.getNext()) {
                        if (instruction instanceof MethodInsnNode && instruction.owner === owner &&
                                instruction.name === callback) ++existingCalls;
                    }
                    if (existingCalls > 1) {
                        throw new Error("Sky's Grass Slabs found duplicate NeoForge 21.1 level hooks");
                    }
                    if (existingCalls === 1) continue;
                    var prefix = new InsnList();
                    prefix.add(new VarInsnNode(Opcodes.ALOAD, reading ? 0 : 1));
                    if (reading) prefix.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    prefix.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner, callback,
                            reading ? '(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/level/storage/LevelStorageSource$LevelDirectory;)V' :
                                    '(Lnet/minecraft/nbt/CompoundTag;)V', false));
                    method.instructions.insert(prefix);
                }
                if (readMethods !== 1 || writeMethods !== 1) {
                    throw new Error("Sky's Grass Slabs could not find the exact NeoForge 21.1 level methods");
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
                    if (method.desc !== '(Lnet/minecraft/resources/ResourceKey;' +
                            'Ljava/util/function/Supplier;Lnet/minecraft/nbt/CompoundTag;Ljava/util/Optional;)' +
                            'Lnet/minecraft/nbt/CompoundTag;') {
                        continue;
                    }

                    var owner = 'zone/moddev/mc/skysgrassslabs/compat/LegacyWorldDataHook';
                    var preparations = 0;
                    var finalizations = 0;
                    var returns = 0;
                    for (var existing = method.instructions.getFirst(); existing !== null;
                            existing = existing.getNext()) {
                        if (existing.getOpcode() === Opcodes.ARETURN) {
                            ++returns;
                        }
                        if (existing instanceof MethodInsnNode && existing.owner === owner) {
                            if (existing.name === 'prepareLegacyChunk') ++preparations;
                            if (existing.name === 'finalizeLegacyChunk') ++finalizations;
                        }
                    }
                    if (returns === 0) {
                        throw new Error("Sky's Grass Slabs found no NeoForge 21.1 chunk return");
                    }
                    if (preparations !== 0 || finalizations !== 0) {
                        if (preparations !== 1 || finalizations !== returns) {
                            throw new Error("Sky's Grass Slabs found a partial NeoForge 21.1 chunk patch");
                        }
                        patched = true;
                        continue;
                    }

                    var prefix = new InsnList();
                    prefix.add(new VarInsnNode(Opcodes.ALOAD, 3));
                    prefix.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            owner,
                            'prepareLegacyChunk',
                            '(Lnet/minecraft/nbt/CompoundTag;)V',
                            false));
                    method.instructions.insert(prefix);

                    for (var instruction = method.instructions.getFirst(); instruction !== null;
                            instruction = instruction.getNext()) {
                        if (instruction.getOpcode() === Opcodes.ARETURN) {
                            method.instructions.insertBefore(instruction, new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    owner,
                                    'finalizeLegacyChunk',
                                    '(Lnet/minecraft/nbt/CompoundTag;)' +
                                    'Lnet/minecraft/nbt/CompoundTag;',
                                    false));
                        }
                    }
                    patched = true;
                }
                if (!patched) {
                    throw new Error("Sky's Grass Slabs could not patch NeoForge 21.1 ChunkStorage");
                }
                return classNode;
            }
        }
    };
}
