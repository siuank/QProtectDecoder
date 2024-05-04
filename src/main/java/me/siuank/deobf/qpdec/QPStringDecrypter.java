package me.siuank.deobf.qpdec;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.slf4j.Logger;

import static org.objectweb.asm.Opcodes.*;

public class QPStringDecrypter {
    static Logger logger = QPDecoder.logger;
    public static void runDecryptLogic(MethodNode method) {
        InsnList nodes = method.instructions;
        for (AbstractInsnNode ain : nodes.toArray()) {
            if (ain.getOpcode() == INVOKESTATIC) {
                MethodInsnNode min = (MethodInsnNode) ain;
                if (min.owner.equals("qProtect") && min.name.equals("decode") && Type.getMethodDescriptor(Type.getType(String.class), Type.getType(String.class)).equals(min.desc)) {
                    AbstractInsnNode previous = ain.getPrevious();
                    if (!(previous instanceof LdcInsnNode)) {
                        logger.warn("warn: node before decode call isn't a ldc node");
                        continue;
                    }
                    LdcInsnNode ldc = (LdcInsnNode) previous;
                    if (!(ldc.cst instanceof String)) {
                        logger.warn("warn: ldc node isn't containing a string");
                        continue;
                    }
                    String raw = (String) ldc.cst;
                    String decoded = decode(raw);
//                    logger.info("decoded string: \"{}\"", decoded);
                    ldc.cst = decoded;
                    nodes.remove(min);
                }
            }
        }
    }

    public static String decode(String var0) {
        try {
            char[] var1 = var0.toCharArray();
            char[] firstStepDecrypt = new char[var1.length];
            char[] keys = new char[10];
            keys[0] = '䠲';
            keys[1] = '⎅';
            keys[2] = '⎆';
            keys[3] = '頓';
            keys[4] = '鄥';
            keys[5] = '䖂';
            keys[6] = '\u0913';
            keys[7] = '\u3422';
            keys[8] = '\u0853';
            keys[9] = '\u0724';
            char[] keys1 = keys;
            keys = new char[10];
            keys[0] = '䠠';
            keys[1] = '萃';
            keys[2] = '蝓';
            keys[3] = '㠂';
            keys[4] = '㡀';
            keys[5] = '㢔';
            keys[6] = '蜹';
            keys[7] = 'း';
            keys[8] = '茄';
            keys[9] = '㌳';
            char[] keys2 = keys;

            for (int i = 0; i < var1.length; ++i) {
                firstStepDecrypt[i] = (char)(var1[i] ^ keys1[i % 10]);
            }

            char[] finallyDecodedChars = new char[firstStepDecrypt.length];

            for (int i = 0; i < var1.length; ++i) {
                finallyDecodedChars[i] = (char)(firstStepDecrypt[i] ^ keys2[i % 10]);
            }

            return new String(finallyDecodedChars);
        } catch (Exception var7) {
            return var0;
        }
    }
}
