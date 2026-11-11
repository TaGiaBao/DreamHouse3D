package vn.dreamhouse;

import java.util.EnumSet;
import java.util.Properties;

final class GameProgress {
    enum Quest {
        HOUSE("Ngôi nhà đón khách", 180),
        GARDEN("Trồng 5 cây ăn quả", 120),
        WALKWAY("Lát ít nhất 4 ô đường", 80);

        final String label;
        final int reward;
        Quest(String label, int reward) { this.label = label; this.reward = reward; }
    }

    private int coins = 1_000;
    private int wood = 80;
    private int stone = 50;
    private int energy = 8;
    private final EnumSet<Quest> claimed = EnumSet.noneOf(Quest.class);
    boolean adventure;

    int balance(FreeBuild build) { return coins - build.cost(); }
    int woodLeft(FreeBuild build) { return wood - build.woodUsed(); }
    int stoneLeft(FreeBuild build) { return stone - build.stoneUsed(); }
    int energy() { return energy; }

    boolean affordable(FreeBuild build) {
        return !adventure || balance(build) >= 0 && woodLeft(build) >= 0 && stoneLeft(build) >= 0;
    }

    boolean harvest(boolean lumber) {
        if (energy == 0) return false;
        if (lumber) wood += 20;
        else stone += 15;
        energy--;
        return true;
    }

    boolean rest(FreeBuild build) {
        if (energy == 8 || balance(build) < 20) return false;
        coins -= 20;
        energy = 8;
        return true;
    }

    boolean ready(Quest quest, FreeBuild build) {
        return switch (quest) {
            case HOUSE -> build.goals().complete();
            case GARDEN -> count(build, FreeBuild.Piece.TREE) >= 5;
            case WALKWAY -> count(build, FreeBuild.Piece.PATH) >= 4;
        };
    }

    boolean claimed(Quest quest) { return claimed.contains(quest); }

    boolean claim(Quest quest, FreeBuild build) {
        if (claimed.contains(quest) || !ready(quest, build)) return false;
        claimed.add(quest);
        coins += quest.reward;
        return true;
    }

    private static long count(FreeBuild build, FreeBuild.Piece piece) {
        return build.modules().values().stream().filter(module -> module.base() == piece).count();
    }

    void write(Properties properties) {
        properties.setProperty("game.coins", Integer.toString(coins));
        properties.setProperty("game.wood", Integer.toString(wood));
        properties.setProperty("game.stone", Integer.toString(stone));
        properties.setProperty("game.energy", Integer.toString(energy));
        properties.setProperty("game.adventure", Boolean.toString(adventure));
        properties.setProperty("game.claimed", claimed.stream().map(Enum::name).sorted()
                .collect(java.util.stream.Collectors.joining(",")));
    }

    static GameProgress read(Properties properties) {
        var progress = new GameProgress();
        progress.coins = amount(properties, "game.coins", 1_000, 1_000_000);
        progress.wood = amount(properties, "game.wood", 80, 1_000_000);
        progress.stone = amount(properties, "game.stone", 50, 1_000_000);
        progress.energy = amount(properties, "game.energy", 8, 8);
        String mode = properties.getProperty("game.adventure", "false");
        if (!mode.equals("true") && !mode.equals("false")) throw new IllegalArgumentException("Chế độ không hợp lệ.");
        progress.adventure = Boolean.parseBoolean(mode);
        String rewards = properties.getProperty("game.claimed", "");
        if (!rewards.isEmpty()) for (String reward : rewards.split(",")) progress.claimed.add(Quest.valueOf(reward));
        return progress;
    }

    private static int amount(Properties properties, String key, int fallback, int maximum) {
        int value = Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)));
        if (value < 0 || value > maximum) throw new IllegalArgumentException("Tài nguyên không hợp lệ.");
        return value;
    }
}
