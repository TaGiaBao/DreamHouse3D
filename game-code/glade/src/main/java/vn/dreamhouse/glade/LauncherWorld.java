package vn.dreamhouse.glade;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;

enum LauncherWorld {
    MENU("Menu"),
    GARDEN("Thung lũng mơ ước"),
    ISLAND("Đảo của cá"),
    NEW("Tạo thế giới mới"),
    RECOVERY("Phục hồi phiên gần nhất"),
    FILE("Mở bản đã lưu");

    final String title;

    LauncherWorld(String title) { this.title = title; }

    String argument() { return "--launcher-world=" + name(); }

    static String fileArgument(Path file) {
        return "--launcher-file=" + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(file.toAbsolutePath().toString().getBytes(StandardCharsets.UTF_8));
    }

    static Path fileFromArgs(String[] args) {
        String prefix = "--launcher-file=";
        for (String arg : args) {
            if (arg.startsWith(prefix)) {
                try {
                    return Path.of(new String(Base64.getUrlDecoder().decode(arg.substring(prefix.length())),
                            StandardCharsets.UTF_8));
                } catch (IllegalArgumentException invalid) { return null; }
            }
        }
        return null;
    }

    static LauncherWorld fromArgs(String[] args) {
        for (var world : values()) {
            for (String arg : args) {
                if (world.argument().equals(arg)) return world;
            }
        }
        return MENU;
    }
}
