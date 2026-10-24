import java.nio.file.*;
import java.nio.charset.StandardCharsets;
public class VerifyCubeMxDictionary {
    public static void main(String[] args) throws Exception {
        var type = Class.forName("com.codex.cubemx.zh.TranslationDictionary");
        var dictionary = type.getMethod("load",Path.class).invoke(null,Path.of(args[0]));
        var translate = type.getMethod("translate",String.class);
        int checked = 0;
        for (var line : Files.readAllLines(Path.of(args[0]),StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) continue;
            var pair = line.split("\t",2);
            if (!pair[1].equals(translate.invoke(dictionary,pair[0]))) throw new AssertionError(pair[0]);
            checked++;
        }
        for (var literal : new String[]{"STM32G473ZET6", "0x08000000", "HAL_GPIO_Init", "SWD"}) {
            if (!literal.equals(translate.invoke(dictionary,literal))) throw new AssertionError(literal);
        }
        System.out.println("Verified dictionary entries="+checked+"; device, address and code identifiers unchanged.");
    }
}
