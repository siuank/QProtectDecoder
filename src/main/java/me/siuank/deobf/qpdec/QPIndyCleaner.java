package me.siuank.deobf.qpdec;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.slf4j.Logger;

import java.lang.invoke.*;
import java.util.Arrays;

import static org.objectweb.asm.Opcodes.*;

public class QPIndyCleaner {
    static String qpBSMDesc =
            "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";

    static Logger logger = QPDecoder.logger;
    public static void cleanInvokeDynamicCall(MethodNode method) {
        for (AbstractInsnNode node : method.instructions.toArray()) {
            if (node instanceof InvokeDynamicInsnNode) {
                InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) node;
//                logger.info("INDY FOUND");
                if ("qProtect".equals(indy.bsm.getOwner()) && qpBSMDesc.equals(indy.bsm.getDesc())) {
                    Object[] args = indy.bsmArgs;
//                    logger.info(Arrays.toString(args));
                    try {
                        flattenIndyCall(method.instructions, node, args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9]);
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                    }
                }
            }
        }
    }

    public static void flattenIndyCall(
            InsnList nodes, AbstractInsnNode ain,
            /* bsm args */Object flag, Object rawClassName, Object rawMethodName, Object rawDescriptor, Object key1, Object key2, Object key3, Object key4, Object key5, Object key6
    ) throws Exception
    {
        char[] encryptedClassName = rawClassName.toString().toCharArray();
        char[] encryptedMethodName = rawMethodName.toString().toCharArray();
        char[] encryptedDescriptor = rawDescriptor.toString().toCharArray();

        // 解密逻辑封装
        char[] decryptedClassName = _decryptChars(encryptedClassName, key1.toString().toCharArray(), key2.toString().toCharArray());
        char[] decryptedMethodName = _decryptChars(encryptedMethodName, key3.toString().toCharArray(), key4.toString().toCharArray());
        char[] decryptedDescriptor = _decryptChars(encryptedDescriptor, key5.toString().toCharArray(), key6.toString().toCharArray());

        // 类名、方法名、方法描述符还原
        String className = new String(decryptedClassName);
        String methodName = new String(decryptedMethodName);
        String descriptor = new String(decryptedDescriptor);

        // 根据标志位调用不同方法
        int callFlag = (Integer) flag;
        MethodInsnNode insnNode;
        switch(callFlag) {
            case 0: // call -> invoke static
                logger.info("static call: {}#{} !{}", className, methodName, descriptor);
                insnNode = new MethodInsnNode(INVOKESTATIC, className, methodName, descriptor, false);
                break;
            case 1: // call -> invoke virtual
                 logger.info("call: {}#{} !{}", className, methodName, descriptor);
                insnNode = new MethodInsnNode(INVOKEVIRTUAL, className, methodName, descriptor, false);
                break;
            default:
                throw new RuntimeException("Error while resolving method calls, is file tampered?");
        }
        nodes.set(ain, insnNode);
    }

    // 从indy分离出来的解密方法，原QP中并没有单独分离出该方法
    // 通过2个不同长度的key进行异或，整体key长度为两个key长度的最小公因数
    private static char[] _decryptChars(char[] encryptedChars, char[] key1, char[] key2) {
        char[] decryptedChars = new char[encryptedChars.length];
        for (int i = 0; i < encryptedChars.length; ++i) {
            decryptedChars[i] = (char) (encryptedChars[i] ^ key1[i % key1.length] ^ key2[i % key2.length]);
        }
        return decryptedChars;
    }

    static {
//        InvokeDynamicInsnNode
    }
}
