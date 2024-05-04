import java.lang.invoke.*;

public class qProtect {
    // invoke dynamic调用方法
    public static CallSite flattenIndyCall(
            /*indy info*/ MethodHandles.Lookup lookup, String calledMethodName, MethodType methodType,
            /* bsm args */Integer flag, String rawClassName, String rawMethodName, String rawDescriptor, String key1, String key2, String key3, String key4, String key5, String key6
    ) throws Exception
    {
        char[] encryptedClassName = rawClassName.toCharArray();
        char[] encryptedMethodName = rawMethodName.toCharArray();
        char[] encryptedDescriptor = rawDescriptor.toCharArray();

        // 解密逻辑封装
        char[] decryptedClassName = _decryptChars(encryptedClassName, key1.toCharArray(), key2.toCharArray());
        char[] decryptedMethodName = _decryptChars(encryptedMethodName, key3.toCharArray(), key4.toCharArray());
        char[] decryptedDescriptor = _decryptChars(encryptedDescriptor, key5.toCharArray(), key6.toCharArray());

        // 类名、方法名、方法描述符还原
        String className = new String(decryptedClassName);
        String methodName = new String(decryptedMethodName);
        String descriptor = new String(decryptedDescriptor);

        // 根据标志位调用不同方法
        int callFlag = flag;
        MethodHandle methodHandle;
        switch(callFlag) {
            case 0: // call -> invoke static
                methodHandle = lookup.findStatic(Class.forName(className), methodName, MethodType.fromMethodDescriptorString(descriptor, qProtect.class.getClassLoader()));
                break;
            case 1: // call -> invoke virtual
                methodHandle = lookup.findVirtual(Class.forName(className), methodName, MethodType.fromMethodDescriptorString(descriptor, qProtect.class.getClassLoader()));
                break;
            default:
                throw new RuntimeException("Error while resolving method calls, is file tampered?");
        }

        methodHandle = methodHandle.asType(methodType);
        return new ConstantCallSite(methodHandle);
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

    // 字符串解密方法，key可能有变换，该项目暂时不处理这玩意
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
            keys[6] = 'ओ';
            keys[7] = '㐢';
            keys[8] = 'ࡓ';
            keys[9] = 'ܤ';
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
