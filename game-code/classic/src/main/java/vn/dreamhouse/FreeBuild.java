package vn.dreamhouse;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;

final class FreeBuild {
    static final int COLUMNS = 7;
    static final int ROWS = 5;
    static final double TILE = 1.75;
    static final double WALL_HEIGHT = 1.65;

    enum Piece {
        BLOCK("Khối nhà", ArchitectureArt.Piece.HOUSE, 85),
        PORCH("Sàn hiên", ArchitectureArt.Piece.PORCH, 40),
        PATH("Lối đi lát đá", ArchitectureArt.Piece.PATH, 15),
        FLAT("Mái bằng", ArchitectureArt.Piece.FLAT_ROOF, 28),
        GABLE("Mái hai dốc", ArchitectureArt.Piece.GABLE_ROOF, 38),
        DOOR("Cửa chính", ArchitectureArt.Piece.DOOR, 20),
        WINDOW("Cửa sổ", ArchitectureArt.Piece.WINDOW, 15),
        TREE("Cây ăn quả", ArchitectureArt.Piece.TREE, 35),
        FLOWERS("Bồn hoa", ArchitectureArt.Piece.FLOWERS, 18),
        FENCE("Hàng rào", ArchitectureArt.Piece.FENCE, 20),
        WELL("Giếng nước", ArchitectureArt.Piece.WELL, 55),
        BENCH("Ghế sân vườn", ArchitectureArt.Piece.BENCH, 25),
        POND("Hồ nước", ArchitectureArt.Piece.POND, 45),
        LAMP("Đèn đường", ArchitectureArt.Piece.LAMP, 28),
        PLASTER(BuildPlan.WallFinish.PLASTER),
        BRICK(BuildPlan.WallFinish.BRICK),
        WOOD(BuildPlan.WallFinish.WOOD),
        STONE(BuildPlan.WallFinish.STONE);

        final String label;
        final ArchitectureArt.Piece art;
        final BuildPlan.WallFinish finish;
        final int cost;

        Piece(String label, ArchitectureArt.Piece art, int cost) {
            this.label = label;
            this.art = art;
            this.finish = null;
            this.cost = cost;
        }

        Piece(BuildPlan.WallFinish finish) {
            this.label = finish.label;
            this.art = null;
            this.finish = finish;
            this.cost = finish == BuildPlan.WallFinish.PLASTER ? 0 : 12;
        }

        boolean roof() {
            return this == FLAT || this == GABLE;
        }

        boolean facade() { return this == DOOR || this == WINDOW; }
        boolean base() { return !roof() && !facade() && finish == null; }

        int wood() {
            return switch (this) {
                case BLOCK -> 12;
                case PORCH, BENCH, FENCE -> 5;
                case DOOR, WINDOW -> 3;
                case WOOD -> 2;
                case WELL, LAMP -> 2;
                default -> 0;
            };
        }

        int stone() {
            return switch (this) {
                case BLOCK -> 5;
                case PATH, FLAT, GABLE, WELL, POND -> 3;
                case LAMP -> 1;
                case STONE -> 2;
                default -> 0;
            };
        }
    }

    record Cell(int column, int row) {
        double x() { return (column - (COLUMNS - 1) / 2.0) * TILE; }
        double z() { return (row - (ROWS - 1) / 2.0) * TILE; }
        Cell front() { return new Cell(column, row - 1); }

        boolean onLot() {
            return column >= 0 && column < COLUMNS && row >= 0 && row < ROWS;
        }
    }

    record Module(Piece base, Piece roof, Piece facade, Piece finish) {
        Module(Piece base) { this(base, null, null, Piece.PLASTER); }

        int cost() {
            return base.cost + (roof == null ? 0 : roof.cost)
                    + (facade == null ? 0 : facade.cost) + finish.cost;
        }

