package vn.dreamhouse.glade;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.EnumMap;
import java.util.LinkedHashMap;

final class LauncherView extends JPanel {
    static final Color INK = new Color(70, 35, 23);
    private static final Color CREAM = new Color(255, 243, 219);
    private static final Color WOOD = new Color(154, 93, 58);
    private static final Color SHADOW = new Color(56, 24, 14, 105);
    private static final int WIDTH = 1024;
    private static final int HEIGHT = 576;

    private final BufferedImage art = loadArt();
    private final BufferedImage backdrop = soften(art);
    private final LinkedHashMap<Component, Rectangle> layout = new LinkedHashMap<>();
    final EnumMap<LauncherWorld, Button> worlds = new EnumMap<>(LauncherWorld.class);
    final Button settings = new Button("Cài đặt", Tone.CREAM, 17);
    final Button quit = new Button("Thoát", Tone.WOOD, 16);
    final Button openFile = new Button("Mở bản đã lưu", Tone.CREAM, 16);
    final Button play = new Button("CHƠI NGAY", Tone.GREEN, 32);
    final Button logs = new Button("Xem nhật ký", Tone.CREAM, 14);
    final Button cancel = new Button("Hủy mở", Tone.WOOD, 14);
    final JButton profileButton = new JButton();
    final JTextArea status = new JTextArea("Game đã sẵn sàng!");
    final JProgressBar progress = new JProgressBar(0, 100);
    private final boolean canRecover;
    private String playerName = GladeLauncher.DEFAULT_PLAYER_NAME;

