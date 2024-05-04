package me.siuank.deobf.qpdec;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.slf4j.Logger;

import java.util.LinkedList;

import static org.objectweb.asm.Opcodes.*;

public class QPFlowRemover {
    static Logger logger = QPDecoder.logger;
    public static void removeFlow(ClassNode node) {
        LinkedList<String> badFields = new LinkedList<>();
        for (FieldNode field : node.fields) {
            if ((field.access & ACC_STATIC) == 0 && (field.access & ACC_FINAL) == 0 && "Z".equals(field.desc)) {
                badFields.add(field.name);
            }
        }

        for (MethodNode method : node.methods) {
            removeFlow(node.name, method);
        }
    }
    public static void removeFlow(String clzName, MethodNode method) {
        InsnList nodes = method.instructions;
        for (AbstractInsnNode node : nodes.toArray()) {
            if (node instanceof JumpInsnNode && node.getOpcode() == IFEQ) {
                AbstractInsnNode previous = node.getPrevious();
                if (!(previous instanceof FieldInsnNode)) continue;
                FieldInsnNode fin = (FieldInsnNode) previous;
                if (fin.getOpcode() != GETSTATIC || !"Z".equals(fin.desc) || !clzName.contains(fin.owner)) continue;
                String debugInfo = String.format("Flow JMP replaced { field: %s.%s # %s }", fin.owner, fin.name, fin.desc);
                JumpInsnNode jump = (JumpInsnNode) node;
                logger.info(debugInfo);
                nodes.set(fin, new LdcInsnNode(debugInfo));

                InsnList replacement = new InsnList();
                replacement.add(new InsnNode(POP));
                replacement.add(new JumpInsnNode(GOTO, jump.label));
                nodes.insert(node, replacement);
                nodes.remove(node);
            }
        }
    }
}