        int wood() {
            return base.wood() + (roof == null ? 0 : roof.wood()) + (facade == null ? 0 : facade.wood())
                    + finish.wood();
        }

        int stone() {
            return base.stone() + (roof == null ? 0 : roof.stone()) + (facade == null ? 0 : facade.stone())
                    + finish.stone();
        }
    }

    private final Map<Cell, Module> modules = new LinkedHashMap<>();

    boolean canPlace(Cell cell, Piece piece) {
        if (cell == null || !cell.onLot()) return false;
        var module = modules.get(cell);
        if (piece.base()) {
            var behind = modules.get(new Cell(cell.column(), cell.row() + 1));
            return module == null && (piece != Piece.BLOCK || behind == null || behind.facade() == null);
        }
        if (module == null || module.base() != Piece.BLOCK) return false;
        if (piece.roof()) return module.roof() != piece;
        if (piece.facade()) return exposedFront(cell) && module.facade() != piece;
        return module.finish() != piece;
    }

    boolean place(Cell cell, Piece piece) {
        if (!canPlace(cell, piece)) return false;
        var old = modules.get(cell);
        var updated = piece.base() ? new Module(piece)
                : piece.roof() ? new Module(old.base(), piece, old.facade(), old.finish())
                : piece.facade() ? new Module(old.base(), old.roof(), piece, old.finish())
                : new Module(old.base(), old.roof(), old.facade(), piece);
        modules.put(cell, updated);
        return true;
    }

    boolean remove(Cell cell) {
        var module = modules.get(cell);
        if (module == null) return false;
        if (module.roof() != null) modules.put(cell, new Module(module.base(), null, module.facade(), module.finish()));
        else if (module.facade() != null) modules.put(cell, new Module(module.base(), null, null, module.finish()));
        else modules.remove(cell);
        return true;
    }

    Module at(Cell cell) {
        return modules.get(cell);
    }

    Map<Cell, Module> modules() {
        return Map.copyOf(modules);
    }

    FreeBuild copy() {
        var copy = new FreeBuild();
        copy.modules.putAll(modules);
        return copy;
    }

    boolean exposedFront(Cell cell) {
        var front = at(cell.front());
        return front == null || front.base() != Piece.BLOCK;
    }

    int cost() {
        return modules.values().stream().mapToInt(Module::cost).sum();
    }

    int woodUsed() { return modules.values().stream().mapToInt(Module::wood).sum(); }
    int stoneUsed() { return modules.values().stream().mapToInt(Module::stone).sum(); }

    record Goals(boolean connected, boolean roofed, boolean entrance, boolean affordable) {
        boolean complete() { return connected && roofed && entrance && affordable; }
    }

    Goals goals() {
        var houses = modules.entrySet().stream().filter(entry -> entry.getValue().base() == Piece.BLOCK)
                .map(Map.Entry::getKey).toList();
        boolean connected = houses.size() >= 3 && connectedCount(houses.getFirst()) == houses.size();
        boolean roofed = !houses.isEmpty() && houses.stream().allMatch(cell -> at(cell).roof() != null);
        boolean entrance = houses.stream().anyMatch(cell -> {
            var front = at(cell.front());
            return at(cell).facade() == Piece.DOOR && front != null && front.base() == Piece.PORCH;
        });
        return new Goals(connected, roofed, entrance, cost() <= BuildPlan.BUDGET);
    }

    private int connectedCount(Cell start) {
        var seen = new HashSet<Cell>();
        var queue = new ArrayDeque<Cell>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            var cell = queue.removeFirst();
            int c = cell.column();
            int r = cell.row();
            for (var neighbor : List.of(new Cell(c - 1, r), new Cell(c + 1, r),
                    new Cell(c, r - 1), new Cell(c, r + 1))) {
                var module = at(neighbor);
                if (module != null && module.base() == Piece.BLOCK && seen.add(neighbor)) queue.add(neighbor);
            }
        }
        return seen.size();
    }
}
