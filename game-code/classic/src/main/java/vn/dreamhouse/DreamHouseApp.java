package vn.dreamhouse;

import javafx.application.Application;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.PickResult;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.EnumMap;

import static vn.dreamhouse.ArchitectureArt.Piece.*;

public final class DreamHouseApp extends Application {
    private static final double HOME_ZOOM = -34;
    private final Group model = new Group();
    private FreeBuild freeBuild = new FreeBuild();
    private GameProgress progress = new GameProgress();
    private LivingWorld living;
    private final CheckBox adventure = new CheckBox("Chơi với tài nguyên");
    private final Label resources = new Label();
    private final Label stockStatus = new Label();
    private final EnumMap<GameProgress.Quest, Button> rewardButtons = new EnumMap<>(GameProgress.Quest.class);
    private final SimpleBooleanProperty roofHidden = new SimpleBooleanProperty(false);
    private final ComboBox<String> photoFilter = new ComboBox<>();
    private final ComboBox<LivingWorld.Weather> weather = new ComboBox<>();
    private final CheckBox automatic = new CheckBox("Tự động ngày / đêm");
    private final ArrayDeque<FreeBuild> undoBuilds = new ArrayDeque<>();
    private final ArrayDeque<FreeBuild> redoBuilds = new ArrayDeque<>();
    private final Button undo = new Button("Hoàn tác");
    private final Button redo = new Button("Làm lại");
    private final Button finishFree = new Button("Nộp thử thách");
    private final Label freeObjectives = new Label();
    private final Group scenery = DioramaArt.garden();
    private final SimpleBooleanProperty showGrid = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty freeMode = new SimpleBooleanProperty(true);
    private final SimpleObjectProperty<FreeBuild.Piece> activePiece =
            new SimpleObjectProperty<>(FreeBuild.Piece.BLOCK);
    private final Box dropPreview = new Box(FreeBuild.TILE - 0.06, 0.06, FreeBuild.TILE - 0.06);
    private final Label freeFeedback = new Label("Kéo một thẻ xuống sân để bắt đầu.");
    private final Rotate azimuth = new Rotate(-28, Rotate.Y_AXIS);
    private final Rotate elevation = new Rotate(29, Rotate.X_AXIS);
    private final Slider width = dimensions(BuildPlan.MIN_SIZE, BuildPlan.MAX_WIDTH, 6);
    private final Slider depth = dimensions(BuildPlan.MIN_SIZE, BuildPlan.MAX_DEPTH, 5);
    private final CheckBox wing = new CheckBox("Gắn thêm cánh nhà chữ L");
    private final CheckBox pitched = new CheckBox("Dùng mái hai dốc");
    private final CheckBox rotated = new CheckBox("Đổi hướng mái chính 90°");
    private final CheckBox leftWindow = new CheckBox("Cửa sổ trái");
    private final CheckBox rightWindow = new CheckBox("Cửa sổ phải");
    private final ComboBox<BuildPlan.RoofPalette> palette = new ComboBox<>();
    private final javafx.beans.property.ObjectProperty<BuildPlan.WallFinish> wallFinish =
            new javafx.beans.property.SimpleObjectProperty<>(BuildPlan.WallFinish.PLASTER);
    private final Label status = new Label();
    private final Label budget = new Label();
    private final Label objectives = new Label();
    private final Button preview = new Button("Xem thử hiên gỗ");
    private final Button apply = new Button("Áp dụng");
    private final Button discard = new Button("Bỏ xem thử");
    private final Button removePorch = new Button("Tháo hiên");
    private final Button submit = new Button("Nộp thiết kế");
    private boolean porchBuilt;
    private boolean porchPreview;
    private boolean loadingDesign;
    private double lastMouseX;
    private double lastMouseY;

