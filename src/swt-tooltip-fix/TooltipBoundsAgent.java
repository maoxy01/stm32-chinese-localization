import java.io.*;
import java.lang.instrument.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Version-pinned guard for Eclipse SWT Win32 issue 3752; leaves vendor JARs intact. */
public final class TooltipBoundsAgent implements Opcodes {
    static final String TABLE = "org/eclipse/swt/widgets/Table";
    static final String HIT = "org/eclipse/swt/internal/win32/LVHITTESTINFO";
    static final String DESC = "(Lorg/eclipse/swt/internal/win32/NMHDR;J)Lorg/eclipse/swt/internal/win32/LRESULT;";
    static final String JAR = "org.eclipse.swt.win32.win32.x86_64_3.132.0.v20251124-0642.jar";
    // Filled from the installed, unmodified vendor class by the build script.
    static final String EXPECTED = "TABLE_SHA256";

    public static void premain(String logFile, Instrumentation instrumentation) {
        log(logFile, "ARMED " + JAR);
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override public byte[] transform(ClassLoader loader, String name, Class<?> type,
                    ProtectionDomain domain, byte[] bytes) {
                if (!TABLE.equals(name)) return null;
                try {
                    if (domain == null || domain.getCodeSource() == null ||
                            !domain.getCodeSource().getLocation().getPath().endsWith("/" + JAR)) {
                        throw new IllegalArgumentException("Unexpected SWT code source");
                    }
                    byte[] patched = patch(bytes);
                    log(logFile, "APPLIED Table.positionTooltip bounds guard; vendor SHA256=" + EXPECTED);
                    return patched;
                } catch (Exception e) {
                    log(logFile, "DISABLED: " + e);
                    return null; // A software update must never receive an unverified transformation.
                }
            }
        });
    }

    static byte[] patch(byte[] bytes) throws Exception {
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        if (!EXPECTED.equals(hash)) throw new IllegalArgumentException("SWT class hash mismatch: " + hash);
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        MethodNode method = node.methods.stream().filter(m -> m.name.equals("positionTooltip") &&
                m.desc.equals(DESC)).findFirst().orElseThrow();
        List<MethodInsnNode> calls = new ArrayList<>();
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof MethodInsnNode m && m.owner.equals(TABLE) && m.name.equals("_getItem")) calls.add(m);
        }
        if (calls.size() != 1) throw new IllegalArgumentException("Unexpected hit-test structure");
        MethodInsnNode call = calls.get(0);
        AbstractInsnNode field = previousCode(call), hit = previousCode(field), self = previousCode(hit);
        if (!(field instanceof FieldInsnNode f) || !f.owner.equals(HIT) || !f.name.equals("iItem") ||
                !(hit instanceof VarInsnNode h) || h.getOpcode() != ALOAD ||
                !(self instanceof VarInsnNode s) || s.getOpcode() != ALOAD || s.var != 0 ||
                !(previousCode(self) instanceof JumpInsnNode negativeRow) || negativeRow.getOpcode() != IFLT) {
            throw new IllegalArgumentException("Unexpected hit-test instructions");
        }
        // Jump to the existing no-hit exit, BEFORE _getItem or OS.GetDC. Preserve all frames.
        LabelNode exit = negativeRow.label;
        InsnList guard = new InsnList();
        guard.add(new VarInsnNode(ALOAD, h.var));
        guard.add(new FieldInsnNode(GETFIELD, HIT, "iSubItem", "I"));
        guard.add(new JumpInsnNode(IFLT, exit));
        guard.add(new VarInsnNode(ALOAD, h.var));
        guard.add(new FieldInsnNode(GETFIELD, HIT, "iSubItem", "I"));
        guard.add(new InsnNode(ICONST_1)); // An SWT table with no explicit columns has one implicit column.
        guard.add(new VarInsnNode(ALOAD, 0));
        guard.add(new FieldInsnNode(GETFIELD, TABLE, "columnCount", "I"));
        guard.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Math", "max", "(II)I", false));
        guard.add(new JumpInsnNode(IF_ICMPGE, exit));
        method.instructions.insertBefore(self, guard);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static AbstractInsnNode previousCode(AbstractInsnNode node) {
        do { node = node.getPrevious(); } while (node != null && node.getOpcode() < 0);
        return node;
    }

    private static synchronized void log(String destination, String message) {
        String line = java.time.Instant.now() + " " + message;
        System.err.println("[XinDa SWT tooltip fix] " + line);
        if (destination != null && !destination.isBlank()) {
            try { Files.writeString(Path.of(destination), line + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND); }
            catch (IOException e) { System.err.println("Cannot write tooltip-fix log: " + e); }
        }
    }
}
