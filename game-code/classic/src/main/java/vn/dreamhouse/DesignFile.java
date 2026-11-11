package vn.dreamhouse;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

final class DesignFile {
    private DesignFile() {}

    static void save(Path destination, BuildPlan plan) throws IOException {
        var properties = new Properties();
        properties.setProperty("version", "1");
        properties.setProperty("width", Integer.toString(plan.width()));
        properties.setProperty("depth", Integer.toString(plan.depth()));
        properties.setProperty("wing", Boolean.toString(plan.wing()));
        properties.setProperty("pitched", Boolean.toString(plan.pitched()));
        properties.setProperty("porch", Boolean.toString(plan.porch()));
        properties.setProperty("rotated", Boolean.toString(plan.rotated()));
        properties.setProperty("leftWindow", Boolean.toString(plan.leftWindow()));
        properties.setProperty("rightWindow", Boolean.toString(plan.rightWindow()));
        properties.setProperty("palette", plan.palette().name());
        properties.setProperty("wallFinish", plan.wallFinish().name());
        saveProperties(destination, properties);
    }

    static void saveFree(Path destination, FreeBuild build) throws IOException {
        saveWorld(destination, build, new GameProgress());
    }

    static void saveWorld(Path destination, FreeBuild build, GameProgress progress) throws IOException {
        saveWorld(destination,build,progress,WorldSettings.defaults());
    }

    static void saveWorld(Path destination, FreeBuild build, GameProgress progress, WorldSettings settings) throws IOException {
        var properties = new Properties();
        properties.setProperty("version", "blocks-2");
        progress.write(properties);
        settings.write(properties);
        build.modules().forEach((cell, module) -> {
            var value = String.join(":", module.base().name(),
                    module.roof() == null ? "" : module.roof().name(),
                    module.facade() == null ? "" : module.facade().name(), module.finish().name());
            properties.setProperty(cell.column() + "," + cell.row(), value);
        });
        saveProperties(destination, properties);
    }

    static FreeBuild loadFree(Path source) throws IOException {
        return loadWorld(source).build();
    }

    record World(FreeBuild build, GameProgress progress, WorldSettings settings) {}

    static World loadWorld(Path source) throws IOException {
        var properties = loadProperties(source);
        var progress = GameProgress.read(properties);
        var settings = WorldSettings.read(properties);
        properties.keySet().removeIf(key -> key.toString().startsWith("game.") || key.toString().startsWith("world."));
        String version = properties.getProperty("version");
        if (!"blocks-1".equals(version) && !"blocks-2".equals(version)) {
            throw new IllegalArgumentException("Tệp này không phải sân xây tự do được hỗ trợ.");
        }
        properties.remove("version");
        var build = new FreeBuild();
        var details = new java.util.LinkedHashMap<FreeBuild.Cell, String[]>();
        for (var key : properties.stringPropertyNames()) {
            var coordinates = key.split(",", -1);
            var pieces = properties.getProperty(key).split(":", -1);
            if (coordinates.length != 2 || ("blocks-1".equals(version) ? pieces.length > 2 : pieces.length != 4)) {
                throw new IllegalArgumentException("Cấu kiện lưu không hợp lệ: " + key);
            }
            var cell = new FreeBuild.Cell(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1]));
            var base = FreeBuild.Piece.valueOf(pieces[0]);
            if (!base.base() || !build.place(cell, base)) {
                throw new IllegalArgumentException("Vị trí khối không hợp lệ: " + key);
            }
            details.put(cell, pieces);
        }
        for (var entry : details.entrySet()) {
            var cell = entry.getKey();
            var pieces = entry.getValue();
            if (pieces.length > 1 && !pieces[1].isEmpty()) {
                var roof = FreeBuild.Piece.valueOf(pieces[1]);
                if (!roof.roof() || !build.place(cell, roof)) throw new IllegalArgumentException("Mái không hợp lệ.");
            }
            if (pieces.length == 4) {
                var finish = FreeBuild.Piece.valueOf(pieces[3]);
                if (finish.finish == null || (finish != FreeBuild.Piece.PLASTER && !build.place(cell, finish))) {
                    throw new IllegalArgumentException("Vật liệu không hợp lệ.");
                }
                if (!pieces[2].isEmpty()) {
                    var facade = FreeBuild.Piece.valueOf(pieces[2]);
                    if (!facade.facade() || !build.place(cell, facade)) {
                        throw new IllegalArgumentException("Cửa mặt tiền không hợp lệ.");
                    }
                }
            }
        }
        if (!progress.affordable(build)) throw new IllegalArgumentException("Thiết kế vượt tài nguyên đã lưu.");
        return new World(build, progress, settings);
    }

    private static void saveProperties(Path destination, Properties properties) throws IOException {
        Path temporary = Files.createTempFile(destination.toAbsolutePath().getParent(), "dreamhouse-", ".tmp");
        try {
            try (var output = Files.newOutputStream(temporary)) {
                properties.store(output, "DreamHouse 3D");
            }
            try {
                Files.move(temporary, destination, ATOMIC_MOVE, REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, destination, REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    static BuildPlan load(Path source) throws IOException {
        var properties = loadProperties(source);
        if (!"1".equals(properties.getProperty("version"))) {
            throw new IllegalArgumentException("Phiên bản tệp thiết kế không được hỗ trợ.");
        }
        return new BuildPlan(Integer.parseInt(properties.getProperty("width")),
                Integer.parseInt(properties.getProperty("depth")), flag(properties, "wing"),
                flag(properties, "pitched"), flag(properties, "porch"), flag(properties, "rotated"),
                flag(properties, "leftWindow"), flag(properties, "rightWindow"),
                BuildPlan.RoofPalette.valueOf(properties.getProperty("palette", "")),
                BuildPlan.WallFinish.valueOf(properties.getProperty("wallFinish", "PLASTER")));
    }

    private static Properties loadProperties(Path source) throws IOException {
        var properties = new Properties();
        try (var input = Files.newInputStream(source)) {
            properties.load(input);
        }
        return properties;
    }

    private static boolean flag(Properties properties, String key) {
        return switch (properties.getProperty(key, "")) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException("Giá trị không hợp lệ: " + key);
        };
    }
}
