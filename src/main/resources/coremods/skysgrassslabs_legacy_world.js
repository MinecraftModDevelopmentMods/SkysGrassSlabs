var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');

function initializeCoreMod() {
    return {
        'skysgrassslabs_legacy_world_info': {
            'target': {
                'type': 'CLASS',
                'name': 'net.minecraft.world.storage.SaveFormatOld'
            },
            'transformer': function(classNode) {
                var patched = false;
                for (var i = 0; i < classNode.methods.size(); ++i) {
                    var method = classNode.methods.get(i);
                    if (method.desc !== '(Ljava/io/File;Lcom/mojang/datafixers/DataFixer;' +
                            'Lnet/minecraft/world/storage/SaveHandler;)' +
                            'Lnet/minecraft/world/storage/WorldInfo;') continue;
                    var prefix = new InsnList();
                    prefix.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    prefix.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                            'zone/moddev/mc/skysgrassslabs/compat/LegacyWorldDataHook',
                            'prepareLegacyWorld', '(Ljava/io/File;)V', false));
                    method.instructions.insert(prefix);
                    patched = true;
                }
                if (!patched) {
                    throw new Error("Sky's Grass Slabs could not patch Forge 25 SaveFormatOld");
                }
                return classNode;
            }
        },
        'skysgrassslabs_legacy_chunk_loader': {
            'target': {
                'type': 'CLASS',
                'name': 'net.minecraft.world.chunk.storage.AnvilChunkLoader'
            },
            'transformer': function(classNode) {
                var descriptor = '(Lnet/minecraft/world/dimension/DimensionType;' +
                        'Lnet/minecraft/world/storage/WorldSavedDataStorage;II)' +
                        'Lnet/minecraft/nbt/NBTTagCompound;';
                var patchedRead = false;
                var patchedReturn = false;
                for (var i = 0; i < classNode.methods.size(); ++i) {
                    var method = classNode.methods.get(i);
                    if (method.desc !== descriptor) continue;
                    for (var instruction = method.instructions.getFirst(); instruction !== null;
                            instruction = instruction.getNext()) {
                        var previous = instruction.getPrevious();
                        if (instruction.getOpcode() === Opcodes.ASTORE && previous !== null &&
                                previous.getOpcode() === Opcodes.INVOKESTATIC &&
                                previous.owner === 'net/minecraft/nbt/CompressedStreamTools' &&
                                previous.desc.endsWith(')Lnet/minecraft/nbt/NBTTagCompound;')) {
                            var prepare = new InsnList();
                            prepare.add(new VarInsnNode(Opcodes.ALOAD, instruction.var));
                            prepare.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                    'zone/moddev/mc/skysgrassslabs/compat/LegacyWorldDataHook',
                                    'prepareLegacyChunk',
                                    '(Lnet/minecraft/nbt/NBTTagCompound;)V', false));
                            method.instructions.insert(instruction, prepare);
                            patchedRead = true;
                        } else if (instruction.getOpcode() === Opcodes.ARETURN) {
                            method.instructions.insertBefore(instruction, new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    'zone/moddev/mc/skysgrassslabs/compat/LegacyWorldDataHook',
                                    'finalizeLegacyChunk',
                                    '(Lnet/minecraft/nbt/NBTTagCompound;)' +
                                    'Lnet/minecraft/nbt/NBTTagCompound;', false));
                            patchedReturn = true;
                        }
                    }
                }
                if (!patchedRead || !patchedReturn) {
                    throw new Error("Sky's Grass Slabs could not patch Forge 25 AnvilChunkLoader");
                }
                return classNode;
            }
        }
    };
}