    @Override
    public void start(Stage stage) {
        if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "JavaFX không phát hiện hỗ trợ đồ họa 3D trên máy này.");
            alert.showAndWait();
            Platform.exit();
            return;
        }

        model.getTransforms().addAll(elevation, azimuth);
        var key = studioLight("#c5b393", -9, -14, -10);
        var fill = studioLight("#72878b", 13, -8, 9);
        var ambient = new AmbientLight(Color.web("#878190"));
        living = new LivingWorld(model, key, ambient);
        var view = new SubScene(new Group(model, key, fill, ambient),
                800, 620, true, SceneAntialiasing.BALANCED);
        view.setFill(Color.TRANSPARENT);
        living.hourProperty().addListener((observable,oldValue,value) -> updateSky(view,value.doubleValue()));
        updateSky(view,living.hourProperty().get());
        var camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(100);
        camera.setTranslateZ(HOME_ZOOM);
        camera.setTranslateY(0.25);
        view.setCamera(camera);
        configureBuilding(view);
        view.setOnMousePressed(event -> {
            lastMouseX = event.getSceneX();
            lastMouseY = event.getSceneY();
            view.setCursor(Cursor.CLOSED_HAND);
        });
        view.setOnMouseReleased(event -> view.setCursor(Cursor.OPEN_HAND));
        view.setCursor(Cursor.OPEN_HAND);
        view.setOnMouseDragged(event -> {
            azimuth.setAngle((azimuth.getAngle() + (event.getSceneX() - lastMouseX) * 0.6) % 360);
            elevation.setAngle(clamp(elevation.getAngle() + (event.getSceneY() - lastMouseY) * 0.35, 8, 72));
            lastMouseX = event.getSceneX();
            lastMouseY = event.getSceneY();
        });
        view.setOnScroll(event -> camera.setTranslateZ(clamp(camera.getTranslateZ() + event.getDeltaY() * 0.035,
                -42, -15)));

        var reset = new Button("Về góc ban đầu");
        roundButton(reset, GameButtonArt.Icon.HOME, "#e8794b");
        reset.setOnAction(event -> {
            azimuth.setAngle(-28);
            elevation.setAngle(29);
            camera.setTranslateZ(HOME_ZOOM);
        });
        var hint = new Label("Kéo để xoay 360° · Cuộn để thu phóng");
        hint.getStyleClass().add("viewport-hint");
        var toolToggle = chestButton("Hộp dụng cụ", false);
        toolToggle.setSelected(true);
        var designToggle = chestButton("Hộp bản thiết kế", true);
        var mode = new ToggleButton();
        mode.getStyleClass().add("game-toggle");
        mode.selectedProperty().bindBidirectional(freeMode);
        mode.textProperty().bind(Bindings.when(freeMode).then("▦  Xây tự do").otherwise("⌂  Nhà mẫu"));
        mode.setTooltip(new Tooltip("Chuyển giữa sân xây tự do và nhà mẫu"));
        var chestRow = new VBox(14, toolToggle, designToggle);
        chestRow.setAlignment(Pos.CENTER_LEFT);
        var brand = new Label("DreamHouse");
        brand.getStyleClass().add("world-brand");
        var tagline = new Label("THUNG LŨNG MƠ ƯỚC");
        tagline.getStyleClass().add("world-tagline");
        var toolbar = new VBox(7, brand, tagline, chestRow, designDrawer(designToggle));
        toolbar.setPickOnBounds(false);
        toolbar.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        var coins = new Label();
        coins.textProperty().bind(budget.textProperty());
        coins.getStyleClass().add("coin-badge");
        var place = new Label();
        place.textProperty().bind(status.textProperty());
        place.getStyleClass().add("place-badge");
        resources.getStyleClass().add("place-badge");
        var hud = new VBox(8, coins, place, resources);
        hud.setAlignment(Pos.TOP_RIGHT);
        hud.setPickOnBounds(false);
        hud.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        var zoomIn = new Button("Phóng to");
        roundButton(zoomIn, GameButtonArt.Icon.PLUS, "#e77a48");
        zoomIn.setOnAction(event -> camera.setTranslateZ(clamp(camera.getTranslateZ() + 3, -42, -15)));
        var zoomOut = new Button("Thu nhỏ");
        roundButton(zoomOut, GameButtonArt.Icon.MINUS, "#43c3d2");
        zoomOut.setOnAction(event -> camera.setTranslateZ(clamp(camera.getTranslateZ() - 3, -42, -15)));
        var toolsShortcut = new Button("Mở hộp dụng cụ");
        roundButton(toolsShortcut, GameButtonArt.Icon.HAMMER, "#e8a450");
        toolsShortcut.setOnAction(event -> toolToggle.fire());
        var designsShortcut = new Button("Mở hộp bản thiết kế");
        roundButton(designsShortcut, GameButtonArt.Icon.SCROLL, "#c18b53");
        designsShortcut.setOnAction(event -> designToggle.fire());
        var photo = new Button("Chụp ảnh");
        photo.setOnAction(event -> takePhoto(view));
        var rail = new VBox(8, reset, toolsShortcut, designsShortcut, new HBox(4, zoomIn, zoomOut), photo, mode);
        rail.setAlignment(Pos.CENTER_RIGHT);
        rail.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        var viewport = new StackPane(view, hint, toolbar, hud, rail);
        StackPane.setAlignment(rail, Pos.CENTER_RIGHT);
        StackPane.setMargin(rail, new Insets(70, 18, 0, 0));
        StackPane.setAlignment(hud, Pos.TOP_RIGHT);
        StackPane.setMargin(hud, new Insets(20));
        viewport.getStyleClass().add("game-world");
        StackPane.setAlignment(toolbar, Pos.TOP_LEFT);
        StackPane.setMargin(toolbar, new Insets(16));
        StackPane.setAlignment(hint, Pos.BOTTOM_LEFT);
        StackPane.setMargin(hint, new Insets(16));
        view.widthProperty().bind(viewport.widthProperty());
        view.heightProperty().bind(viewport.heightProperty());
        var root = new BorderPane(viewport);
        root.getStyleClass().add("game-shell");
        var sidebar = controls();
        Runnable positionTools = () -> {
            root.setLeft(null);
            root.setBottom(null);
            sidebar.setPrefWidth(freeMode.get() ? Region.USE_COMPUTED_SIZE : 340);
            sidebar.setPrefHeight(freeMode.get() ? 258 : Region.USE_COMPUTED_SIZE);
            sidebar.setMaxHeight(freeMode.get() ? 258 : Double.MAX_VALUE);
            if (toolToggle.isSelected()) {
                if (freeMode.get()) root.setBottom(sidebar);
                else root.setLeft(sidebar);
            }
        };
        positionTools.run();
        toolToggle.selectedProperty().addListener(observable -> positionTools.run());
        freeMode.addListener(observable -> {
            positionTools.run();
            rebuild();
        });
        showGrid.addListener(observable -> rebuild());
        roofHidden.addListener(observable -> rebuild());
        stage.setTitle("DreamHouse 3D");
        var scene = new Scene(root, 1360, 860);
        scene.getStylesheets().add(DreamHouseApp.class.getResource("studio.css").toExternalForm());
        stage.setScene(scene);
        stage.setMinWidth(880);
        stage.setMinHeight(560);
        rebuild();
        stage.show();
        living.start();
    }

    @Override
    public void stop() {
        if (living != null) living.stop();
    }

    private void takePhoto(SubScene view) {
        var chooser = new FileChooser();
        chooser.setTitle("Chụp ảnh thung lũng");
        chooser.setInitialFileName("thung-lung-mo-uoc.png");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Ảnh PNG", "*.png"));
        var file = chooser.showSaveDialog(view.getScene().getWindow());
        if (file == null) return;
        var image = view.snapshot(null, null);
        try {
            var output = new java.awt.image.BufferedImage((int) image.getWidth(), (int) image.getHeight(),
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
            for (int y=0;y<output.getHeight();y++) for(int x=0;x<output.getWidth();x++) {
                Color color=image.getPixelReader().getColor(x,y);
                if("Ấm áp".equals(photoFilter.getValue())) color=color.interpolate(Color.web("#ffe4a0"),0.13);
                else if("Hoài cổ".equals(photoFilter.getValue()))
                    color=color.desaturate().interpolate(Color.web("#c4a581"),0.18);
                int rgb=0xff000000 | (int)Math.round(color.getRed()*255)<<16
                        | (int)Math.round(color.getGreen()*255)<<8 | (int)Math.round(color.getBlue()*255);
                output.setRGB(x,y,rgb);
            }
            if (!javax.imageio.ImageIO.write(output, "png", file)) throw new IOException("Không có bộ ghi PNG.");
            freeFeedback.setText("Đã lưu ảnh thung lũng.");
        } catch (IOException error) {
            new Alert(Alert.AlertType.ERROR, "Không lưu được ảnh: " + error.getMessage()).showAndWait();
        }
    }

    private static ToggleButton chestButton(String label, boolean blueprints) {
        var button = new ToggleButton(label);
        button.getStyleClass().addAll("candy-button", blueprints ? "candy-gold" : "candy-green");
        var caption = new Label(blueprints ? "BẢN THIẾT KẾ" : "XÂY NHÀ  ▶");
        caption.getStyleClass().add("candy-caption");
        button.setGraphic(caption);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setTooltip(new Tooltip(label));
        return button;
    }

    private static void roundButton(Button button, GameButtonArt.Icon icon, String color) {
        button.setGraphic(GameButtonArt.round(icon, color));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.getStyleClass().add("round-game-button");
        button.setTooltip(new Tooltip(button.getText()));
    }

    private HBox designDrawer(ToggleButton toggle) {
        var save = new Button("Lưu thiết kế");
        var open = new Button("Mở thiết kế");
        save.setOnAction(event -> {
            toggle.setSelected(false);
            chooseDesignFile(true);
        });
        open.setOnAction(event -> {
            toggle.setSelected(false);
            chooseDesignFile(false);
        });
        var drawer = new HBox(8, save, open);
        drawer.getStyleClass().add("design-drawer");
        drawer.visibleProperty().bind(toggle.selectedProperty());
        drawer.managedProperty().bind(toggle.selectedProperty());
        return drawer;
    }

    private void configureBuilding(SubScene view) {
        dropPreview.setMouseTransparent(true);
        view.setOnMouseDragOver(event -> {
            if (!freeMode.get() || !(event.getGestureSource() instanceof Button card)
                    || !(card.getUserData() instanceof FreeBuild.Piece piece)) return;
            var cell = pickCell(event.getPickResult());
            var proposed = freeBuild.copy();
            showDropPreview(cell, proposed.place(cell,piece) && progress.affordable(proposed));
            event.consume();
        });
        view.setOnMouseDragExited(event -> dropPreview.setVisible(false));
        view.setOnMouseDragReleased(event -> {
            if (freeMode.get() && event.getGestureSource() instanceof Button card
                    && card.getUserData() instanceof FreeBuild.Piece piece) {
                placePiece(pickCell(event.getPickResult()), piece);
            }
            dropPreview.setVisible(false);
            event.consume();
        });
        view.setOnMouseClicked(event -> {
            if (!freeMode.get() || !event.isStillSincePress()) return;
            if(event.getButton()==MouseButton.PRIMARY) {
                for(Node node=event.getPickResult().getIntersectedNode();node!=null;node=node.getParent()) {
                    if(node.getUserData() instanceof DioramaArt.Resource resource) {
                        harvest(resource==DioramaArt.Resource.WOOD);
                        return;
                    }
                }
            }
            var cell = pickCell(event.getPickResult());
            if (cell == null) return;
            if (event.getButton() == MouseButton.SECONDARY) {
                var next = freeBuild.copy();
                if (next.remove(cell)) {
                    commitBuild(next);
                    freeFeedback.setText("Đã tháo cấu kiện trên cùng.");
                }
            } else if (event.getButton() == MouseButton.PRIMARY) {
                placePiece(cell, activePiece.get());
            }
        });
    }

    private static FreeBuild.Cell pickCell(PickResult pick) {
        for (Node node = pick.getIntersectedNode(); node != null; node = node.getParent()) {
            if (node.getUserData() instanceof FreeBuild.Cell cell) return cell;
        }
        return null;
    }

    private void placePiece(FreeBuild.Cell cell, FreeBuild.Piece piece) {
        var next = freeBuild.copy();
        if (!next.place(cell, piece)) {
            freeFeedback.setText(piece.base() ? "Chọn ô trống và chừa khoảng trống trước cửa."
                    : "Chọn khối nhà phù hợp. Cửa cần mặt tiền thoáng.");
            return;
        }
        if (!progress.affordable(next)) {
            freeFeedback.setText("Thiếu xu, gỗ hoặc đá. Thu thập tài nguyên trong tab Nhiệm vụ.");
            return;
        }
        commitBuild(next);
        living.sparkle(cell);
        freeFeedback.setText("Đã đặt " + piece.label.toLowerCase(Locale.ROOT) + ".");
    }

    private void commitBuild(FreeBuild next) {
        undoBuilds.addLast(freeBuild);
        if (undoBuilds.size() > 50) undoBuilds.removeFirst();
        redoBuilds.clear();
        freeBuild = next;
        rebuild();
    }

    private void restoreBuild(ArrayDeque<FreeBuild> from, ArrayDeque<FreeBuild> to) {
        if (from.isEmpty()) return;
        if (!progress.affordable(from.getLast())) {
            freeFeedback.setText("Cần thêm tài nguyên để khôi phục thiết kế này.");
            return;
        }
        to.addLast(freeBuild);
        freeBuild = from.removeLast();
        rebuild();
    }

    private void showDropPreview(FreeBuild.Cell cell, boolean allowed) {
        dropPreview.setVisible(cell != null);
        if (cell == null) return;
        var module = freeBuild.at(cell);
        double height = module == null || module.base() == FreeBuild.Piece.PATH ? 0
                : module.base() == FreeBuild.Piece.PORCH ? 0.19
                : FreeBuild.WALL_HEIGHT + (module.roof() == FreeBuild.Piece.GABLE ? 0.40 : 0.15);
        dropPreview.setTranslateX(cell.x());
        dropPreview.setTranslateY(-height - 0.07);
        dropPreview.setTranslateZ(cell.z());
        dropPreview.setMaterial(DioramaArt.satin(allowed ? "#a0e9b1" : "#ff8c91"));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static PointLight studioLight(String color, double x, double y, double z) {
        var light = new PointLight(Color.web(color));
        light.setTranslateX(x);
        light.setTranslateY(y);
        light.setTranslateZ(z);
        return light;
    }

    private VBox controls() {
        var title = new Label("DreamHouse 3D");
        title.getStyleClass().add("studio-title");
        pitched.setSelected(true);
        leftWindow.setSelected(true);
        rightWindow.setSelected(true);
        palette.getItems().setAll(BuildPlan.RoofPalette.values());
        palette.setValue(BuildPlan.RoofPalette.BERRY);
        palette.setAccessibleText("Màu mái");
        palette.valueProperty().addListener(observable -> rebuild());
        wallFinish.addListener(observable -> rebuild());
        rotated.disableProperty().bind(pitched.selectedProperty().not());
        width.valueProperty().addListener(observable -> rebuild());
        depth.valueProperty().addListener(observable -> rebuild());
        for (var option : new CheckBox[]{wing, pitched, rotated, leftWindow, rightWindow}) {
            option.selectedProperty().addListener(observable -> rebuild());
        }

        preview.setOnAction(event -> {
            porchPreview = true;
            rebuild();
        });
        apply.setOnAction(event -> {
            porchBuilt = true;
            porchPreview = false;
            rebuild();
        });
        discard.setOnAction(event -> {
            porchPreview = false;
            rebuild();
        });
        removePorch.setOnAction(event -> {
            porchBuilt = false;
            rebuild();
        });
        submit.setOnAction(event -> {
            var result = new Alert(Alert.AlertType.INFORMATION);
            result.initOwner(submit.getScene().getWindow());
            result.setTitle("Thử thách hoàn thành");
            result.setHeaderText("Ngôi nhà đón khách đã sẵn sàng!");
            result.setContentText("Bạn đã hoàn thành thiết kế với " + plan().cost()
                    + " xu trên ngân sách " + BuildPlan.BUDGET + " xu.");
            result.showAndWait();
        });
        status.setWrapText(true);
        budget.setWrapText(true);
        objectives.setWrapText(true);
        var subtitle = new Label("XƯỞNG THIẾT KẾ NHÀ");
        subtitle.getStyleClass().add("studio-subtitle");
        var header = new VBox(4, subtitle, title);
        header.getStyleClass().add("studio-header");
        var settings = new VBox(12, new Label("Chiều rộng"), width, new Label("Chiều sâu"), depth, rotated,
                new HBox(8, new Label("Màu mái"), palette), new HBox(8, leftWindow, rightWindow));
        var challenge = new VBox(12, new Label("Ngôi nhà đón khách"), objectives, submit);
        var tabs = new TabPane(tab("Kiến trúc", architecturePalette()), tab("Chi tiết", settings),
                tab("Thử thách", challenge));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        var summary = new VBox(4, status, budget);
        summary.getStyleClass().add("studio-summary");
        var buildTools = new TabPane(tab("Xây dựng", freeTools()), tab("Vật liệu", freeMaterials()),
                tab("Sân vườn", gardenTools()), tab("Nhiệm vụ", freeMission()), tab("Thế giới", worldTools()));
        buildTools.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        buildTools.visibleProperty().bind(freeMode);
        buildTools.managedProperty().bind(freeMode);
        tabs.visibleProperty().bind(freeMode.not());
        tabs.managedProperty().bind(freeMode.not());
        var tools = new StackPane(tabs, buildTools);
        VBox.setVgrow(tools, Priority.ALWAYS);
        var panel = new VBox(header, tools, summary);
        header.visibleProperty().bind(freeMode.not());
        header.managedProperty().bind(freeMode.not());
        summary.visibleProperty().bind(freeMode.not());
        summary.managedProperty().bind(freeMode.not());
        panel.getStyleClass().add("studio-sidebar");
        return panel;
    }

    private VBox freeTools() {
        var hint = new Label("Kéo thẻ xuống sân hoặc chọn thẻ rồi bấm một ô. Bấm phải để tháo cấu kiện.");
        hint.setWrapText(true);
        var cards = new TilePane(10, 10);
        cards.setPrefColumns(7);
        cards.setPrefTileWidth(125);
        for (var piece : FreeBuild.Piece.values()) {
            if (piece.art != null && piece.ordinal() < FreeBuild.Piece.TREE.ordinal())
                cards.getChildren().add(buildCard(piece, ArchitectureArt.thumbnail(piece.art)));
        }
        undo.setOnAction(event -> restoreBuild(undoBuilds, redoBuilds));
        redo.setOnAction(event -> restoreBuild(redoBuilds, undoBuilds));
        freeFeedback.setWrapText(true);
        freeObjectives.setWrapText(true);
        var grid = new ToggleButton("Lưới xây dựng");
        grid.selectedProperty().bindBidirectional(showGrid);
        grid.getStyleClass().add("game-toggle");
        return new VBox(8, new HBox(14, hint, undo, redo, grid), cards, freeFeedback);
    }

    private VBox freeMission() {
        finishFree.setOnAction(event -> {
            if (!progress.claim(GameProgress.Quest.HOUSE, freeBuild)) return;
            rebuild();
            var result = new Alert(Alert.AlertType.INFORMATION,
                    "Bạn đã hoàn thành ngôi nhà đón khách và nhận 180 xu.");
            result.initOwner(finishFree.getScene().getWindow());
            result.setHeaderText("Hoàn thành thử thách xây tự do!");
            result.showAndWait();
        });
        rewardButtons.put(GameProgress.Quest.HOUSE, finishFree);
        stockStatus.setWrapText(true);
        var missions = new VBox(10, adventure, stockStatus, new Label("Ngôi nhà đón khách"), freeObjectives, finishFree);
        adventure.setOnAction(event -> {
            progress.adventure = adventure.isSelected();
            if (!progress.affordable(freeBuild)) {
                progress.adventure = false;
                adventure.setSelected(false);
                freeFeedback.setText("Thiết kế đang vượt tài nguyên. Thu thập thêm trước khi bật chế độ này.");
            }
            rebuild();
        });
        for(var quest : new GameProgress.Quest[]{GameProgress.Quest.GARDEN, GameProgress.Quest.WALKWAY}) {
            var claim = new Button(quest.label + " · +" + quest.reward + " xu");
            claim.setOnAction(event -> {
                if(progress.claim(quest,freeBuild)) rebuild();
            });
            rewardButtons.put(quest,claim);
            missions.getChildren().add(claim);
        }
        var wood = new Button("Chặt gỗ +20");
        wood.setOnAction(event -> harvest(true));
        var stone = new Button("Đào đá +15");
        stone.setOnAction(event -> harvest(false));
        var rest = new Button("Nghỉ ngơi · 20 xu");
        rest.setOnAction(event -> {
            freeFeedback.setText(progress.rest(freeBuild) ? "Đã hồi 8 năng lượng." : "Năng lượng đã đầy hoặc thiếu xu.");
            rebuild();
        });
        missions.getChildren().add(new HBox(8,wood,stone,rest));
        return missions;
    }

    private void harvest(boolean wood) {
        freeFeedback.setText(progress.harvest(wood) ? "Đã thu thập " + (wood ? "20 gỗ." : "15 đá.")
                : "Hết năng lượng. Nghỉ ngơi để thu thập tiếp.");
        rebuild();
    }

    private VBox gardenTools() {
        var cards = new TilePane(10,10);
        cards.setPrefColumns(7);
        for(var piece : FreeBuild.Piece.values()) {
            if(piece.art!=null && piece.ordinal()>=FreeBuild.Piece.TREE.ordinal())
                cards.getChildren().add(buildCard(piece,ArchitectureArt.thumbnail(piece.art)));
        }
        return new VBox(8,new Label("Đặt cây, hoa, đèn và các chi tiết sân vườn vào ô đất trống."),cards);
    }

    private VBox worldTools() {
        var hour = new Slider(0,24,living.hourProperty().get());
        hour.setShowTickLabels(true);
        hour.setMajorTickUnit(6);
        hour.valueProperty().bindBidirectional(living.hourProperty());
        hour.setPrefWidth(340);
        var time = new Label();
        time.textProperty().bind(Bindings.format("Giờ trong ngày · %.1fh",living.hourProperty()));
        automatic.setOnAction(event -> living.setAutoTime(automatic.isSelected()));
        weather.getItems().setAll(LivingWorld.Weather.values());
        weather.setValue(LivingWorld.Weather.CLEAR);
        weather.valueProperty().addListener((observable,oldValue,value) -> living.setWeather(value));
        var roof = new CheckBox("Mở mái để xem mặt bằng");
        roof.selectedProperty().bindBidirectional(roofHidden);
        photoFilter.getItems().setAll("Ảnh gốc","Ấm áp","Hoài cổ");
        photoFilter.setValue("Ảnh gốc");
        return new VBox(10,new HBox(15,new VBox(5,time,hour),weather,automatic,roof),
                new HBox(10,new Label("Bộ lọc chụp ảnh"),photoFilter),
                new Label("Đèn đường sáng khi trời tối. Nhân vật đi lại trên các ô đường và hiên nối nhau."));
    }

    private static void updateSky(SubScene view,double hour) {
        double daylight=Math.max(0,Math.sin((hour-6)*Math.PI/12));
        double dusk=Math.max(0,1-Math.abs(hour-17)/3);
        Color daytime=Color.web("#c3e8e2").interpolate(Color.web("#f5d5a7"),dusk*0.9);
        view.setFill(Color.web("#344361").interpolate(daytime,Math.min(1,daylight*1.4)));
    }

    private VBox freeMaterials() {
        var hint = new Label("Chọn mẫu rồi kéo hoặc bấm vào khối nhà để đổi vật liệu của riêng khối đó.");
        hint.setWrapText(true);
        var cards = new TilePane(10, 10);
        cards.setPrefColumns(3);
        for (var piece : FreeBuild.Piece.values()) {
            if (piece.finish == null) continue;
            var image = new ImageView(ArchitectureArt.wallPattern(piece.finish, piece.finish.mainColor));
            image.setFitWidth(68);
            image.setFitHeight(43);
            cards.getChildren().add(buildCard(piece, image));
        }
        return new VBox(12, new Label("Vật liệu xây dựng"), hint, cards);
    }

    private Button buildCard(FreeBuild.Piece piece, Node thumbnail) {
        var price = new Label(piece.cost + " xu");
        price.getStyleClass().add("card-price");
        var artwork = new VBox(1, thumbnail, price);
        artwork.setAlignment(Pos.CENTER);
        var card = new Button(piece.label, artwork);
        card.setUserData(piece);
        card.setTooltip(new Tooltip(piece.label + " · " + piece.cost + " xu"));
        configureCard(card, activePiece.isEqualTo(piece));
        card.setMinWidth(116);
        card.setOnAction(event -> activePiece.set(piece));
        card.setOnDragDetected(event -> {
            activePiece.set(piece);
            card.startFullDrag();
            event.consume();
        });
        return card;
    }

    private static Tab tab(String title, VBox content) {
        content.getStyleClass().add("studio-tab-content");
        var scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return new Tab(title, scroll);
    }

    private VBox architecturePalette() {
        var block = new Button("Nhà chữ nhật", ArchitectureArt.thumbnail(HOUSE));
        var extension = new Button("Nhà chữ L", ArchitectureArt.thumbnail(WING));
        configureCard(block, wing.selectedProperty().not());
        configureCard(extension, wing.selectedProperty());
        block.setOnAction(event -> wing.setSelected(false));
        extension.setOnAction(event -> wing.setSelected(true));

        var flat = new Button("Mái bằng", ArchitectureArt.thumbnail(FLAT_ROOF));
        var gable = new Button("Mái hai dốc", ArchitectureArt.thumbnail(GABLE_ROOF));
        configureCard(flat, pitched.selectedProperty().not());
        configureCard(gable, pitched.selectedProperty());
        flat.setOnAction(event -> pitched.setSelected(false));
        gable.setOnAction(event -> pitched.setSelected(true));

        preview.setGraphic(ArchitectureArt.thumbnail(PORCH));
        configureCard(preview, discard.disableProperty().not().or(removePorch.disableProperty().not()));
        var materials = new HBox(6);
        for (var finish : BuildPlan.WallFinish.values()) {
            var sample = new ImageView(ArchitectureArt.wallPattern(finish, finish.mainColor));
            sample.setFitWidth(54);
            sample.setFitHeight(29);
            var choice = new Button(finish.label, sample);
            configureCard(choice, wallFinish.isEqualTo(finish));
            choice.setOnAction(event -> wallFinish.set(finish));
            materials.getChildren().add(choice);
        }
        var materialBox = new VBox(6, new Label("Vật liệu xây dựng"), materials);
        materialBox.getStyleClass().add("material-box");
        return new VBox(8, new Label("Khối nhà"), new HBox(8, block, extension),
                new Label("Khối mái"), new HBox(8, flat, gable), new Label("Khối hiên"),
                new HBox(8, preview, new VBox(4, apply, discard, removePorch)), materialBox);
    }

    private static void configureCard(Button button, ObservableBooleanValue selected) {
        button.setContentDisplay(ContentDisplay.TOP);
        button.getStyleClass().add("build-card");
        var chosen = javafx.css.PseudoClass.getPseudoClass("chosen");
        button.pseudoClassStateChanged(chosen, selected.get());
        selected.addListener((observable, previous, current) -> button.pseudoClassStateChanged(chosen, current));
    }

    private static Slider dimensions(int min, int max, int value) {
        var slider = new Slider(min, max, value);
        slider.setMajorTickUnit(1);
        slider.setMinorTickCount(0);
        slider.setBlockIncrement(1);
        slider.setSnapToTicks(true);
        slider.setShowTickLabels(true);
        return slider;
    }

    private BuildPlan plan() {
        return new BuildPlan((int) Math.round(width.getValue()), (int) Math.round(depth.getValue()),
                wing.isSelected(), pitched.isSelected(), porchBuilt, rotated.isSelected(),
                leftWindow.isSelected(), rightWindow.isSelected(), palette.getValue(), wallFinish.get());
    }

    private void chooseDesignFile(boolean saving) {
        var chooser = new FileChooser();
        chooser.setTitle(saving ? "Lưu thiết kế" : "Mở thiết kế");
        var blocks = new FileChooser.ExtensionFilter("Sân xây tự do (*.dreamblocks)", "*.dreamblocks");
        var preset = new FileChooser.ExtensionFilter("Nhà mẫu (*.dreamhouse)", "*.dreamhouse");
        if (saving) {
            chooser.getExtensionFilters().add(freeMode.get() ? blocks : preset);
            chooser.setInitialFileName(freeMode.get() ? "san-xay.dreamblocks" : "nha-cua-toi.dreamhouse");
        } else {
            chooser.getExtensionFilters().addAll(blocks, preset);
            chooser.setSelectedExtensionFilter(freeMode.get() ? blocks : preset);
        }
        var owner = status.getScene().getWindow();
        var selected = saving ? chooser.showSaveDialog(owner) : chooser.showOpenDialog(owner);
        if (selected == null) return;
        try {
            if (saving) {
                if (freeMode.get()) DesignFile.saveWorld(selected.toPath(), freeBuild, progress,
                        new WorldSettings(living.hourProperty().get(),weather.getValue().name(),automatic.isSelected()));
                else DesignFile.save(selected.toPath(), plan());
            } else if (selected.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".dreamblocks")) {
                var saved = DesignFile.loadWorld(selected.toPath());
                freeBuild = saved.build();
                progress = saved.progress();
                adventure.setSelected(progress.adventure);
                living.hourProperty().set(saved.settings().hour());
                weather.setValue(LivingWorld.Weather.valueOf(saved.settings().weather()));
                automatic.setSelected(saved.settings().automatic());
                living.setAutoTime(automatic.isSelected());
                undoBuilds.clear();
                redoBuilds.clear();
                freeMode.set(true);
                freeFeedback.setText("Đã mở sân xây.");
                rebuild();
            } else {
                var saved = DesignFile.load(selected.toPath());
                freeMode.set(false);
                restoreDesign(saved);
            }
        } catch (IOException | IllegalArgumentException error) {
            var alert = new Alert(Alert.AlertType.ERROR, error.getMessage());
            alert.initOwner(owner);
            alert.setHeaderText(saving ? "Không lưu được thiết kế" : "Không mở được thiết kế");
            alert.showAndWait();
        }
    }

    private void restoreDesign(BuildPlan saved) {
        loadingDesign = true;
        try {
            width.setValue(saved.width());
            depth.setValue(saved.depth());
            wing.setSelected(saved.wing());
            pitched.setSelected(saved.pitched());
            rotated.setSelected(saved.rotated());
            leftWindow.setSelected(saved.leftWindow());
            rightWindow.setSelected(saved.rightWindow());
            palette.setValue(saved.palette());
            wallFinish.set(saved.wallFinish());
            porchBuilt = saved.porch();
            porchPreview = false;
        } finally {
            loadingDesign = false;
        }
        rebuild();
    }

    private void rebuild() {
        if (loadingDesign) return;
        if (freeMode.get()) {
            renderBuildingLot();
            return;
        }
        var plan = plan();
        int w = plan.width();
        int d = plan.depth();
        model.getChildren().clear();
        model.getChildren().add(scenery);
        box(14, 0.25, 11, 0.8, 0.125, 0, "#abd1a2");
        box(13.94, 0.42, 10.94, 0.8, 0.46, 0, "#ae816a");
        box(14.12, 0.10, 11.12, 0.8, 0.68, 0, "#f1c6a2");
        model.getChildren().add(DioramaArt.contactShadow(w + 2.1, d + 2.1, 0.22, 0.28));
        if (plan.wing()) {
            model.getChildren().add(DioramaArt.contactShadow(3.7, 3.7, w / 2.0 + 1.35, -d / 2.0 + 1.45));
        }
        for (int i = 0; i < 4; i++) {
            box(0.96, 0.035, 0.34, 0, -0.018, -4.95 + i * 0.40, "#efe0ca");
        }
        model.getChildren().addAll(DioramaArt.tree(-5.05, 2.75, 1.0),
                DioramaArt.tree(-4.40, 3.90, 0.7), DioramaArt.tree(6.55, 3.25, 0.86));
        houseBlock(w, d, 2.6, 0, 0, plan.rotated(), plan.wallFinish().mainColor, plan.palette().mainColor);
        double front = -d / 2.0;
        box(1.12, 1.70, 0.08, 0, -0.85, front, "#f9debd");
        box(0.9, 1.55, 0.10, 0, -0.775, front, "#866258");
        box(0.11, 0.11, 0.08, 0.31, -0.73, front - 0.08, "#ffd88f");
        if (plan.leftWindow()) window(-w * 0.30, front);
        if (plan.rightWindow()) window(w * 0.30, front);

        if (plan.wing()) {
            double wingX = w / 2.0 + 1.25;
            houseBlock(2.5, 2.5, 2.05, wingX, front + 1.25, false,
                    plan.wallFinish().wingColor, plan.palette().wingColor);
            window(wingX, front);
        }
        if (porchPreview || porchBuilt) {
            box(2.5, 0.20, 1.2, 0, -0.10, front - 0.60, "#b48163")
                    .setOpacity(porchPreview ? 0.55 : 1);
            box(1.5, 0.10, 0.42, 0, -0.05, front - 1.40, "#9e725a")
                    .setOpacity(porchPreview ? 0.55 : 1);
        }
        String porch = porchPreview ? " · Đang xem thử hiên" : porchBuilt ? " · Đã thêm hiên" : "";
        status.setText(w + " × " + d + " ô" + porch);
        updateChallenge(plan);
    }

    private void renderBuildingLot() {
        model.getChildren().clear();
        model.getChildren().add(scenery);
        double tile = FreeBuild.TILE;
        double lotWidth = FreeBuild.COLUMNS * tile;
        double lotDepth = FreeBuild.ROWS * tile;
        box(lotWidth + 0.38, 0.11, lotDepth + 0.38, 0, 0.13, 0, "#8eac7d");
        box(lotWidth + 0.31, 0.33, lotDepth + 0.31, 0, 0.32, 0, "#b68a76");
        box(lotWidth + 0.43, 0.10, lotDepth + 0.43, 0, 0.53, 0, "#f0c9a9");
        for (int row = 0; row < FreeBuild.ROWS; row++) {
            for (int column = 0; column < FreeBuild.COLUMNS; column++) {
                var cell = new FreeBuild.Cell(column, row);
                box(tile - (showGrid.get() ? 0.018 : 0), 0.12,
                        tile - (showGrid.get() ? 0.018 : 0), cell.x(), 0.06, cell.z(),
                        showGrid.get() && (column + row) % 2 == 0 ? "#b4ce93" : "#afc98e").setUserData(cell);
            }
        }
        var displayed = freeBuild;
        var modules = displayed.modules();
        modules.forEach((cell, module) -> {
            if (module.base() == FreeBuild.Piece.BLOCK) model.getChildren().add(DioramaArt.contactShadow(
                    FreeBuild.TILE + 0.62, FreeBuild.TILE + 0.62, cell.x() + 0.08, cell.z() + 0.10));
        });
        modules.forEach((cell, module) -> renderModule(displayed, cell, module));
        dropPreview.setVisible(false);
        model.getChildren().add(dropPreview);
        living.rebuild(freeBuild);
        model.getChildren().add(living);
        status.setText("Thung lũng Mơ Ước · " + modules.size() + " ô đã xây");
        budget.setText(progress.adventure ? progress.balance(freeBuild) + " xu còn lại" : modules.size() + " ô · " + displayed.cost() + " xu");
        resources.setText(progress.adventure ? "Gỗ " + progress.woodLeft(freeBuild) + " · Đá " + progress.stoneLeft(freeBuild)
                + " · Năng lượng " + progress.energy() + "/8" : "Xây tự do · Tài nguyên không giới hạn");
        stockStatus.setText("Xu còn lại " + progress.balance(freeBuild) + " · Gỗ " + progress.woodLeft(freeBuild)
                + " · Đá " + progress.stoneLeft(freeBuild) + " · Năng lượng " + progress.energy() + "/8");
        budget.setStyle("");
        undo.setDisable(undoBuilds.isEmpty());
        redo.setDisable(redoBuilds.isEmpty());
        var goals = displayed.goals();
        freeObjectives.setText(requirement(goals.connected(), "Ít nhất 3 khối nhà ghép liền") + "\n"
                + requirement(goals.roofed(), "Mọi khối nhà đều có mái") + "\n"
                + requirement(goals.entrance(), "Cửa chính có hiên ngay trước") + "\n"
                + requirement(goals.affordable(), "Tối đa " + BuildPlan.BUDGET + " xu"));
        rewardButtons.forEach((quest,button) -> {
            button.setDisable(progress.claimed(quest) || !progress.ready(quest,freeBuild));
            if(progress.claimed(quest)) button.setText("Đã nhận · " + quest.label);
            else button.setText(quest.label + " · +" + quest.reward + " xu");
        });
    }

    private void renderModule(FreeBuild displayed, FreeBuild.Cell cell, FreeBuild.Module module) {
        int start = model.getChildren().size();
        double tile = FreeBuild.TILE;
        double height = FreeBuild.WALL_HEIGHT;
        double x = cell.x();
        double z = cell.z();
        if (module.base().ordinal() >= FreeBuild.Piece.TREE.ordinal()) {
            var decoration = GardenAssets.create(module.base());
            decoration.setTranslateX(x);
            decoration.setTranslateZ(z);
            model.getChildren().add(decoration);
        } else if (module.base() == FreeBuild.Piece.PATH) {
            for (int step = -1; step <= 1; step++) {
                box(0.96, 0.05, 0.40, x, -0.025, z + step * 0.53, "#e8ccb3");
            }
        } else if (module.base() == FreeBuild.Piece.PORCH) {
            box(tile - 0.035, 0.19, tile - 0.035, x, -0.095, z, "#a9775b");
            for (int plank = -2; plank <= 2; plank++) {
                box(tile - 0.035, 0.028, 0.325, x, -0.197, z + plank * 0.344, "#d8a77c");
            }
        } else {
            var finish = module.finish().finish;
            if(roofHidden.get()) {
                box(tile,0.08,tile,x,-0.04,z,"#c6a77c");
                for(int side:new int[]{-1,1}) {
                    var wall=box(0.09,height,tile,x+side*(tile/2-0.045),-height/2,z,finish.mainColor);
                    applyWallFinish(wall,finish,finish.mainColor);
                }
                box(tile,height,0.09,x,-height/2,z+tile/2-0.045,finish.mainColor);
                box(tile,0.30,0.09,x,-0.15,z-tile/2+0.045,finish.mainColor);
            } else {
                var shell = box(tile - 0.015, height, tile - 0.015, x, -height / 2, z, finish.mainColor);
                applyWallFinish(shell, finish, finish.mainColor);
            }
            box(tile, 0.13, tile, x, -0.08, z, "#ddc4a1");
            for (int side : new int[]{-1, 1}) {
                for (int end : new int[]{-1, 1}) {
                    box(0.10, height, 0.10, x + side * (tile / 2 - 0.045), -height / 2,
                            z + end * (tile / 2 - 0.045), "#a87849");
                }
            }
            for(int side:new int[]{-1,1}) {
                box(tile,0.09,0.08,x,-height+0.06,z+side*(tile/2-0.04),"#b48451");
                box(0.08,0.09,tile,x+side*(tile/2-0.04),-height+0.06,z,"#b48451");
            }
            var frontModule = displayed.at(cell.front());
            var facade = module.facade();
            if (facade == FreeBuild.Piece.DOOR || (facade == null && frontModule != null
                    && frontModule.base() == FreeBuild.Piece.PORCH)) {
                var frame = DioramaArt.archPanel(1.02, 1.39, 0.11, "#a16b39");
                frame.setTranslateX(x);
                frame.setTranslateY(-0.695);
                frame.setTranslateZ(z - tile / 2 - 0.02);
                var door = DioramaArt.archPanel(0.79, 1.24, 0.08, "#59978c");
                door.setTranslateX(x);
                door.setTranslateY(-0.62);
                door.setTranslateZ(z - tile / 2 - 0.10);
                model.getChildren().addAll(frame, door);
                box(0.035, 0.84, 0.025, x, -0.45, z - tile / 2 - 0.15, "#427f77");
                box(0.08, 0.08, 0.08, x + 0.25, -0.54, z - tile / 2 - 0.06, "#f2cd78");
            } else if (facade == FreeBuild.Piece.WINDOW || frontModule == null) {
                box(0.69, 0.64, 0.08, x, -0.96, z - tile / 2, "#fff1d8");
                box(0.55, 0.50, 0.06, x, -0.96, z - tile / 2 - 0.04, "#a5cfdf");
                box(0.045, 0.50, 0.04, x, -0.96, z - tile / 2 - 0.08, "#fff1d8");
                box(0.55, 0.045, 0.04, x, -0.96, z - tile / 2 - 0.08, "#fff1d8");
                box(0.20, 0.66, 0.08, x - 0.48, -0.96, z - tile / 2, "#719a85");
                box(0.20, 0.66, 0.08, x + 0.48, -0.96, z - tile / 2, "#719a85");
                box(0.83, 0.14, 0.22, x, -0.59, z - tile / 2 - 0.035, "#b87856");
                box(0.72, 0.085, 0.19, x, -0.70, z - tile / 2 - 0.035, "#8caf76");
            }
            if (module.roof() != null && !roofHidden.get()) {
                addRoof(tile - 0.26, tile - 0.26, height, x, z,
                        false, module.roof() == FreeBuild.Piece.GABLE, "#bc775a");
                if(module.roof()==FreeBuild.Piece.GABLE && module.facade()==FreeBuild.Piece.DOOR) {
                    box(0.23,0.57,0.25,x+0.45,-2.12,z+0.28,"#a99583");
                    box(0.31,0.09,0.33,x+0.45,-2.43,z+0.28,"#c7b5a0");
                }
            }
        }
        var children = model.getChildren();
        children.subList(start, children.size()).forEach(node -> node.setUserData(cell));
    }

    private void updateChallenge(BuildPlan plan) {
        int shownCost = plan.cost() + (porchPreview ? BuildPlan.PORCH_COST : 0);
        boolean overBudget = shownCost > BuildPlan.BUDGET;
        String prefix = porchPreview ? "Sau khi thêm hiên: " : "Chi phí: ";
        String warning = overBudget ? " · Vượt ngân sách " + (shownCost - BuildPlan.BUDGET) + " xu" : "";
        budget.setText(prefix + shownCost + " / " + BuildPlan.BUDGET + " xu" + warning);
        budget.setStyle(overBudget ? "-fx-text-fill: #a63643;" : "-fx-text-fill: #344f43;");
        objectives.setText(requirement(plan.wing(), "Mặt bằng chữ L") + "\n"
                + requirement(plan.pitched(), "Mái hai dốc") + "\n"
                + requirement(plan.porch(), "Hiên gỗ trước cửa"));
        preview.setDisable(porchBuilt || porchPreview);
        apply.setDisable(!porchPreview || overBudget);
        discard.setDisable(!porchPreview);
        removePorch.setDisable(!porchBuilt);
        submit.setDisable(porchPreview || !plan.meetsChallenge());
    }

    private static String requirement(boolean met, String label) {
        return (met ? "✓ " : "○ ") + label;
    }

    private void houseBlock(double w, double d, double h, double x, double z,
                            boolean turn, String wall, String roof) {
        var shell = box(w, h, d, x, -h / 2, z, wall);
        applyWallFinish(shell, wallFinish.get(), wall);
        addRoof(w, d, h, x, z, turn, pitched.isSelected(), roof);
    }

    private static void applyWallFinish(Shape3D shell, BuildPlan.WallFinish finish, String wall) {
        if (finish != BuildPlan.WallFinish.PLASTER) {
            var surface = new PhongMaterial(Color.WHITE);
            surface.setDiffuseMap(ArchitectureArt.wallPattern(finish, wall));
            shell.setMaterial(surface);
        }
    }

    private void addRoof(double w, double d, double h, double x, double z, boolean turn,
                         boolean gable, String roof) {
        if (!gable) {
            box(w + 0.25, 0.20, d + 0.25, x, -h - 0.10, z, roof);
            box(w + 0.32, 0.08, d + 0.32, x, -h, z, "#f3d8c5");
            return;
        }

        float halfSpan = (float) ((turn ? d : w) / 2 + 0.13);
        float halfRun = (float) ((turn ? w : d) / 2 + 0.13);
        float rise = halfSpan * 0.55f;
        var mesh = new TriangleMesh();
        mesh.getPoints().addAll(-halfSpan, 0, -halfRun, halfSpan, 0, -halfRun,
                0, -rise, -halfRun, -halfSpan, 0, halfRun, halfSpan, 0, halfRun, 0, -rise, halfRun);
        mesh.getTexCoords().addAll(0, 0);
        mesh.getFaces().addAll(0, 0, 1, 0, 2, 0, 3, 0, 5, 0, 4, 0,
                0, 0, 2, 0, 5, 0, 0, 0, 5, 0, 3, 0, 1, 0, 4, 0, 5, 0, 1, 0, 5, 0, 2, 0,
                0, 0, 3, 0, 4, 0, 0, 0, 4, 0, 1, 0);
        var roofShape = new MeshView(mesh);
        mesh.getFaceSmoothingGroups().addAll(0, 0, 1, 1, 2, 2, 0, 0);
        roofShape.setMaterial(DioramaArt.satin(roof));
        roofShape.setTranslateX(x);
        roofShape.setTranslateY(-h);
        roofShape.setTranslateZ(z);
        roofShape.setRotationAxis(Rotate.Y_AXIS);
        roofShape.setRotate(turn ? 90 : 0);
        box(w + 0.30, 0.08, d + 0.30, x, -h + 0.02, z, "#f4d8c5");
        model.getChildren().add(roofShape);
        var trim = new Group();
        double span = halfSpan;
        double run = halfRun;
        double slope = Math.hypot(span, rise);
        double angle = Math.toDegrees(Math.atan2(rise, span));
        int rows = Math.max(2, (int)Math.ceil(run * 2 / 0.36));
        int courses = Math.max(2, (int)Math.ceil(slope / 0.33));
        double tileDepth = run * 2 / rows;
        double tileLength = slope / courses;
        for (int row = 0; row < rows; row++) {
            for (int side : new int[]{-1, 1}) {
                for (int course = 0; course < courses; course++) {
                    double fraction = (course + 0.5) / courses;
                    var clayTile = DioramaArt.softBox(tileLength + 0.025, 0.075, tileDepth - 0.012,
                            (course + row) % 3 == 0 ? "#d38c62" : "#c4774d");
                    clayTile.setTranslateX(side * span * fraction);
                    clayTile.setTranslateY(-rise * (1 - fraction) - 0.025);
                    clayTile.setTranslateZ(-run + (row + 0.5) * tileDepth);
                    clayTile.setRotationAxis(Rotate.Z_AXIS);
                    clayTile.setRotate(side * angle);
                    trim.getChildren().add(clayTile);
                }
            }
        }
        var ridge = new Cylinder(0.055, run * 2, 12);
        ridge.setMaterial(DioramaArt.satin("#cf906c"));
        ridge.setRotationAxis(Rotate.X_AXIS);
        ridge.setRotate(90);
        ridge.setTranslateY(-rise - 0.015);
        trim.getChildren().add(ridge);
        trim.setTranslateX(x);
        trim.setTranslateY(-h);
        trim.setTranslateZ(z);
        trim.setRotationAxis(Rotate.Y_AXIS);
        trim.setRotate(turn ? 90 : 0);
        model.getChildren().add(trim);
    }

    private void window(double x, double front) {
        box(0.94, 0.92, 0.12, x, -1.35, front, "#fff3d6");
        box(0.74, 0.72, 0.07, x, -1.35, front - 0.065, "#a7d8e3");
        box(0.055, 0.72, 0.04, x, -1.35, front - 0.105, "#fff3d6");
        box(0.74, 0.055, 0.04, x, -1.35, front - 0.105, "#fff3d6");
        box(1.04, 0.10, 0.24, x, -0.89, front - 0.04, "#f3d6b6");
    }

    private Shape3D box(double w, double h, double d, double x, double y, double z, String color) {
        var shape = DioramaArt.softBox(w, h, d, color);
        shape.setTranslateX(x);
        shape.setTranslateY(y);
        shape.setTranslateZ(z);
        model.getChildren().add(shape);
        return shape;
    }

    public static void main(String[] args) {
        launch(args);
    }
}

