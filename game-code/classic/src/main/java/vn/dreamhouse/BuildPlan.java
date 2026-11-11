package vn.dreamhouse;

record BuildPlan(int width, int depth, boolean wing, boolean pitched, boolean porch,
                 boolean rotated, boolean leftWindow, boolean rightWindow, RoofPalette palette,
                 WallFinish wallFinish) {
    static final int MIN_SIZE = 4;
    static final int MAX_WIDTH = 8;
    static final int MAX_DEPTH = 7;
    static final int BUDGET = 1_000;
    static final int PORCH_COST = 120;

    BuildPlan {
        if (width < MIN_SIZE || width > MAX_WIDTH || depth < MIN_SIZE || depth > MAX_DEPTH) {
            throw new IllegalArgumentException("Kích thước nhà nằm ngoài giới hạn.");
        }
    }

    int cost() {
        return 300 + width * depth * 9 + (wing ? 145 : 0)
                + (pitched ? 95 : 0) + (porch ? PORCH_COST : 0);
    }

    boolean meetsChallenge() {
        return wing && pitched && porch && cost() <= BUDGET;
    }

    enum WallFinish {
        PLASTER("Tường sơn", "#efd9ad", "#e6c58f"),
        BRICK("Gạch đỏ", "#bd715d", "#d38e75"),
        WOOD("Gỗ sáng", "#d3a676", "#e6c197"),
        STONE("Đá tự nhiên", "#b7b0a2", "#cbc2af");

        final String label;
        final String mainColor;
        final String wingColor;

        WallFinish(String label, String mainColor, String wingColor) {
            this.label = label;
            this.mainColor = mainColor;
            this.wingColor = wingColor;
        }
    }

    enum RoofPalette {
        BERRY("Hồng mận", "#9a5574", "#b06b80"),
        TERRACOTTA("Đỏ đất nung", "#a94f39", "#c16b51"),
        SAGE("Xanh lá xô thơm", "#4f7666", "#719584");

        final String label;
        final String mainColor;
        final String wingColor;

        RoofPalette(String label, String mainColor, String wingColor) {
            this.label = label;
            this.mainColor = mainColor;
            this.wingColor = wingColor;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