    LauncherView(boolean canRecover) {
        this.canRecover = canRecover;
        setLayout(null);
        setBackground(new Color(105, 76, 58));
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        profileButton.setContentAreaFilled(false);
        profileButton.setBorderPainted(false);
        profileButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        profileButton.addChangeListener(event -> repaint());
        profileButton.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent event) { repaint(); }
            @Override public void focusLost(java.awt.event.FocusEvent event) { repaint(); }
        });
        setPlayerName(playerName);
        place(profileButton,17,72,284,78);
        place(settings, 882, 15, 126, 40);
        place(quit, 908, 61, 100, 39);
        place(openFile, 818, 400, 188, 41);
        world(LauncherWorld.GARDEN, 212);
        world(LauncherWorld.ISLAND, 279);
        world(LauncherWorld.NEW, 346);
        if (canRecover) world(LauncherWorld.RECOVERY, 413);
        place(play, 797, 464, 211, 99);
        place(logs, 19, 480, 165, 37);
        place(cancel, 19, 522, 165, 37);
        logs.setVisible(false);
        cancel.setVisible(false);

        progress.setValue(100);
        progress.setOpaque(false);
        progress.setBorderPainted(false);
        progress.setUI(new BasicProgressBarUI() {
            @Override protected void paintDeterminate(Graphics g, JComponent c) { paintMeter((Graphics2D) g, false, 0); }
            @Override protected void paintIndeterminate(Graphics g, JComponent c) {
                Rectangle box = getBox(null);
                paintMeter((Graphics2D) g, true, box == null ? 0 : box.x);
            }
        });
        progress.getAccessibleContext().setAccessibleName("Tiến trình khởi động game");
        place(progress, 234, 482, 536, 43);

        status.setEditable(false);
        status.setFocusable(false);
        status.setOpaque(false);
        status.setLineWrap(true);
        status.setWrapStyleWord(true);
        status.setForeground(Color.WHITE);
        status.setMargin(new Insets(0, 0, 0, 0));
        status.getAccessibleContext().setAccessibleName("Trạng thái khởi động");
        place(status, 245, 532, 515, 41);
        select(LauncherWorld.GARDEN);
    }

    void select(LauncherWorld selected) {
        openFile.getModel().setSelected(selected == LauncherWorld.FILE);
        worlds.forEach((world, button) -> {
            button.getModel().setSelected(world == selected);
            button.getAccessibleContext().setAccessibleDescription(world == selected ? "Đang chọn" : "Chọn thế giới");
            button.repaint();
        });
    }

    void setPlayerName(String name) {
        playerName=name;
        profileButton.setToolTipText(name+" · Bấm để đổi tên");
        profileButton.getAccessibleContext().setAccessibleName("Đổi tên người chơi · "+name);
        repaint();
    }

    private void world(LauncherWorld world, int y) {
        Button button = new Button(world.title, Tone.CREAM, world == LauncherWorld.RECOVERY ? 14 : 17);
        button.world = world;
        worlds.put(world, button);
        place(button, 35, y, 258, 61);
    }

    private void place(Component control, int x, int y, int width, int height) {
        layout.put(control, new Rectangle(x, y, width, height));
        add(control);
    }

    @Override public void doLayout() {
        double scale = scale();
        int left = (int) ((getWidth() - WIDTH * scale) / 2);
        int top = (int) ((getHeight() - HEIGHT * scale) / 2);
        layout.forEach((control, rect) -> {
            control.setBounds(left + (int) (rect.x * scale), top + (int) (rect.y * scale),
                    (int) (rect.width * scale), (int) (rect.height * scale));
            if (control instanceof Button button) button.setFont(font(button.textSize * scale, Font.BOLD));
        });
        status.setFont(font(17 * scale, Font.BOLD));
    }

    private double scale() { return Math.min(getWidth() / (double) WIDTH, getHeight() / (double) HEIGHT); }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = clean(graphics);
        if (backdrop != null) crop(g, backdrop, 0, 0, getWidth(), getHeight());
        g.setPaint(new GradientPaint(0, getHeight() * .58f, new Color(78, 39, 24, 0),
                0, getHeight(), new Color(42, 22, 15, 180)));
        g.fillRect(0, 0, getWidth(), getHeight());
        double scale = scale();
        g.translate((getWidth() - WIDTH * scale) / 2, (getHeight() - HEIGHT * scale) / 2);
        g.scale(scale, scale);
        logo(g);
        profile(g);
        panel(g, 17, 163, 292, canRecover ? 319 : 271, CREAM, new Color(214, 174, 131), 18);
        woodLines(g, 22, 168, 280, canRecover ? 302 : 257, new Color(158, 103, 63, 35));
        centered(g, "THẾ GIỚI CỦA BẠN", 163, 194, 21, INK);
        news(g);
        g.setColor(CREAM);
        g.setFont(font(15, Font.PLAIN));
        g.drawString("v" + GladeLauncher.VERSION, 18, 571);
        g.dispose();
    }

    private static void logo(Graphics2D g) {
        String title = "DreamHouse";
        Color[] colors = { new Color(240, 105, 77), new Color(246, 183, 81), new Color(252, 204, 97),
                new Color(155, 195, 94), new Color(98, 180, 205), new Color(141, 136, 208), new Color(222, 128, 170) };
        g.setFont(font(41, Font.BOLD));
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int x = 18;
        for (int i = 0; i < title.length(); i++) {
            GlyphVector glyph = g.getFont().createGlyphVector(g.getFontRenderContext(), title.substring(i, i + 1));
            Shape shape = glyph.getOutline(x, 46);
            g.setColor(INK);
            g.draw(shape);
            g.setPaint(new GradientPaint(0, 14, CREAM, 0, 46, colors[i * colors.length / title.length()]));
            g.fill(shape);
            x += Math.round(glyph.getGlyphMetrics(0).getAdvanceX());
        }
        g.setColor(INK);
        g.setFont(font(12, Font.BOLD));
        g.drawString("THUNG LŨNG MƠ ƯỚC", 19, 64);
    }

    private void profile(Graphics2D g) {
        panel(g, 64, 82, 237, 63, profileButton.getModel().isRollover()?Color.WHITE:CREAM, new Color(230, 202, 165), 16);
        if(profileButton.isFocusOwner()){
            g.setColor(INK);
            g.drawRoundRect(68,86,229,54,12,12);
        }
        g.setFont(font(20, Font.BOLD));
        g.setColor(INK);
        var display=playerName;
        var metrics=g.getFontMetrics();
        if(metrics.stringWidth(display)>185){
            var breaks=java.text.BreakIterator.getCharacterInstance();
            breaks.setText(playerName);
            int end=breaks.last();
            do{end=breaks.previous();}while(end>0 && metrics.stringWidth(playerName.substring(0,end)+"…")>185);
            display=playerName.substring(0,Math.max(0,end))+"…";
        }
        g.drawString(display, 94, 108);
        g.setFont(font(12, Font.PLAIN));
        g.drawString("Đổi tên người chơi", 94, 129);
        g.setColor(INK);
        g.fillOval(17, 73, 71, 77);
        g.setColor(WOOD);
        g.fillOval(19, 72, 67, 74);
        g.setColor(CREAM);
        g.fillOval(24, 78, 57, 62);
        g.setColor(new Color(187, 221, 197));
        g.fillOval(27, 81, 51, 56);
        g.setColor(new Color(116, 167, 183));
        g.fillRoundRect(35, 119, 35, 16, 12, 12);
        g.setColor(new Color(91, 52, 36));
        g.fillOval(32, 87, 41, 40);
        g.setColor(new Color(247, 188, 145));
        g.fillOval(36, 97, 34, 30);
        g.setColor(new Color(126, 74, 48));
        g.fillOval(34, 88, 32, 18);
        g.fillOval(57, 94, 14, 13);
        g.setColor(INK);
        g.fillOval(44, 108, 3, 5);
        g.fillOval(59, 108, 3, 5);
        g.drawArc(50, 113, 7, 5, 195, 150);
        g.setColor(new Color(224, 135, 113));
        g.fillOval(39, 116, 7, 4);
        g.fillOval(61, 116, 7, 4);
    }

    private void news(Graphics2D g) {
        g.setStroke(new BasicStroke(7, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(INK);
        g.draw(new Line2D.Double(783, 175, 872, 114));
        g.draw(new Line2D.Double(872, 114, 956, 174));
        g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(231, 184, 120));
        g.draw(new Line2D.Double(783, 175, 872, 114));
        g.draw(new Line2D.Double(872, 114, 956, 174));
        g.setStroke(new BasicStroke(1));
        g.setColor(CREAM);
        g.fillOval(866, 108, 12, 12);
        panel(g, 737, 169, 270, 218, new Color(212, 156, 101), WOOD, 16);
        woodLines(g, 743, 178, 259, 203, new Color(101, 51, 32, 72));
        centered(g, "BẢN TIN", 872, 202, 22, INK);
        panel(g, 753, 214, 239, 153, CREAM, new Color(245, 225, 195), 15);
        g.setColor(INK);
        g.setFont(font(18, Font.BOLD));
        g.drawString("Cập nhật v" + GladeLauncher.VERSION, 771, 241);
        g.setFont(font(16, Font.PLAIN));
        g.drawString("Thế giới Chibi tự do!", 771, 265);
        g.setColor(new Color(147, 95, 58, 85));
        g.drawLine(770, 278, 975, 278);
        g.setColor(INK);
        g.drawString("Khám phá các khối", 771, 306);
        g.drawString("xây dựng mới", 771, 329);
        thumbnail(g, 903, 285, 72, 66);
    }

    private void paintMeter(Graphics2D original, boolean moving, int x) {
        Graphics2D g = clean(original);
        int width = progress.getWidth();
        int height = progress.getHeight();
        if (width < 12 || height < 12) { g.dispose(); return; }
        panel(g, 0, 0, width - 1, height - 5, CREAM, new Color(199, 151, 107), height / 2);
        Shape track = new RoundRectangle2D.Double(5, 5, width - 11, height - 14, height / 2., height / 2.);
        g.setColor(new Color(122, 74, 46, 82));
        g.fill(track);
        Shape clip = g.getClip();
        g.clip(track);
        g.setPaint(new GradientPaint(0, 5, new Color(255, 226, 132), 0, height - 9, new Color(241, 161, 51)));
        int fill = moving ? Math.max(22, width / 4) : (width - 10) * progress.getValue() / 100;
        g.fillRoundRect(moving ? x : 5, 5, fill, height - 14, height / 2, height / 2);
        g.setClip(clip);
        String label = moving ? "Đang khởi động…" : progress.getValue() == 100 ? "Sẵn sàng" : "Chưa sẵn sàng";
        centered(g, label, width / 2, height / 2 + 3, Math.max(11, height * 2 / 5), INK);
        g.dispose();
    }

    private void thumbnail(Graphics2D g, int x, int y, int width, int height) {
        Shape old = g.getClip();
        g.clip(new RoundRectangle2D.Double(x, y, width, height, 12, 12));
        if (art != null) crop(g, art.getSubimage(art.getWidth() / 4, art.getHeight() / 10,
                art.getWidth() / 2, art.getHeight() * 3 / 4), x, y, width, height);
        else {
            g.setColor(new Color(184, 202, 136));
            g.fillRect(x, y, width, height);
            GladeIcons.paint(g, "HOUSE", x, y, Math.min(width, height));
        }
        g.setClip(old);
    }

    private static void crop(Graphics2D g, BufferedImage image, int x, int y, int width, int height) {
        double scale = Math.max(width / (double) image.getWidth(), height / (double) image.getHeight());
        int w = (int) Math.ceil(image.getWidth() * scale);
        int h = (int) Math.ceil(image.getHeight() * scale);
        g.drawImage(image, x + (width - w) / 2, y + (height - h) / 2, w, h, null);
    }

    private static void woodLines(Graphics2D g, int x, int y, int width, int height, Color color) {
        g.setColor(color);
        for (int offset = 14; offset < height - 8; offset += 26) {
            g.draw(new CubicCurve2D.Double(x, y + offset, x + width / 3., y + offset - 6,
                    x + width * .72, y + offset + 6, x + width, y + offset));
        }
    }

    private static void panel(Graphics2D g, int x, int y, int w, int h, Color top, Color bottom, int radius) {
        g.setColor(SHADOW);
        g.fillRoundRect(x, y + 4, w, h, radius, radius);
        g.setPaint(new GradientPaint(x, y, top, x, y + h, bottom));
        g.fillRoundRect(x, y, w, h, radius, radius);
        g.setColor(new Color(255, 245, 220, 135));
        g.drawRoundRect(x + 2, y + 2, w - 5, h - 5, radius, radius);
        g.setColor(new Color(83, 46, 24, 110));
        g.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
    }

    private static Font font(double size, int style) {
        return new Font("Segoe UI", style, Math.max(1, (int) Math.round(size)));
    }

    private static void centered(Graphics2D g, String text, int x, int baseline, int size, Color color) {
        g.setFont(font(size, Font.BOLD));
        g.setColor(color);
        g.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2, baseline);
    }

    private static void gear(Graphics2D g, double x, double y, double radius) {
        var cog = new Path2D.Double();
        for (int point = 0; point < 32; point++) {
            double angle = Math.PI * point / 16;
            double r = point % 4 < 2 ? radius : radius * .77;
            double px = x + Math.cos(angle) * r, py = y + Math.sin(angle) * r;
            if (point == 0) cog.moveTo(px, py);
            else cog.lineTo(px, py);
        }
        cog.closePath();
        g.setPaint(new GradientPaint(0, (float)(y - radius), new Color(245, 191, 127),
                0, (float)(y + radius), new Color(162, 94, 56)));
        g.fill(cog);
        g.setColor(INK);
        g.draw(cog);
        g.setColor(CREAM);
        g.fill(new Ellipse2D.Double(x - radius * .32, y - radius * .32, radius * .64, radius * .64));
    }

    private static Graphics2D clean(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        return g;
    }

    private static BufferedImage loadArt() {
        try (var stream = LauncherView.class.getResourceAsStream("/vn/dreamhouse/glade/launcher-island.png")) {
            return stream == null ? null : ImageIO.read(stream);
        } catch (IOException error) {
            PlayerFiles.error("Không đọc được tranh nền launcher", error);
            return null;
        }
    }

    private static BufferedImage soften(BufferedImage original) {
        if (original == null) return null;
        int width = Math.min(original.getWidth(), 810);
        int height = original.getHeight() * width / original.getWidth();
        var result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();
        return result;
    }

    private enum Tone { CREAM, WOOD, GREEN }

    final class Button extends JButton {
        final Tone tone;
        final int textSize;
        LauncherWorld world;

        Button(String text, Tone tone, int textSize) {
            super(text);
            this.tone = tone;
            this.textSize = textSize;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = clean(graphics);
            int w = getWidth();
            int h = getHeight();
            int drop = getModel().isPressed() ? 2 : 0;
            int radius = tone == Tone.GREEN ? 28 : 15;
            Color top = switch (tone) {
                case GREEN -> new Color(149, 220, 62);
                case WOOD -> new Color(184, 127, 81);
                case CREAM -> CREAM;
            };
            Color bottom = switch (tone) {
                case GREEN -> new Color(39, 155, 74);
                case WOOD -> new Color(127, 74, 49);
                case CREAM -> new Color(232, 207, 169);
            };
            if (!isEnabled()) { top = new Color(212, 197, 178); bottom = new Color(173, 153, 131); }
            else if (getModel().isRollover()) top = top.brighter();
            if (isSelected()) { top = new Color(245, 255, 205); bottom = new Color(197, 222, 139); }
            panel(g, 1, 1 + drop, w - 2, h - 6, top, bottom, radius);
            if (isFocusOwner() || isSelected()) {
                g.setColor(new Color(65, 103, 40));
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(4, 4 + drop, w - 9, h - 13, radius, radius);
            }
            int textLeft = 0;
            if (world != null) {
                int icon = h - 13;
                if (world == LauncherWorld.GARDEN || world == LauncherWorld.ISLAND) thumbnail(g, 7, 5 + drop, icon, icon);
                else {
                    g.setColor(new Color(246, 187, 73));
                    g.fillOval(7, 5 + drop, icon, icon);
                    centered(g, world == LauncherWorld.NEW ? "+" : "↶", 7 + icon / 2,
                            (h + icon / 3) / 2 + drop, Math.max(13, icon / 2), INK);
                }
                textLeft = icon + 16;
            }
            g.setFont(getFont());
            var fm = g.getFontMetrics();
            int y = (h + fm.getAscent() - fm.getDescent()) / 2 - 2 + drop;
            int x = world == null ? (w - fm.stringWidth(getText())) / 2 : textLeft;
            if (this == settings) {
                double gearRadius = h * .23;
                gear(g, x - 3, h / 2. - 2 + drop, gearRadius);
                x += (int)gearRadius;
            }
            if (tone == Tone.GREEN) {
                Shape glyph = getFont().createGlyphVector(g.getFontRenderContext(), getText()).getOutline(x, y);
                g.setColor(new Color(35, 84, 39));
                g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(glyph);
                g.setColor(CREAM);
                g.fill(glyph);
            } else {
                g.setColor(tone == Tone.WOOD ? Color.WHITE : INK);
                g.drawString(getText(), x, y);
            }
            g.dispose();
        }
    }
}
