package vn.dreamhouse.glade;

import java.io.IOException;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.collision.CollisionResults;
import com.jme3.input.*;
import com.jme3.input.controls.*;
import com.jme3.light.*;
import com.jme3.math.*;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.*;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.shape.Torus;
import com.jme3.shadow.*;
import com.jme3.system.AppSettings;

import javax.swing.*;
import java.nio.file.*;
import java.util.*;

public final class GladeApp extends SimpleApplication {
    private enum Tool {
        SELECT("Chỉnh / Di chuyển",null), PAINT("Vật liệu & màu",null), TERRAIN("Địa hình",null), HOUSE("Vẽ nhà",GladeDesign.Kind.HOUSE),
        MANOR("Nhà tầng",GladeDesign.Kind.MANOR), TOWER("Tháp tròn",GladeDesign.Kind.TOWER),
        CANAL_HOUSE("Nhà ven kênh",GladeDesign.Kind.CANAL_HOUSE), BRICK_HOUSE("Nhà phố gạch",GladeDesign.Kind.BRICK_HOUSE),
        PAVILION("Chòi gỗ",GladeDesign.Kind.PAVILION), BRIDGE("Cầu gỗ",GladeDesign.Kind.BRIDGE),
        WALL("Vẽ tường",GladeDesign.Kind.WALL), PATH("Vẽ lối đi",GladeDesign.Kind.PATH),
        FENCE("Hàng rào",GladeDesign.Kind.FENCE),
        TREE("Trồng cây",GladeDesign.Kind.TREE), FLOWERS("Trồng hoa",GladeDesign.Kind.FLOWERS),
        WATER("Vẽ / đổ nước",GladeDesign.Kind.WATER), LAND("Kéo mép đất",null), ERASE("Xóa",null),
        LOG_CABIN("Nhà gỗ",GladeDesign.Kind.LOG_CABIN), BRICK_WALL("Tường gạch",GladeDesign.Kind.BRICK_WALL),
        TIMBER_WALL("Vách gỗ",GladeDesign.Kind.TIMBER_WALL), HEDGE("Rào cây",GladeDesign.Kind.HEDGE),
        ROOF_TILE("Mái ngói đỏ",GladeDesign.Kind.ROOF_TILE), ROOF_SLATE("Mái đá xám",GladeDesign.Kind.ROOF_SLATE),
        PENTAGON("Khối ngũ giác",GladeDesign.Kind.PENTAGON), ROOF_FLAT("Mái bằng",GladeDesign.Kind.ROOF_FLAT),
        ROOF_PYRAMID("Mái chóp bốn dốc",GladeDesign.Kind.ROOF_PYRAMID), ROOF_PENTAGON("Mái ngũ giác",GladeDesign.Kind.ROOF_PENTAGON),
        ROOF_SHED("Mái một dốc",GladeDesign.Kind.ROOF_SHED),
        ROOF_CURVED("Mái ngói cong",GladeDesign.Kind.ROOF_CURVED),
        CHIMNEY_BRICK("Ống khói gạch",GladeDesign.Kind.CHIMNEY_BRICK), CHIMNEY_STONE("Ống khói đá",GladeDesign.Kind.CHIMNEY_STONE),
        WINDOW_ARCH("Cửa sổ vòm",GladeDesign.Kind.WINDOW_ARCH), WINDOW_ROUND("Cửa sổ tròn",GladeDesign.Kind.WINDOW_ROUND),
        DOOR_ARCH("Cửa vòm",GladeDesign.Kind.DOOR_ARCH), DOOR_DOUBLE("Cửa đôi",GladeDesign.Kind.DOOR_DOUBLE),
        STAIRS_STONE("Thang đá",GladeDesign.Kind.STAIRS_STONE), STAIRS_WOOD("Thang gỗ",GladeDesign.Kind.STAIRS_WOOD),
        DECK("Sàn gỗ",GladeDesign.Kind.DECK), PERGOLA("Giàn hoa",GladeDesign.Kind.PERGOLA),
        PLANTER("Bồn hoa",GladeDesign.Kind.PLANTER), WELL("Giếng nước",GladeDesign.Kind.WELL), BENCH("Ghế vườn",GladeDesign.Kind.BENCH),
        RAISE("Đắp đất",null),LOWER("Hạ đất",null),LEVEL("San đất",null);
        boolean terrainBrush() {return this==TERRAIN || this==RAISE || this==LOWER || this==LEVEL;}
        final String label;
        final GladeDesign.Kind kind;
        Tool(String label,GladeDesign.Kind kind) { this.label=label;this.kind=kind; }
    }

    private boolean managedSession,mainMenuOpen,settingsOpen,confirmationOpen,approvedAction;
    private SessionStore sessionStore;
    private SessionStore.Recovery recovery;
    private GladeDesign savedDesign;
    private GladeDesign autosavedDesign;
    private Path currentFile;
    private float autosaveClock;
    private boolean autosavePending,fileDialogOpen;
    private float autosaveInterval;
    private float volume=.55f,cameraSensitivity=1;
    private boolean ambientEnabled,qualityLow,fullscreen;
    private GameSound sounds;
    private DirectionalLightShadowRenderer shadow;
    private FilterPostProcessor effects;
    private final SceneRenderer sceneRenderer=new SceneRenderer();

    private final Node content=new Node("Công trình");
    private final Node preview=new Node("Xem trước");
    private final DesignHistory designHistory=new DesignHistory();
    private GladeDesign design=CozyShowcase.create();
    private Node terrain;
    private LandBounds renderedLand;
    private TerrainField renderedField;
    private List<GladeDesign.Item> renderedLandscape=List.of();
    private Map<Long,WaterFlow.Result> renderedFlows=Map.of();
    private TerrainField terrainDraft;
    private int terrainButton=-1;
    private boolean loweringTerrain;
    private final Map<Long,Float> waterBirth=new HashMap<>();
    private GladeDesign animatedWaterScene;
    private final java.util.concurrent.ExecutorService waterWorker=java.util.concurrent.Executors
            .newSingleThreadExecutor(Thread.ofVirtual().name("dreamhouse-water").factory());
    private record PendingWater(GladeDesign before,GladeDesign after,java.util.concurrent.Future<SceneRules.PreparedWater> task){}
    private PendingWater pendingWater;
    private final Map<Long,GladeDesign.Point> waterOrigins=new HashMap<>();
    private TerrainField waterTerrain;
    private float waterLift;
    private float brushRadius=1.8f,brushClock,brushLevel;
    private float waterRadius=1.4f,pathWidth=1.15f;
    private boolean adjustingBrush;
    private GladeDesign.Finish pathFinish=PathSurface.finish("dirt","");
    private GladeDesign.Point brushPrevious;
    private long draftSupport=-1;
    private boolean stackMode=true,snapBlocks=true,freePlacement;
    private BuildSnap.Result lastSnap;
    private SnapGuide snapGuide;
    private boolean houseFlyout;
    private String flyoutGroup="";
    private java.awt.Rectangle flyoutBounds;
    private float houseHoverClock;
    private final Node landHandles=new Node("Tay nắm mép đất");
    private final Node objectHandles=new Node("Tay nắm công trình");
    private final Node brushOutline=new Node("Vị trí bút đất");
    private Geometry brushRing;
    private float brushRingRadius;
    private float brushCursorClock;
    private LandBounds.Edge landEdge;
    private GladeDesign landDraft;
    private enum ResizeAxis {
        WIDTH, WIDTH_NEG, DEPTH, DEPTH_NEG, HEIGHT, HEIGHT_NEG,ROOF,BASE,ROOF_FRONT,ROOF_BACK,ROOF_LEFT,ROOF_RIGHT,ROTATE,BOTTOM,ELEVATION;
        boolean vertical() {return this==HEIGHT || this==HEIGHT_NEG || this==ROOF || this==BASE || this==ROTATE || this==BOTTOM || this==ELEVATION;}
        boolean width() {return this==WIDTH || this==WIDTH_NEG;}
        float sign() {return this==WIDTH_NEG || this==DEPTH_NEG?-1:1;}
    }
    private ResizeAxis resizeAxis;
    private GladeDesign.Item resizeOriginal;
    private GladeDesign.Item resizedItem;
    private float resizeScreenY,resizeScreenX,resizePlaneY;
    private AxisDrag resizeDrag;
    private GladeDesign clipboardDesign;
    private Long clipboardRoot;
    private int pasteCount;
    private String category="architecture";
    private int catalogPage;
    private boolean panning;
    private final Set<String> cameraKeys=new HashSet<>();
    private final Vector3f cameraVelocity=new Vector3f();
    private boolean walking;
    private final Vector3f walkPosition=new Vector3f();
    private WalkController walkController;
    private float orbitDistance=44,orbitElevation=.62f;

    private final Vector3f focus=new Vector3f(0,0.65f,0);
    private GladeArt art;
    private GladeHud hud;
    private ScreenshotAppState screenshots;
    private Tool tool=Tool.SELECT;
    private Long selected;
    private GladeDesign.Item moving;
    private Spatial hidden;
    private final List<Spatial> hiddenParts=new ArrayList<>();
    private GladeDesign.Point start;
    private GladeDesign.Point current;
    private final List<GladeDesign.Point> stroke=new ArrayList<>();
    private float houseHeight=2.3f;
    private boolean pitched=true;
    private boolean rotating;
    private final CameraDrag cameraDrag=new CameraDrag();
    private boolean drawing;
    private boolean pendingLeft,pendingSelected;
    private final ArrayDeque<Vector2f> leftButtonPositions=new ArrayDeque<>();
    private Vector2f gesturePointer;
    private Vector2f pointer() {return gesturePointer==null?inputManager.getCursorPosition():gesturePointer;}
    private Long pendingPick;
    private final Vector2f leftPress=new Vector2f();
    private BuildGhost buildGhost;
    private float ghostClock;
    private GladeDesign.Point leftGround;
    private String paintTarget="wall";
    private boolean libraryOpen;
    private String libraryGroup="architecture";
    private GladeDesign.Finish activeFinish=GladeDesign.Finish.DEFAULT;
    private java.awt.Rectangle paintBounds;
    private final Node contextHints=new Node("Gợi ý thao tác");
    private final Node feedbackToast=new Node("Kết quả thao tác");
    private String noticedMessage="",noticeKey="";
    private float noticeTime;
    private String lastHint="";
    private boolean control;
    private boolean toolsVisible=true;
    private boolean sceneMenuOpen;
    private boolean blueprintsOpen;
    private boolean helpOpen;
    private final BuildGuide buildGuide=new BuildGuide();
    private BlueprintShelf blueprintShelf;
    private List<BlueprintShelf.Entry> savedModules=List.of();
    private String shelfSelected="",deleteModuleConfirm="";
    private int blueprintPage,skippedModules;
    private BlueprintModule activeModule;
    private ModulePreview modulePreview;
    private GladeDesign modulePreviewSource;
    private GladeDesign.Point modulePreviewPoint;
    private java.awt.Rectangle guideBounds;

    private float yaw=-0.65f;
    private float elevation=0.62f;
    private float distance=44;
    private float elapsed,launchReadyAt;
    private float previewClock;
    private boolean dirtyPreview;
    private int knownWidth;
    private int knownHeight;
    private int photoFrames=-1;
    private boolean photoMode;
    private boolean smokeCaptured;
    private boolean launchReady;
    private long autosaveRequest;
    private int lighting;
    private DirectionalLight sun;
    private AmbientLight ambient;
    private ScenicBackdrop backdrop;
    private CozyAtmosphere atmosphere;
    private String message="Kéo chuột trái để vẽ. Giữ chuột phải để xoay. Cuộn để thu phóng.";
    private LauncherWorld startupWorld=LauncherWorld.MENU;
    private Path startupFile;
    private String startupError;

    public static void main(String[] args) {
        var app=new GladeApp();
        app.managedSession=true;app.mainMenuOpen=true;
        app.startupWorld=LauncherWorld.fromArgs(args);
        if(app.startupWorld==LauncherWorld.FILE)app.startupFile=LauncherWorld.fileFromArgs(args);
        var preferences=PlayerFiles.settings();
        try{app.volume=FastMath.clamp(Float.parseFloat(preferences.getProperty("volume","0.55")),0,1);app.cameraSensitivity=FastMath.clamp(Float.parseFloat(preferences.getProperty("cameraSensitivity","1")),.4f,2);}catch(NumberFormatException ignored){}
        app.ambientEnabled=Boolean.parseBoolean(preferences.getProperty("ambient","false"));app.qualityLow=Boolean.parseBoolean(preferences.getProperty("lowQuality","false"));
        var settings=new AppSettings(true);
        settings.setTitle("DreamHouse · Glade — Dựng & biến đổi kiến trúc");
        settings.setIcons(new Object[]{LauncherIcon.image(16),LauncherIcon.image(32),LauncherIcon.image(64)});
        var size=GladeLauncher.resolution(preferences);
        settings.setResolution(size.width,size.height);
        settings.setSamples(4);
        settings.setVSync(true);
        settings.setResizable(true);
        settings.setGammaCorrection(true);
        settings.setAudioRenderer(Boolean.getBoolean("dreamhouse.noAudio")?null:AppSettings.LWJGL_OPENAL);
        settings.setRenderer(AppSettings.LWJGL_OPENGL33);
        app.setSettings(settings);
        app.setShowSettings(false);
        LaunchSignals.publish("GRAPHICS");
        app.start();
    }

    @Override public void simpleInitApp() {
        LaunchSignals.publish("SCENE");
        flyCam.setEnabled(false);
        var debugKeys=stateManager.getState(com.jme3.app.DebugKeysAppState.class);
        if(debugKeys!=null) stateManager.detach(debugKeys);
        setDisplayStatView(false);
        setDisplayFps(false);
        inputManager.setCursorVisible(true);
        art=new GladeArt(assetManager);
        backdrop=new ScenicBackdrop(assetManager);rootNode.attachChild(backdrop.node);
        rootNode.attachChild(content);
        rootNode.attachChild(preview);
        rootNode.attachChild(landHandles);
        rootNode.attachChild(objectHandles);
        rootNode.attachChild(brushOutline);
        guiNode.attachChild(contextHints);
        guiNode.attachChild(feedbackToast);
        buildGhost=new BuildGhost(assetManager);rootNode.attachChild(buildGhost);
        modulePreview=new ModulePreview(assetManager);rootNode.attachChild(modulePreview);modulePreview.hide();
        snapGuide=new SnapGuide(assetManager);rootNode.attachChild(snapGuide);
        sun=new DirectionalLight(new Vector3f(-0.65f,-1,-0.45f).normalizeLocal(),
                new ColorRGBA(1.1f,0.97f,0.77f,1));
        ambient=new AmbientLight(new ColorRGBA(0.48f,0.54f,0.56f,1));
        rootNode.addLight(sun);
        rootNode.addLight(ambient);
        shadow=new DirectionalLightShadowRenderer(assetManager,2048,3);
        shadow.setLight(sun);
        shadow.setShadowIntensity(0.48f);
        shadow.setEdgeFilteringMode(EdgeFilteringMode.PCFPOISSON);
        shadow.setEdgesThickness(4);
        shadow.setShadowZExtend(72);
        viewPort.addProcessor(shadow);
        effects=new FilterPostProcessor(assetManager);
        effects.addFilter(new SSAOFilter(1.0f,1.45f,0.18f,0.12f));
        atmosphere=new CozyAtmosphere(sun,ambient,viewPort,shadow);
        if(managedSession){
            var savedLight=PlayerFiles.settings().getProperty("lighting","0");
            try{lighting=Math.clamp(Integer.parseInt(savedLight),0,2);}catch(NumberFormatException ignored){lighting=0;}
            atmosphere.select(lighting);
        }
        effects.addFilter(atmosphere.haze);
        var bloom=new BloomFilter(BloomFilter.GlowMode.Objects);bloom.setBloomIntensity(.45f);bloom.setBlurScale(1.2f);effects.addFilter(bloom);
        effects.addFilter(new FXAAFilter());
        viewPort.addProcessor(effects);
        hud=new GladeHud(assetManager);
        guiNode.attachChild(hud.node());
        try {
            PlayerFiles.directory("captures");
            blueprintShelf=new BlueprintShelf(PlayerFiles.directory("blueprints"));reloadBlueprints();
            if(managedSession){sessionStore=new SessionStore(PlayerFiles.directory("recovery"));recovery=sessionStore.recover().orElse(null);savedDesign=design;}
        }
        catch(Exception error) { message="Không tạo được thư mục ảnh."; }
        screenshots=new ScreenshotAppState(PlayerFiles.root().resolve("captures")+java.io.File.separator);
        screenshots.setFileName("DreamHouse-"+java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))+"-");
        stateManager.attach(screenshots);
        if(audioRenderer!=null){try{sounds=new GameSound(volume);sounds.ambient(ambientEnabled);}catch(Exception error){PlayerFiles.error("Không bật được âm thanh",error);}}
        applyQuality();
        if(context instanceof com.jme3.system.lwjgl.LwjglWindow window)org.lwjgl.glfw.GLFW.glfwSetWindowSizeLimits(window.getWindowHandle(),960,640,org.lwjgl.glfw.GLFW.GLFW_DONT_CARE,org.lwjgl.glfw.GLFW.GLFW_DONT_CARE);
        bindInput();
        openStartupWorld();
        rebuild();
        updateCamera();
        rebuildHud();
        LaunchSignals.publish("FIRST_FRAME");
    }

    private void openStartupWorld() {
        var world = switch(startupWorld) {
            case GARDEN -> GardenScene.create();
            case ISLAND -> GardenScene.island();
            case NEW -> new GladeDesign(List.of(),2);
            case RECOVERY -> recovery == null ? null : recovery.design();
            case FILE -> readStartupFile();
            case MENU -> null;
        };
        if(world == null) {
            if(startupWorld == LauncherWorld.RECOVERY) startupError="Chưa có phiên hợp lệ để phục hồi.";
            return;
        }
        design=world;
        savedDesign=world;
        if(startupWorld==LauncherWorld.FILE)currentFile=startupFile;
        mainMenuOpen=false;
        if(startupWorld==LauncherWorld.RECOVERY)recovery=null;
        if(startupWorld==LauncherWorld.GARDEN || startupWorld==LauncherWorld.ISLAND){
            focus.set(0,1.9f,0);yaw=-.38f;elevation=.43f;distance=startupWorld==LauncherWorld.GARDEN?30:22;
        }
        message=startupWorld.title;
    }

    private GladeDesign readStartupFile() {
        if(startupFile!=null) {
            try { return GladeDesign.load(startupFile); }
            catch(IOException | IllegalArgumentException error) { PlayerFiles.error("Không mở được bản lưu từ launcher",error); }
        }
        startupError="Không mở được bản lưu. Hãy chọn Mở bản thiết kế.";
        return null;
    }

    private void bindInput() {
        if(inputManager.hasMapping(INPUT_MAPPING_EXIT)) inputManager.deleteMapping(INPUT_MAPPING_EXIT);
        if(inputManager.hasMapping(INPUT_MAPPING_CAMERA_POS)) inputManager.deleteMapping(INPUT_MAPPING_CAMERA_POS);
        inputManager.addMapping("left",new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("right",new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addMapping("pan",new MouseButtonTrigger(MouseInput.BUTTON_MIDDLE));
        inputManager.addMapping("moveX",new MouseAxisTrigger(MouseInput.AXIS_X,false),new MouseAxisTrigger(MouseInput.AXIS_X,true));
        inputManager.addMapping("moveY",new MouseAxisTrigger(MouseInput.AXIS_Y,false),new MouseAxisTrigger(MouseInput.AXIS_Y,true));
        inputManager.addMapping("zoomIn",new MouseAxisTrigger(MouseInput.AXIS_WHEEL,false));
        inputManager.addMapping("zoomOut",new MouseAxisTrigger(MouseInput.AXIS_WHEEL,true));
        inputManager.addMapping("freePlacement",new KeyTrigger(KeyInput.KEY_LMENU),new KeyTrigger(KeyInput.KEY_RMENU));
        inputManager.addMapping("control",new KeyTrigger(KeyInput.KEY_LCONTROL),new KeyTrigger(KeyInput.KEY_RCONTROL));
        inputManager.addMapping("copyKey",new KeyTrigger(KeyInput.KEY_C));
        inputManager.addMapping("cutKey",new KeyTrigger(KeyInput.KEY_X));
        inputManager.addMapping("pasteKey",new KeyTrigger(KeyInput.KEY_V));
        inputManager.addMapping("undo",new KeyTrigger(KeyInput.KEY_Z));
        inputManager.addMapping("redo",new KeyTrigger(KeyInput.KEY_Y));
        inputManager.addMapping("cancel",new KeyTrigger(KeyInput.KEY_ESCAPE));
        inputManager.addMapping("brushSmaller",new KeyTrigger(KeyInput.KEY_LBRACKET));
        inputManager.addMapping("brushLarger",new KeyTrigger(KeyInput.KEY_RBRACKET));
        inputManager.addMapping("help",new KeyTrigger(KeyInput.KEY_F1));
        inputManager.addMapping("cameraForward",new KeyTrigger(KeyInput.KEY_W),new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("cameraBack",new KeyTrigger(KeyInput.KEY_S),new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("cameraLeft",new KeyTrigger(KeyInput.KEY_A),new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("cameraRight",new KeyTrigger(KeyInput.KEY_D),new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addListener((ActionListener)(name,pressed,tpf) -> {
            if(name.startsWith("camera")){if(pressed)cameraKeys.add(name);else cameraKeys.remove(name);return;}
            if(name.equals("control")) { control=pressed;return; }
            if(photoMode && !name.equals("left") && !name.equals("cancel"))return;
            if(name.equals("freePlacement")){freePlacement=pressed;lastSnap=null;if(snapGuide!=null)snapGuide.hide();if(drawing)dirtyPreview=true;return;}
            if((fileDialogOpen || confirmationOpen || mainMenuOpen || settingsOpen || helpOpen || blueprintsOpen || libraryOpen || sceneMenuOpen) && Set.of("copyKey","cutKey","pasteKey","undo","redo","brushSmaller","brushLarger").contains(name))return;
            if((fileDialogOpen || confirmationOpen) && name.equals("cancel"))return;
            if((fileDialogOpen || confirmationOpen) && name.equals("help"))return;
            if(pressed && (name.equals("brushSmaller") || name.equals("brushLarger"))) {
                float delta=name.equals("brushLarger")?.25f:-.25f;
                if(strokeBrush())setStrokeSize(strokeSize()+delta);
                else if(tool.terrainBrush())brushRadius=FastMath.clamp(brushRadius+delta,.35f,6);
                if(drawing)dirtyPreview=true;
                return;
            }
            if(pressed && control && Set.of("copyKey","cutKey","pasteKey").contains(name)) {
                stopCameraDrag();cancel();
                if(name.equals("pasteKey")) pasteClipboard();else captureClipboard(name.equals("cutKey"));
                rebuildHud();return;
            }
            if(name.equals("help") && pressed) { action("help");return; }
            if(name.equals("right") || name.equals("pan")) return;
            if(name.equals("undo") && pressed && control) { history(false);return; }
            if(name.equals("redo") && pressed && control) { history(true);return; }
            if(name.equals("cancel") && pressed) {if(photoMode){leavePhoto();return;}if(managedSession && (mainMenuOpen || settingsOpen)){settingsOpen=false;mainMenuOpen=false;rebuildHud();return;}if(walking){toggleWalk();return;}stopCameraDrag();cancel();tool=Tool.SELECT;sceneMenuOpen=false;blueprintsOpen=false;helpOpen=false;libraryOpen=false;rebuildHud();return; }
            if(!name.equals("left")) return;
            gesturePointer=leftButtonPositions.pollFirst();
            try {if(!pressed && terrainButton==1)return;if(pressed) beginGesture();else finishGesture();} finally {gesturePointer=null;}
        },"left","right","pan","control","freePlacement","undo","redo","cancel","help","copyKey","cutKey","pasteKey","brushSmaller","brushLarger","cameraForward","cameraBack","cameraLeft","cameraRight");
        inputManager.addListener((AnalogListener)(name,value,tpf) -> {
            if(name.equals("zoomIn")) { zoom(value,true); }
            else if(name.equals("zoomOut")) { zoom(value,false); }
            else if(panning || rotating) {return;}
            else {

                if(drawing) {
                    current=landEdge!=null?rawGround():ground();
                    if(landEdge==null && resizeAxis==null) appendStroke();
                    dirtyPreview=current!=null || resizeAxis!=null && resizeAxis.vertical();
                }
            }
        },"moveX","moveY","zoomIn","zoomOut");
        inputManager.addRawInputListener(new RawInputListener() {
            public void beginInput() {cameraDrag.viewport(cam.getWidth(),cam.getHeight());}
            public void endInput() {applyCameraDrag(cameraDrag.finishBatch());}
            public void onJoyAxisEvent(com.jme3.input.event.JoyAxisEvent e){}
            public void onJoyButtonEvent(com.jme3.input.event.JoyButtonEvent e){}
            public void onMouseButtonEvent(com.jme3.input.event.MouseButtonEvent e) {
                if(e.getButtonIndex()==MouseInput.BUTTON_LEFT)leftButtonPositions.addLast(new Vector2f(e.getX(),e.getY()));
                cameraButton(e.getButtonIndex(),e.isPressed(),e.getX(),e.getY());
            }
            public void onKeyEvent(com.jme3.input.event.KeyInputEvent e){}
            public void onTouchEvent(com.jme3.input.event.TouchEvent e){}
            public void onMouseMotionEvent(com.jme3.input.event.MouseMotionEvent e) {
                cameraDrag.pointer(e.getX(),e.getY());
            }
        });
    }

    private void zoom(float value,boolean inward) {
        if(walking || drawing || pendingLeft || adjustingBrush)return;
        if(mainMenuOpen || settingsOpen || confirmationOpen || fileDialogOpen || helpOpen || blueprintsOpen || libraryOpen || sceneMenuOpen)return;
        var cursor=pointer();
        var panel=hud.bounds("brushPanel");
        if(panel!=null && panel.contains(cursor.x,cursor.y))return;
        float amount=FastMath.clamp(value*.025f,0,.12f);
        distance=FastMath.clamp(distance*(inward?1-amount:1+amount),5,Math.max(design.halfWidth(),design.halfDepth())*3.2f+12);updateCamera();
    }
    private void terrainStroke(boolean lower,int button) {
        if(drawing || pendingLeft || rotating || panning || mainMenuOpen || settingsOpen || confirmationOpen || fileDialogOpen || hud.hit(pointer().x,pointer().y)!=null || pointer().y<124 || helpOpen || sceneMenuOpen || libraryOpen || blueprintsOpen)return;
        current=buildGround();if(current==null){outsidePlot();return;}
        terrainButton=button;loweringTerrain=lower;terrainDraft=design.terrain();brushLevel=terrainDraft.height(current.x(),current.z());brushClock=0;drawing=true;sculpt(.12f);
    }

    private void cameraButton(int button,boolean pressed) {
        var cursor=pointer();cameraButton(button,pressed,cursor.x,cursor.y);
    }
    private void cameraButton(int button,boolean pressed,float x,float y) {
        if(button!=MouseInput.BUTTON_RIGHT && button!=MouseInput.BUTTON_MIDDLE) return;
        if(button==MouseInput.BUTTON_RIGHT && tool==Tool.TERRAIN && !walking && !photoMode){
            gesturePointer=new Vector2f(x,y);try{if(pressed)terrainStroke(true,1);else if(terrainButton==1)finishGesture();}finally{gesturePointer=null;}return;
        }
        if(pressed && (adjustingBrush || helpOpen || sceneMenuOpen || blueprintsOpen || libraryOpen || drawing || pendingLeft || mainMenuOpen || settingsOpen || confirmationOpen || fileDialogOpen || guideBounds!=null && guideBounds.contains(x,y))) return;
        if(pressed && cameraKeys.isEmpty())cameraVelocity.set(0,0,0);
        if(!pressed) cameraDrag.pointer(x,y);
        applyCameraDrag(cameraDrag.finishBatch());
        if(button==MouseInput.BUTTON_RIGHT) rotating=pressed;else panning=pressed;
        var mode=rotating?CameraDrag.Mode.ORBIT:panning?CameraDrag.Mode.PAN:CameraDrag.Mode.NONE;
        cameraDrag.begin(mode,x,y);
        inputManager.setCursorVisible(true);
    }

    private void applyCameraDrag(CameraDrag.Delta delta) {
        if(delta.x()==0 && delta.y()==0) return;
        if(delta.mode()==CameraDrag.Mode.ORBIT) {
            yaw=FastMath.normalize(yaw-delta.x()*.0035f*cameraSensitivity,-FastMath.PI,FastMath.PI);
            elevation=FastMath.clamp(elevation+delta.y()*.0025f*cameraSensitivity,walking?-.85f:.16f,walking?.85f:1.40f);
        } else if(delta.mode()==CameraDrag.Mode.PAN) {
            var shift=CameraController.pan(cam.getLeft(),cam.getDirection(),walking?8:distance,delta.x(),delta.y());
            if(walking)walkController.move(walkPosition,shift);
            else{focus.addLocal(shift);boundCamera(focus);}
        }
        updateCamera();
    }

    private void stopCameraDrag() {
        rotating=false;panning=false;pendingLeft=false;cameraDrag.reset();
        if(inputManager!=null) inputManager.setCursorVisible(true);
    }

    @Override public void loseFocus() {
        stopCameraDrag();cameraKeys.clear();cameraVelocity.set(0,0,0);if(drawing)cancel();control=false;freePlacement=false;leftButtonPositions.clear();lastSnap=null;if(snapGuide!=null)snapGuide.hide();
        super.loseFocus();
    }

    @Override public void gainFocus() {
        stopCameraDrag();super.gainFocus();
    }



    private static float[] defaultSize(Tool option) {
        return switch(option) {
            case TREE -> new float[]{1.8f,1.8f,3.2f};
            case FLOWERS -> new float[]{1.1f,1.1f,.45f};
            case MANOR -> new float[]{4,3,3.8f};
            case CANAL_HOUSE -> new float[]{2.7f,2.6f,3.6f};
            case BRICK_HOUSE -> new float[]{3.1f,2.8f,4.6f};
            case LOG_CABIN -> new float[]{3.8f,3,2.5f};
            case TOWER -> new float[]{3,3,4.4f};
            case PENTAGON -> new float[]{3.8f,3.6f,2.8f};
            case PAVILION -> new float[]{2.8f,2.6f,1.9f};
            case BRIDGE,FENCE -> new float[]{1.4f,1,0.8f};
            case BRICK_WALL -> new float[]{1,1,2.2f};
            case TIMBER_WALL -> new float[]{1,1,1.8f};
            case HEDGE -> new float[]{1,1,1.3f};
            case ROOF_TILE,ROOF_SLATE -> new float[]{3.6f,2.8f,.5f};
            case ROOF_FLAT -> new float[]{3.6f,2.8f,RoofProfiles.SLAB};
            case ROOF_PYRAMID,ROOF_PENTAGON,ROOF_SHED,ROOF_CURVED -> new float[]{3.6f,2.8f,1.25f};
            case CHIMNEY_BRICK,CHIMNEY_STONE -> new float[]{.5f,.5f,1.2f};
            case WINDOW_ARCH -> new float[]{.9f,.5f,1.08f};
            case WINDOW_ROUND -> new float[]{.8f,.5f,.8f};
            case DOOR_ARCH -> new float[]{1.12f,.5f,1.8f};
            case DOOR_DOUBLE -> new float[]{1.6f,.5f,2.1f};
            case STAIRS_STONE,STAIRS_WOOD -> new float[]{1.6f,3,1.5f};
            case DECK -> new float[]{3,2.5f,.65f};
            case PERGOLA -> new float[]{3,2.5f,2.4f};
            case PLANTER -> new float[]{2,1,.5f};
            case WELL -> new float[]{1.5f,1.5f,2.1f};
            case BENCH -> new float[]{2,.8f,1.2f};
            default -> new float[]{3.6f,2.8f,2.3f};
        };
    }

    private Tool[] categoryTools() {
        return switch(category) {
            case "structures" -> new Tool[]{Tool.WALL,Tool.BRICK_WALL,Tool.TIMBER_WALL,Tool.FENCE,Tool.STAIRS_STONE,Tool.BRIDGE,Tool.TOWER};
            case "palette" -> new Tool[]{Tool.ROOF_TILE,Tool.ROOF_SLATE,Tool.BRICK_WALL,Tool.TIMBER_WALL,Tool.WALL,Tool.LOG_CABIN};
            case "windows" -> new Tool[]{Tool.WINDOW_ARCH,Tool.WINDOW_ROUND,Tool.DOOR_ARCH,Tool.DOOR_DOUBLE,Tool.CHIMNEY_BRICK,Tool.CHIMNEY_STONE};
            case "terrain" -> new Tool[]{Tool.TERRAIN};
            case "roofs" -> new Tool[]{Tool.ROOF_TILE,Tool.ROOF_SLATE,Tool.CHIMNEY_BRICK,Tool.CHIMNEY_STONE,Tool.WINDOW_ARCH,Tool.WINDOW_ROUND,Tool.DOOR_ARCH,Tool.DOOR_DOUBLE};
            case "wood" -> new Tool[]{Tool.LOG_CABIN,Tool.PAVILION,Tool.DECK,Tool.PERGOLA,Tool.STAIRS_WOOD,Tool.BRIDGE};
            case "garden" -> new Tool[]{Tool.PLANTER,Tool.WELL,Tool.BENCH,Tool.HEDGE,Tool.TREE,Tool.FLOWERS};
            case "landscape" -> new Tool[]{Tool.PATH};
            default -> new Tool[]{Tool.HOUSE,Tool.TOWER,Tool.PENTAGON,Tool.WALL,Tool.FENCE,Tool.STAIRS_STONE};
        };
    }

    private void placeAttachment() {
        var cursor=pointer();
        var origin=cam.getWorldCoordinates(cursor,0);
        var ray=new Ray(origin,cam.getWorldCoordinates(cursor,1).subtractLocal(origin).normalizeLocal());
        var results=new CollisionResults();content.collideWith(ray,results);
        for(var hit:results) {
            Long id=null;
            for(Spatial spatial=hit.getGeometry();spatial!=null;spatial=spatial.getParent()) {
                id=spatial.getUserData("itemId");if(id!=null) break;
            }
            if(id==null) continue;
            long hitId=id;
            var target=design.items().stream().filter(candidate->candidate.id()==hitId).findFirst().orElse(null);
            if(target==null) continue;
            if(target.mount()!=null) {
                long hostId=target.mount().hostId();
                target=design.items().stream().filter(candidate->candidate.id()==hostId).findFirst().orElse(null);
            }
            if(target==null || !target.kind().building() || target.kind()==GladeDesign.Kind.PAVILION || tool.kind.roof() && !target.kind().roofHost()) continue;
            try {
                var point=new GladeDesign.Point(hit.getContactPoint().x,hit.getContactPoint().z);
                float u=target.localX(point)/target.width(),v=target.localZ(point)/target.depth();
                float angle=0,lift=0;
                GladeDesign.Kind attachmentKind=tool.kind;
                float localHeight=hit.getContactPoint().y-design.placement(target).elevation();
                var attachedRoof=design.attachedRoof(target.id());
                boolean dormer=tool.kind.opening() && tool!=Tool.DOOR_ARCH && tool!=Tool.DOOR_DOUBLE && localHeight>target.height()+.05f
                        && (attachedRoof==null || attachedRoof.kind()!=GladeDesign.Kind.ROOF_FLAT);
                if(tool.kind.roof()) {u=0;v=0;}
                else if(tool.kind.chimney()) {u=FastMath.clamp(u,-.35f,.35f);v=FastMath.clamp(v,-.35f,.35f);}
                else {
                    if(Math.abs(u)>Math.abs(v)) { angle=u>0?90:270;u=Math.copySign(.5f,u);v=FastMath.clamp(v,-.38f,.38f); }
                    else { angle=v>0?0:180;v=Math.copySign(.5f,v);u=FastMath.clamp(u,-.38f,.38f); }
                    lift=tool==Tool.DOOR_ARCH || tool==Tool.DOOR_DOUBLE?0:FastMath.clamp(localHeight/target.height(),.25f,.84f);
                    if(dormer){lift=1;u=FastMath.clamp(target.localX(point)/target.width(),-.40f,.40f);v=FastMath.clamp(target.localZ(point)/target.depth(),-.40f,.40f);}
                    else if(tool.kind.opening() && tool!=Tool.DOOR_ARCH && tool!=Tool.DOOR_DOUBLE && localHeight<.65f){attachmentKind=GladeDesign.Kind.DOOR_ARCH;lift=0;}
                }
                if(target.kind()==GladeDesign.Kind.TOWER && attachmentKind.opening()) {
                    float theta=FastMath.atan2(target.localX(point)/(target.width()/2),target.localZ(point)/(target.depth()/2));
                    u=FastMath.sin(theta)*.5f;v=FastMath.cos(theta)*.5f;angle=theta*FastMath.RAD_TO_DEG;
                }
                if(target.kind()==GladeDesign.Kind.PENTAGON && attachmentKind.opening() && lift<1) {
                    var local=new Vector2f(target.localX(point),target.localZ(point));
                    var edge=Pentagon.nearestEdge(target.width(),target.depth(),local);
                    var snap=edge.closest(local);var normal=edge.normal();
                    u=snap.x/target.width();v=snap.y/target.depth();
                    angle=FastMath.atan2(normal.x,normal.y)*FastMath.RAD_TO_DEG;
                }
                var size=defaultSize(tool);
                if(attachmentKind==GladeDesign.Kind.DOOR_ARCH){size[0]=Math.max(1,size[0]);size[2]=Math.max(1.8f,size[2]);}
                if(tool.kind.roof()) {size[0]=target.width();size[1]=target.depth();}
                var item=new GladeDesign.Item(design.nextId(),attachmentKind,target.center(),size[0],size[1],size[2],true,List.of(),angle,
                        new GladeDesign.Mount(target.id(),u,v,lift));
                item=new GladeDesign.Item(item.id(),item.kind(),item.center(),item.width(),item.depth(),item.height(),item.pitched(),item.points(),item.rotation(),item.mount(),item.form().finish(activeFinish));
                commit(design.add(item),"Đã gắn "+tool.label.toLowerCase(Locale.ROOT)+". Cấu kiện sẽ đi cùng căn nhà.");
            } catch(IllegalArgumentException error) { message=error.getMessage();rebuildHud(); }
            return;
        }
        message="Hãy bấm trực tiếp vào công trình để gắn cấu kiện.";
        rebuildHud();
    }

    private void beginGesture() {
        if(rotating || panning || terrainButton==1 || pendingWater!=null)return;
        var cursor=pointer();
        var button=hud.hit(cursor.x,cursor.y);
        if(photoMode){
            if("photoTake".equals(button) || "photoBack".equals(button))action(button);
            return;
        }
        if(guideBounds!=null && guideBounds.contains(cursor.x,cursor.y) && !"guideHide".equals(button))return;
        if(confirmationOpen || fileDialogOpen)return;
        if(mainMenuOpen || settingsOpen){if(button!=null && (button.startsWith("menu:") || button.startsWith("settings:")))action(button);return;}
        if(libraryOpen) {
            if(button!=null && (button.startsWith("library") || button.startsWith("catalog:")))action(button);
            else {libraryOpen=false;rebuildHud();}
            return;
        }
        if(blueprintsOpen){
            if(button!=null && (button.startsWith("blueprint:") || button.equals("save") || button.equals("load")))action(button);
            else{blueprintsOpen=false;rebuildHud();}
            return;
        }
        if(button!=null) {
            if(helpOpen && !Set.of("helpClose","helpStart").contains(button)) return;
            if(button.equals("brushWidth")){adjustingBrush=true;dragBrush(cursor.x);return;}
            if(button.equals("brushPanel"))return;
            action(button);return;
        }
        if(houseFlyout && flyoutBounds!=null && flyoutBounds.contains(cursor.x,cursor.y)) return;
        if(helpOpen) {helpOpen=false;rebuildHud();return;}
        if(sceneMenuOpen || blueprintsOpen) {sceneMenuOpen=false;blueprintsOpen=false;rebuildHud();return;}
        if((toolsVisible && cursor.y<124) || cursor.y>cam.getHeight()-100
) return;
        if(walking)return;
        if(activeModule!=null){placeModule();return;}
        if(tool==Tool.PAINT) {
            Long hit=pick();
            if(hit==null) {beginLeftPending(null);return;}
            var target=design.items().stream().filter(part->part.id()==hit).findFirst().orElseThrow();
            if(target.mount()!=null){selected=target.mount().hostId();paintTarget=target.kind().roof()?"roof":"wall";}
            else {selected=hit;paintTarget="wall";}
            rebuildHud();return;
        }
        if(tool==Tool.TERRAIN){
            String edge=pickTag(landHandles,"landEdge");
            if(edge!=null){landEdge=LandBounds.Edge.valueOf(edge);landDraft=design;current=rawGround();drawing=true;dirtyPreview=true;return;}
            terrainStroke(false,0);return;
        }
        if(tool==Tool.WATER){
            start=buildGround();waterLift=0;
            var cursorPoint=pointer();var origin=cam.getWorldCoordinates(cursorPoint,0);var ray=new Ray(origin,cam.getWorldCoordinates(cursorPoint,1).subtractLocal(origin).normalizeLocal());var hits=new CollisionResults();content.collideWith(ray,hits);
            if(hits.size()>0){var hit=hits.getClosestCollision();var p=hit.getContactPoint();if(hit.getContactNormal().y>.3f && p.x>design.land().minX()+.26f && p.x<design.land().maxX()-.26f && p.z>design.land().minZ()+.26f && p.z<design.land().maxZ()-.26f){start=new GladeDesign.Point(p.x,p.z);waterLift=Math.max(0,p.y-design.terrain().height(p.x,p.z));}}
            if(start==null){outsidePlot();return;}
            current=start;leftPress.set(pointer());stroke.clear();stroke.add(start);drawing=true;dirtyPreview=true;selected=null;return;
        }
        if(tool.terrainBrush()) {
            current=buildGround();if(current==null){outsidePlot();return;}
            terrainDraft=design.terrain();brushLevel=terrainDraft.height(current.x(),current.z());brushClock=0;drawing=true;
            sculpt(.28f);return;
        }
        if(tool==Tool.LAND) {
            String edge=pickTag(landHandles,"landEdge");
            if(edge==null) { message="Kéo một nút vàng ở mép mảnh đất.";rebuildHud();return; }
            landEdge=LandBounds.Edge.valueOf(edge);
            landDraft=design;
            current=rawGround();
            drawing=true;
            dirtyPreview=true;
            return;
        }
        if(tool.kind!=null && !tool.kind.stroke() && !tool.kind.mounted() && tool!=Tool.WATER) {
            Long hit=pick();if(hit!=null && stackTarget()==null){var target=design.items().stream().filter(i->i.id()==hit).findFirst().orElse(null);if(target!=null && (target.kind().building() || target.kind()==GladeDesign.Kind.DECK || target.mount()!=null)){tool=Tool.SELECT;beginLeftPending(hit);rebuildHud();return;}}
        }
        if(tool.kind!=null && tool.kind.mounted()) { placeAttachment();return; }
        if(tool==Tool.SELECT) {
            String axis=pickTag(objectHandles,"resizeAxis");
            var item=selectedItem();
            if(axis!=null && item!=null) {
                resizeAxis=ResizeAxis.valueOf(axis);
                resizeOriginal=item;
                if(resizeAxis==ResizeAxis.ROOF && design.customRoof(item.id())) resizeOriginal=new GladeDesign.Item(item.id(),item.kind(),item.center(),item.width(),item.depth(),item.height(),item.pitched(),item.points(),item.rotation(),item.mount(),item.form().roof(visibleRoofRise(item,design)));
                resizedItem=resizeOriginal;
                Vector3f localAxis=switch(resizeAxis) {
                    case WIDTH,ROOF_RIGHT -> Vector3f.UNIT_X.clone();
                    case WIDTH_NEG,ROOF_LEFT -> Vector3f.UNIT_X.negate();
                    case DEPTH,ROOF_FRONT -> Vector3f.UNIT_Z.clone();
                    case DEPTH_NEG,ROOF_BACK -> Vector3f.UNIT_Z.negate();
                    default -> Vector3f.UNIT_Y.clone();
                };
                var worldAxis=new Quaternion().fromAngleAxis(design.placement(item).rotation()*FastMath.DEG_TO_RAD,Vector3f.UNIT_Y).mult(localAxis);
                var grab=new Vector3f[1];objectHandles.depthFirstTraversal(part->{if(axis.equals(part.getUserData("resizeAxis")) && grab[0]==null)grab[0]=part.getWorldTranslation().clone();});
                resizeDrag=grab[0]==null?null:new AxisDrag(cam,grab[0],worldAxis,cursor);
                resizeScreenY=cursor.y;resizeScreenX=cursor.x;
                resizePlaneY=design.placement(item).elevation()+item.height()+((resizeAxis==ResizeAxis.ROOF_FRONT || resizeAxis==ResizeAxis.ROOF_BACK)?visibleRoofRise(item,design):resizeAxis==ResizeAxis.ROOF_LEFT || resizeAxis==ResizeAxis.ROOF_RIGHT?.22f:0);
                if(resizeAxis==ResizeAxis.ROTATE) resizePlaneY=design.placement(item).elevation();
                start=ground();
                current=start;
                hideItem(item.id());
                drawing=true;
                dirtyPreview=true;
                return;
            }
        }
        if(tool==Tool.SELECT) {
            Long hit=pick();if(pickTag(objectHandles,"moveHandle")!=null)hit=selected;
            beginLeftPending(hit);return;
        }
        start=tool==Tool.ERASE?ground():buildGround();
        if(tool.kind!=null && tool!=Tool.WATER && !tool.kind.stroke() && !tool.kind.mounted() && stackMode) {
            var support=stackTarget();
            if(support!=null){
                var target=supportPoint(support);
                if(target!=null){draftSupport=support.id();start=target;}
            }
        }
        if(start==null){if(tool!=Tool.ERASE)outsidePlot();return;}
        Long id=pick();
        if(tool==Tool.SELECT && pickTag(objectHandles,"moveHandle")!=null) id=selected;
        if(tool==Tool.ERASE) {
            if(id!=null){
                int count=design.assemblyIds(id).size();
                String feedback=count==1?"Đã xóa công trình.":design.item(id).stonePlatform()
                        ?"Đã xóa nền · Các khối bên trên hạ xuống địa hình."
                        :"Đã xóa cả cụm "+count+" khối · Ctrl+Z để khôi phục.";
                commit(design.remove(id),feedback);
            }
            return;
        }
        if(tool==Tool.SELECT) {
            selected=id;
            moving=id==null?null:selectedItem();
            if(moving==null) { rebuildHud();return; }
            houseHeight=moving.height();
            pitched=moving.pitched();
            if(moving.mount()!=null) {
                moving=null;
                message="Cấu kiện đi cùng căn nhà. Có thể chỉnh kích thước, xoay, nhân bản hoặc xóa để đặt lại.";
                rebuildHud();return;
            }
            hideItem(id);
        }
        current=start;
        stroke.clear();
        stroke.add(start);
        drawing=true;
        dirtyPreview=true;
        if(tool!=Tool.PATH)rebuildHud();
    }

    private void beginLeftPending(Long hit) {
        pendingLeft=true;pendingPick=hit;pendingSelected=hit!=null && Objects.equals(hit,selected) && tool==Tool.SELECT;
        leftPress.set(pointer());leftGround=ground();
    }
    private void updateLeftGesture() {
        if(!pendingLeft || !pendingSelected || pointer().distance(leftPress)<=8)return;
        pendingLeft=false;
        if(leftGround==null)return;
        var item=selectedItem();if(item==null || item.mount()!=null)return;
        moving=item;start=leftGround;current=ground();if(current==null)current=start;
        hideItem(item.id());drawing=true;dirtyPreview=true;stroke.clear();stroke.add(start);refreshHandles();
    }

    private void finishGesture() {
        if(adjustingBrush){dragBrush(pointer().x);adjustingBrush=false;return;}
        updateLeftGesture();
        if(pendingLeft) {pendingLeft=false;if(pointer().distance(leftPress)<=8){selected=pendingPick;if(tool!=Tool.PAINT)tool=Tool.SELECT;}rebuildHud();return;}
        if(!drawing) return;
        try {
            if(terrainDraft!=null) {
                var end=ground();
                if(end!=null && brushPrevious!=null && brushPrevious.distance(end)>brushRadius*.16f){
                    current=end;applySculpt(.12f);
                }
                var changed=design.withTerrain(terrainDraft);
                terrainDraft=null;
                commit(changed,loweringTerrain || tool==Tool.LOWER?"Đã hạ đất. Ctrl+Z để hoàn tác.":"Đã chỉnh địa hình. Ctrl+Z để hoàn tác.");
                return;
            }
            if(landEdge!=null) {
                current=rawGround();
                updateLandDraft();
                if(landDraft!=null && !landDraft.land().equals(design.land())) {
                    commit(landDraft,"Đã kéo mảnh đất. Có thể hoàn tác để trở lại kích thước cũ.");
                    var prior=selected;selected=null;focusSelected();selected=prior;
                }
                return;
            }
            if(resizeAxis!=null) {
                current=ground();
                updateResizeDraft();
                if(resizedItem!=null && !resizedItem.equals(resizeOriginal))
                    commit(resizeScene(resizedItem),"Đã thay đổi kích thước bằng tay nắm.");
                return;
            }
            var release=ground();
            if(release!=null) current=release;
            if(moving!=null && start.distance(current)<0.02f) {
                rebuildHud();
                return;
            }
            if(tool==Tool.WATER && moving==null && pointer().distance(leftPress)<8){current=start;stroke.clear();stroke.add(start);}
            appendStroke();
            if((tool==Tool.TREE || tool==Tool.FLOWERS) && moving==null) {
                var all=new ArrayList<>(design.items());all.addAll(natureDrafts());
                commit(new GladeDesign(all,design.land(),design.terrain()),"Đã tô một nét cây / hoa. Ctrl+Z hoàn tác cả nét.");return;
            }
            var item=draft();
            if(moving!=null && moving.equals(item)){rebuildHud();return;}
            if(item!=null){
                if(moving==null && tool==Tool.WATER && item.points().size()>=8){
                    var after=design.add(item);
                    pendingWater=new PendingWater(design,after,waterWorker.submit(()->SceneRules.PreparedWater.build(after,item)));
                    drawing=false;dirtyPreview=false;
                    message="Đang chuẩn bị dòng nước · Esc hoặc Ctrl+Z để hủy nét.";
                    return;
                }
                boolean placed=moving==null && previewStructure();
                boolean strokeAdded=moving==null && strokeBrush() && !design.items().isEmpty();
                commit(moving==null?design.add(item):design.replace(item),"Đã cập nhật cảnh.",strokeAdded);
                if(placed){selected=item.id();tool=Tool.SELECT;rebuildHud();}
            }
        } catch(IllegalArgumentException error) {
            message=error.getMessage();
            rebuildHud();
        } finally { if(pendingWater==null)cancel(); }
    }

    private void cancel() {
        if(pendingWater!=null){pendingWater.task().cancel(true);pendingWater=null;}
        adjustingBrush=false;
        activeModule=null;modulePreviewSource=null;modulePreviewPoint=null;if(modulePreview!=null)modulePreview.clear();
        pendingLeft=false;lastSnap=null;if(snapGuide!=null)snapGuide.hide();if(buildGhost!=null)buildGhost.hide();
        boolean sculpting=terrainDraft!=null;
        terrainDraft=null;terrainButton=-1;loweringTerrain=false;brushPrevious=null;brushClock=0;draftSupport=-1;
        drawing=false;waterLift=0;
        moving=null;
        landEdge=null;
        landDraft=null;
        resizeAxis=null;resizeDrag=null;
        resizeOriginal=null;
        resizedItem=null;
        if(hidden!=null) hidden.setCullHint(Spatial.CullHint.Inherit);
        for(var part:hiddenParts) part.setCullHint(Spatial.CullHint.Inherit);
        hiddenParts.clear();
        hidden=null;
        preview.detachAllChildren();
        if(sculpting) rebuild();
        refreshHandles();
    }

    private void finishWater() {
        var pending=pendingWater;
        if(pending==null || !pending.task().isDone())return;
        pendingWater=null;
        if(design!=pending.before()){cancel();return;}
        SceneRules.PreparedWater result;
        try{result=pending.task().get();}
        catch(InterruptedException error){Thread.currentThread().interrupt();cancel();return;}
        catch(java.util.concurrent.ExecutionException | java.util.concurrent.CancellationException error){
            cancel();
            PlayerFiles.error("Chuẩn bị dòng nước",error);
            message="Chưa thể tạo dòng nước. Hãy thử vẽ lại.";
            return;
        }
        SceneRules.install(pending.after(),result);
        commit(pending.after(),"Đã tạo dòng nước. Ctrl+Z để hoàn tác.",!design.items().isEmpty());
    }

    private List<GladeDesign.Item> natureDrafts() {
        var result=new ArrayList<GladeDesign.Item>();var size=defaultSize(tool);long id=design.nextId();
        for(var point:stroke){
            var center=new GladeDesign.Point(FastMath.clamp(point.x(),design.land().minX()+size[0]/2,design.land().maxX()-size[0]/2),FastMath.clamp(point.z(),design.land().minZ()+size[1]/2,design.land().maxZ()-size[1]/2));
            result.add(new GladeDesign.Item(id++,tool.kind,center,size[0],size[1],houseHeight,false,List.of(),0,null,new GladeDesign.Form(0,0,0,draftSupport).finish(activeFinish)));
            if(result.size()==128)break;
        }
        return result;
    }

    private GladeDesign.Item draft() {
        if(start==null || current==null) return null;
        if(moving!=null) {
            float dx=current.x()-start.x(),dz=current.z()-start.z();
            boolean footprint=moving.kind().footprint();
            float halfW=footprint?moving.halfWidth():0.3f;
            float halfD=footprint?moving.halfDepth():0.3f;
            float minX=moving.center().x()-halfW,maxX=moving.center().x()+halfW;
            float minZ=moving.center().z()-halfD,maxZ=moving.center().z()+halfD;
            for(var point:moving.points()) {
                minX=Math.min(minX,point.x());maxX=Math.max(maxX,point.x());
                minZ=Math.min(minZ,point.z());maxZ=Math.max(maxZ,point.z());
            }
            var assembly=design.assemblyIds(moving.id());
            for(var part:design.items())if(part.id()!=moving.id() && assembly.contains(part.id()) && part.mount()==null && part.kind().footprint()){
                minX=Math.min(minX,part.center().x()-part.halfWidth());maxX=Math.max(maxX,part.center().x()+part.halfWidth());minZ=Math.min(minZ,part.center().z()-part.halfDepth());maxZ=Math.max(maxZ,part.center().z()+part.halfDepth());
            }
            dx=FastMath.clamp(dx,design.land().minX()-minX,design.land().maxX()-maxX);
            dz=FastMath.clamp(dz,design.land().minZ()-minZ,design.land().maxZ()-maxZ);
            float x=moving.center().x()+dx,z=moving.center().z()+dz;
            var translated=new ArrayList<GladeDesign.Point>();
            for(var point:moving.points()) translated.add(new GladeDesign.Point(
                    point.x()+dx,point.z()+dz));
            var moved=new GladeDesign.Item(moving.id(),moving.kind(),new GladeDesign.Point(x,z),
                    moving.width(),moving.depth(),moving.height(),moving.pitched(),translated,moving.rotation(),moving.mount(),moving.form());
            var placed=design.releaseUnsupportedMove(moved);
            return placed.form().support()!=moving.form().support()?placed:snapPlacement(placed);
        }
        if(tool==Tool.WATER) {
            return WaterGesture.draft(design,pointer().distance(leftPress)<8?List.of(start):stroke,pointer().distance(leftPress)<8?start:current,waterRadius,waterLift);
        }
        if(tool==Tool.PATH) {
            var points=new ArrayList<>(stroke);
            if(points.getLast().distance(current)>.025f && points.size()<4096)points.add(current);
            return new GladeDesign.Item(design.nextId(),GladeDesign.Kind.PATH,start,pathWidth,pathWidth,1,false,points,0,null,
                    new GladeDesign.Form(0,0,0,-1).finish(pathFinish));
        }
        float width=Math.max(0.8f,Math.abs(current.x()-start.x()));
        float depth=Math.max(0.8f,Math.abs(current.z()-start.z()));
        width=Math.min(design.land().width(),width);depth=Math.min(design.land().depth(),depth);
        float x=(start.x()+current.x())/2,z=(start.z()+current.z())/2;
        if(start.distance(current)<0.15f) { var size=defaultSize(tool);width=size[0];depth=size[1];x=start.x();z=start.z(); }
        x=FastMath.clamp(x,design.land().minX()+width/2,design.land().maxX()-width/2);
        z=FastMath.clamp(z,design.land().minZ()+depth/2,design.land().maxZ()-depth/2);
        var center=new GladeDesign.Point(x,z);
        List<GladeDesign.Point> points=List.of();
        if(tool.kind!=null && tool.kind.stroke()) {
            points=tool.kind.wallStroke() && stroke.size()>1?List.copyOf(stroke)
                    :List.of(start,start.distance(current)<0.10f
                    ?new GladeDesign.Point(FastMath.clamp(start.x()+1.5f,design.land().minX(),design.land().maxX()),start.z()):current);
            width=tool==Tool.BRIDGE?1.4f:1;
            depth=1;
        }
        if(tool==Tool.FENCE && points.size()>1){var snapped=new ArrayList<>(points);snapped.set(0,SceneRules.snapFence(design,snapped.getFirst()));snapped.set(snapped.size()-1,SceneRules.snapFence(design,snapped.getLast()));points=List.copyOf(snapped);}
        if(tool==Tool.TREE || tool==Tool.FLOWERS) { center=current;width=1;depth=1; }
        float roof=tool.kind.building() && (tool!=Tool.HOUSE || activeFinish.variant()==18 || activeFinish.variant()==GladeDesign.HILLSIDE_STYLE || activeFinish.variant()>0 && activeFinish.variant()<7)?Math.min(32,width*(tool==Tool.TOWER?.60f:.32f)):0;
        return snapPlacement(new GladeDesign.Item(design.nextId(),tool.kind,center,width,depth,houseHeight,roof>0,points,0,null,
                new GladeDesign.Form(0,roof,0,draftSupport).finish(activeFinish)));
    }

    private GladeDesign.Item snapPlacement(GladeDesign.Item item){
        if(!snapBlocks || freePlacement){lastSnap=null;if(snapGuide!=null)snapGuide.hide();return item;}
        lastSnap=BuildSnap.resolve(design,item,FastMath.clamp(distance*.006f,.08f,.32f),lastSnap);
        if(snapGuide!=null)snapGuide.show(lastSnap);
        return lastSnap.neighbor()<0?item:BuildSnap.copy(item,lastSnap.center());
    }

    private GladeDesign.Point buildGround(){return ground(true);}
    private void outsidePlot(){message="Hãy bấm bên trong viền khu xây. Dùng Địa hình để kéo rộng đất.";}
    private GladeDesign.Point ground(){return ground(false);}
    private GladeDesign.Point ground(boolean insidePlot) {
        var position=rawGround(insidePlot);
        if(position==null) return null;
        if(resizeAxis!=null) return position;
        return new GladeDesign.Point(FastMath.clamp(position.x(),design.land().minX()+0.2f,design.land().maxX()-0.2f),
                FastMath.clamp(position.z(),design.land().minZ()+0.2f,design.land().maxZ()-0.2f));
    }

    private GladeDesign.Point rawGround(){return rawGround(false);}
    private GladeDesign.Point rawGround(boolean insidePlot) {
        var cursor=pointer();
        var origin=cam.getWorldCoordinates(cursor,0);
        var direction=cam.getWorldCoordinates(cursor,1).subtractLocal(origin).normalizeLocal();
        if(!Vector3f.isValidVector(origin) || !Vector3f.isValidVector(direction) || direction.lengthSquared()<.5f)return null;
        var position=new Vector3f();var ray=new Ray(origin,direction);
        if(resizeAxis!=null && (!resizeAxis.vertical() || resizeAxis==ResizeAxis.ROTATE)) {
            if(!ray.intersectsWherePlane(new Plane(Vector3f.UNIT_Y,resizePlaneY),position)) return null;
        } else if(draftSupport>=0) {
            long supportId=draftSupport;var support=design.items().stream().filter(item->item.id()==supportId).findFirst().orElse(null);
            if(support==null)return null;var point=supportPoint(support);if(point==null)return null;position.set(point.x(),design.top(support),point.z());
        } else {
            var hits=new CollisionResults();var surface=terrain==null?null:terrain.getChild("Mặt đất");
            if(surface!=null && landEdge==null) surface.collideWith(ray,hits);
            if(hits.size()>0) position.set(hits.getClosestCollision().getContactPoint());
            else if(insidePlot || !ray.intersectsWherePlane(new Plane(Vector3f.UNIT_Y,0),position)) return null;
        }
        if(!Vector3f.isValidVector(position))return null;
        return new GladeDesign.Point(FastMath.clamp(position.x,-LandBounds.LIMIT_X,LandBounds.LIMIT_X),
                FastMath.clamp(position.z,-LandBounds.LIMIT_Z,LandBounds.LIMIT_Z));
    }

    private GladeDesign.Point supportPoint(GladeDesign.Item support){
        var cursor=pointer();var origin=cam.getWorldCoordinates(cursor,0);var ray=new Ray(origin,cam.getWorldCoordinates(cursor,1).subtractLocal(origin).normalizeLocal());var hits=new CollisionResults();content.collideWith(ray,hits);
        for(var hit:hits)for(Spatial node=hit.getGeometry();node!=null;node=node.getParent()){
            Long id=node.getUserData("itemId");if(id==null)continue;
            var object=design.items().stream().filter(i->i.id()==id).findFirst().orElse(null);
            if(id==support.id() || object!=null && object.mount()!=null && object.mount().hostId()==support.id())return new GladeDesign.Point(hit.getContactPoint().x,hit.getContactPoint().z);
            break;
        }
        var point=new Vector3f();return ray.intersectsWherePlane(new Plane(Vector3f.UNIT_Y,design.top(support)),point)?new GladeDesign.Point(FastMath.clamp(point.x,-LandBounds.LIMIT_X,LandBounds.LIMIT_X),FastMath.clamp(point.z,-LandBounds.LIMIT_Z,LandBounds.LIMIT_Z)):null;
    }

    private GladeDesign.Item stackTarget() {
        var cursor=pointer();var origin=cam.getWorldCoordinates(cursor,0);
        var hits=new CollisionResults();content.collideWith(new Ray(origin,cam.getWorldCoordinates(cursor,1).subtractLocal(origin).normalizeLocal()),hits);
        for(var hit:hits) {
            if(hit.getContactNormal().y<.20f) return null;
            for(Spatial spatial=hit.getGeometry();spatial!=null;spatial=spatial.getParent()) {
                Long id=spatial.getUserData("itemId");if(id==null) continue;
                var item=design.items().stream().filter(part->part.id()==id).findFirst().orElse(null);
                if(item!=null && item.mount()!=null) {long host=item.mount().hostId();item=design.items().stream().filter(part->part.id()==host).findFirst().orElse(null);}
                if(item!=null && (item.kind().building() || item.kind()==GladeDesign.Kind.DECK) && hit.getContactPoint().y>=design.top(item)-.10f) return item;
                return null;
            }
        }
        return null;
    }

    private void sculpt(float amount) {
        if(current==null || terrainDraft==null)return;
        applySculpt(amount);
        showDesign(design.withTerrain(terrainDraft));
    }

    private void applySculpt(float amount) {
        if(current==null || terrainDraft==null) return;
        var previous=brushPrevious==null?current:brushPrevious;
        int steps=Math.max(1,Math.min(24,(int)Math.ceil(previous.distance(current)/(brushRadius*.35f))));
        float strength=(tool==Tool.LOWER || tool==Tool.TERRAIN && loweringTerrain?-amount:amount)/(float)Math.sqrt(steps);
        for(int i=1;i<=steps;i++) {
            float t=(float)i/steps;
            terrainDraft=terrainDraft.brush(FastMath.interpolateLinear(t,previous.x(),current.x()),FastMath.interpolateLinear(t,previous.z(),current.z()),
                    brushRadius,strength,tool==Tool.LEVEL?brushLevel:null);
        }
        brushPrevious=current;
    }

    private Long pick() {
        var cursor=pointer();
        var origin=cam.getWorldCoordinates(cursor,0);
        var direction=cam.getWorldCoordinates(cursor,1).subtractLocal(origin).normalizeLocal();
        if(!Vector3f.isValidVector(origin) || !Vector3f.isValidVector(direction) || direction.lengthSquared()<.5f)return null;
        var results=new CollisionResults();
        content.collideWith(new Ray(origin,direction),results);
        for(var hit:results){
            var geometry=hit.getGeometry();
            boolean hidden=false;
            for(Spatial parent=geometry;parent!=content && parent!=null;parent=parent.getParent())
                if(parent.getCullHint()==Spatial.CullHint.Always){hidden=true;break;}
            if(hidden)continue;
            for(Spatial node=geometry;node!=content && node!=null;node=node.getParent()) {
                Long id=node.getUserData("itemId");
                if(id!=null)return id;
            }
        }
        return null;
    }

    private GladeDesign.Item selectedItem() {
        return design.items().stream().filter(item->Objects.equals(item.id(),selected)).findFirst().orElse(null);
    }

    private void hideItem(long id) {
        hidden=content.getChildren().stream().filter(node->Objects.equals(id,node.getUserData("itemId"))).findFirst().orElse(null);
        if(hidden!=null) hidden.setCullHint(Spatial.CullHint.Always);
        var related=design.assemblyIds(id);
        for(var item:design.items()) if(item.id()!=id && related.contains(item.id()))
            for(var spatial:content.getChildren()) if(Objects.equals(item.id(),spatial.getUserData("itemId"))) {
                spatial.setCullHint(Spatial.CullHint.Always);hiddenParts.add(spatial);
            }
    }

    private void previewItem(GladeDesign.Item item,GladeDesign temporary) {
        preview.detachAllChildren();
        preview.attachChild(art.render(item,temporary));
        var related=temporary.assemblyIds(item.id());
        for(var part:temporary.items()) if(part.id()!=item.id() && related.contains(part.id()))
            preview.attachChild(art.render(part,temporary));
    }

    private String pickTag(Node group,String key) {
        var cursor=pointer();
        if(group==objectHandles && key.equals("resizeAxis")){
            String[] nearest={null};float[] best={64};
            group.depthFirstTraversal(part->{if(part.getName().startsWith("Đầu thước ")){var screen=cam.getScreenCoordinates(part.getWorldTranslation());float distance=cursor.distanceSquared(new Vector2f(screen.x,screen.y));if(screen.z>=0 && screen.z<=1 && distance<best[0]){best[0]=distance;nearest[0]=part.getUserData(key);}}});
            if(nearest[0]!=null)return nearest[0];
        }
        var origin=cam.getWorldCoordinates(cursor,0);
        var direction=cam.getWorldCoordinates(cursor,1).subtractLocal(origin).normalizeLocal();
        var results=new CollisionResults();
        var ray=new Ray(origin,direction);
        group.depthFirstTraversal(spatial->{if(spatial instanceof Geometry geometry) {
            for(Spatial parent=geometry;parent!=null && parent!=group;parent=parent.getParent())
                if(parent.getUserData(key)!=null) {geometry.collideWith(ray,results);break;}
        }});
        for(var hit:results) for(Spatial node=hit.getGeometry();node!=null;node=node.getParent()) {
            String value=node.getUserData(key);
            if(value!=null) return value;
        }
        return null;
    }

    private void updateLandDraft() {
        if(landEdge==null || current==null) return;
        float coordinate=landEdge==LandBounds.Edge.LEFT || landEdge==LandBounds.Edge.RIGHT?current.x():current.z();
        landDraft=design.resizeLand(landEdge,coordinate);
        var bounds=landDraft.land();
        float edge=switch(landEdge){
            case LEFT->bounds.minX();case RIGHT->bounds.maxX();case BACK->bounds.minZ();case FRONT->bounds.maxZ();
        };
        if(Math.abs(edge-coordinate)>.15f)message="Đã tới giới hạn. Cần chừa đủ đất cho công trình hiện có.";
    }

    private GladeDesign resizeScene(GladeDesign.Item item) {
        var next=design.replace(item);
        if(resizeAxis==ResizeAxis.ROOF && item.kind().building()) {
            for(var part:design.items()) if(part.kind().roof() && part.mount()!=null && part.mount().hostId()==item.id()) {
                if(item.form().roof()<=.02f) next=next.remove(part.id());
                else {
                    float height=RoofProfiles.shaped(part.kind())?RoofProfiles.rise(part.kind(),item.form().roof())
                            :item.form().roof()/((design.placement(part).width()/2+.32f)*.62f)*.5f;
                    next=next.replace(new GladeDesign.Item(part.id(),part.kind(),part.center(),part.width(),part.depth(),height,true,part.points(),part.rotation(),part.mount(),part.form()));
                }
            }
        }
        return next;
    }

    private void updateResizeDraft() {
        if(resizeOriginal==null || resizeDrag==null) return;
        float w=resizeOriginal.width(),d=resizeOriginal.depth(),h=resizeOriginal.height();
        var placement=design.placement(resizeOriginal);
        float delta=resizeDrag.delta(pointer());
        if(!Float.isFinite(delta))return;
        var form=resizeOriginal.form();
        switch(resizeAxis) {
            case WIDTH,WIDTH_NEG -> w=FastMath.clamp(w+2*delta,.5f,design.land().width());
            case DEPTH,DEPTH_NEG -> d=FastMath.clamp(d+2*delta,.5f,design.land().depth());
            case ROOF -> form=form.roof(FastMath.clamp(form.roof()+delta,0,32));
            case BASE -> form=form.stilts(FastMath.clamp(form.stilts()+delta,0,32));
            case BOTTOM -> {float amount=FastMath.clamp(delta,Math.max(-32-form.base(),h-160),Math.min(160-form.base(),h-.04f));h=FastMath.clamp(h-amount,.04f,160);form=form.base(FastMath.clamp(form.base()+amount,-32,160));}
            case ELEVATION -> form=form.base(FastMath.clamp(form.base()+delta,-32,160));
            case HEIGHT,HEIGHT_NEG -> h=FastMath.clamp(h+delta,.04f,160);
            case ROOF_FRONT -> form=form.ends(FastMath.clamp(form.roofFront()+delta,0,32),form.roofBack());
            case ROOF_BACK -> form=form.ends(form.roofFront(),FastMath.clamp(form.roofBack()+delta,0,32));
            case ROOF_LEFT -> form=form.sides(FastMath.clamp(form.roofLeft()+delta,0,32),form.roofRight());
            case ROOF_RIGHT -> form=form.sides(form.roofLeft(),FastMath.clamp(form.roofRight()+delta,0,32));
            default -> {}
        }
        if(h<=.12f) form=form.roof(0);
        float rotation=resizeOriginal.rotation();
        if(resizeAxis==ResizeAxis.ROTATE && current!=null && start!=null) {
            float initial=FastMath.atan2(start.x()-placement.center().x(),start.z()-placement.center().z());
            float now=FastMath.atan2(current.x()-placement.center().x(),current.z()-placement.center().z());
            rotation+=FastMath.normalize(now-initial,-FastMath.PI,FastMath.PI)*FastMath.RAD_TO_DEG;
        }
        try {
            var candidate=new GladeDesign.Item(resizeOriginal.id(),resizeOriginal.kind(),resizeOriginal.center(),
                    w,d,h,resizeOriginal.pitched(),resizeOriginal.points(),rotation,resizeOriginal.mount(),form);
            design.replace(candidate);
            resizedItem=candidate;
        } catch(IllegalArgumentException outside) {
            message="Đã chạm mép đất. Kéo rộng đất hoặc dời công trình trước.";
        }
    }

    private void handle(Node group,Vector3f position,String key,String value,String tint) {
        var holder=new Node("Tay nắm");
        holder.setUserData(key,value);
        var knob=new Geometry("Nút kéo",new Sphere(12,18,0.18f));
        var mat=new com.jme3.material.Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color",ColorRGBA.White.clone());
        knob.setMaterial(mat);
        holder.attachChild(knob);
        var ring=new Geometry("Vòng tay nắm",new Torus(24,8,0.035f,0.30f));
        ring.rotate(-FastMath.HALF_PI,0,0);
        ring.setMaterial(mat);
        holder.attachChild(ring);
        holder.setLocalTranslation(position);
        group.attachChild(holder);
    }

    private void refreshHandles() {
        if(photoMode){objectHandles.detachAllChildren();landHandles.detachAllChildren();return;}
        landHandles.detachAllChildren();
        objectHandles.detachAllChildren();
        landHandles.setShadowMode(RenderQueue.ShadowMode.Off);
        objectHandles.setShadowMode(RenderQueue.ShadowMode.Off);
        if(tool==Tool.LAND || tool==Tool.TERRAIN) {
            var land=landDraft==null?design.land():landDraft.land();
            var field=terrainDraft==null?design.terrain():terrainDraft;
            handle(landHandles,new Vector3f(land.minX(),field.height(land.minX(),land.centerZ())+.20f,land.centerZ()),"landEdge","LEFT","#f5c566");
            handle(landHandles,new Vector3f(land.maxX(),field.height(land.maxX(),land.centerZ())+.20f,land.centerZ()),"landEdge","RIGHT","#f5c566");
            handle(landHandles,new Vector3f(land.centerX(),field.height(land.centerX(),land.minZ())+.20f,land.minZ()),"landEdge","BACK","#f5c566");
            handle(landHandles,new Vector3f(land.centerX(),field.height(land.centerX(),land.maxZ())+.20f,land.maxZ()),"landEdge","FRONT","#f5c566");
            return;
        }
        var item=resizedItem==null?selectedItem():resizedItem;
        if(moving!=null && current!=null) {var live=draft();if(live!=null)item=live;}
        if(mainMenuOpen || settingsOpen || confirmationOpen)return;
        if(tool!=Tool.SELECT || item==null || !item.kind().footprint() || item.kind()==GladeDesign.Kind.WATER) return;
        var placement=design.placement(item);
        var frame=new Node("Khung kích thước");
        float w=item.width()/2,d=item.depth()/2,h=item.height(),roof=visibleRoofRise(item,design);
        float bottom=-item.form().stilts(),top=h+roof;
        for(int side:new int[]{-1,1}) {
            dashed(frame,new Vector3f(-w,bottom,side*d),new Vector3f(w,bottom,side*d));
            dashed(frame,new Vector3f(-w,h,side*d),new Vector3f(w,h,side*d));
            dashed(frame,new Vector3f(side*w,bottom,-d),new Vector3f(side*w,bottom,d));
            dashed(frame,new Vector3f(side*w,h,-d),new Vector3f(side*w,h,d));
            for(int z:new int[]{-1,1}) dashed(frame,new Vector3f(side*w,bottom,z*d),new Vector3f(side*w,h,z*d));
            directionHandle(frame,new Vector3f(side*(w+.42f),h,0),new Vector3f(side,0,0),side>0?"WIDTH":"WIDTH_NEG","#ffffff");
            directionHandle(frame,new Vector3f(0,h,side*(d+.42f)),new Vector3f(0,0,side),side>0?"DEPTH":"DEPTH_NEG","#ffffff");
        }
        if(item.kind()!=GladeDesign.Kind.WATER) {
            directionHandle(frame,new Vector3f(0,top+.55f,0),Vector3f.UNIT_Y,item.kind().building()?"ROOF":"HEIGHT","#ffffff");
            if(roof>.02f && item.kind().roofHost()) {
                for(int side:new int[]{-1,1}) {
                    float extension=side>0?item.form().roofRight():item.form().roofLeft();
                    directionHandle(frame,new Vector3f(side*(w+extension+.5f),h+.22f,d*.70f),new Vector3f(side,0,0),side>0?"ROOF_RIGHT":"ROOF_LEFT","#ffffff");
                    dashed(frame,new Vector3f(-w,h,side*d),new Vector3f(0,top,side*d));
                    dashed(frame,new Vector3f(w,h,side*d),new Vector3f(0,top,side*d));
                }
            }
            float rx=w+1.05f,rz=-d-.6f;
            whiteLine(frame,.025f,h-bottom,.025f,rx,(h+bottom)/2,rz);
            float spread=h-bottom<.4f?.45f:0;
            if(spread>0){whiteLine(frame,spread,.025f,.025f,rx-spread/2,h,rz);whiteLine(frame,spread,.025f,.025f,rx+spread/2,bottom,rz);}
            rulerKnob(frame,new Vector3f(rx-spread,h,rz),"HEIGHT");
            rulerKnob(frame,new Vector3f(rx+spread,bottom,rz),"BOTTOM");
            rulerKnob(frame,new Vector3f(rx,(h+bottom)/2,rz),"ELEVATION");
            var turn=new Node("Kéo để xoay");turn.setUserData("resizeAxis","ROTATE");turn.setUserData("scaledGrip",true);
            for(int i=0;i<16;i++) {float a=i*.11f;float b=(i+1)*.11f;
                dashed(turn,new Vector3f(.55f*FastMath.cos(a),0,.55f*FastMath.sin(a)),new Vector3f(.55f*FastMath.cos(b),0,.55f*FastMath.sin(b)));}
            for(float a:new float[]{0,1.65f}) {
                var tip=new Geometry("Đầu cung xoay",new com.jme3.scene.shape.Cylinder(2,12,.001f,.14f,.23f,true,false));
                var direction=new Vector3f(-FastMath.sin(a),0,FastMath.cos(a));if(a==0)direction.negateLocal();
                var turnRotation=new Quaternion();turnRotation.lookAt(direction,Vector3f.UNIT_Y);tip.setLocalRotation(turnRotation);
                tip.setLocalTranslation(.55f*FastMath.cos(a),0,.55f*FastMath.sin(a));tip.setMaterial(whiteMaterial());tip.setQueueBucket(RenderQueue.Bucket.Transparent);turn.attachChild(tip);
            }
            var grip=new Geometry("Nút xoay",new Sphere(8,12,.14f));grip.setMaterial(whiteMaterial());grip.setLocalTranslation(0,0,.55f);turn.attachChild(grip);
            turn.setLocalTranslation(w+.5f,bottom+.12f,d+.5f);frame.attachChild(turn);
        }
        var move=new Node("Dời khối");move.setUserData("moveHandle","MOVE");move.setUserData("scaledGrip",true);
        whiteLine(move,.65f,.025f,.045f,0,0,0);whiteLine(move,.045f,.025f,.65f,0,0,0);
        move.setLocalTranslation(0,h+.10f,0);frame.attachChild(move);
        frame.rotate(0,placement.rotation()*FastMath.DEG_TO_RAD,0);
        frame.setLocalTranslation(placement.center().x(),placement.elevation(),placement.center().z());
        objectHandles.attachChild(frame);updateHandleScales();
    }

    private float visibleRoofRise(GladeDesign.Item item,GladeDesign scene) {
        if(scene.hasUpper(item.id())) return 0;
        var custom=scene.attachedRoof(item.id());
        if(custom==null)return scene.roofRise(item);
        return RoofProfiles.shaped(custom.kind())?RoofProfiles.rise(custom.kind(),custom.height())
                :(scene.placement(custom).width()/2+.32f)*.62f*custom.height()/.5f;
    }
    private void rulerKnob(Node frame,Vector3f position,String axis) {
        var holder=new Node("Đầu thước "+axis);holder.setUserData("resizeAxis",axis);holder.setUserData("scaledGrip",true);
        var knob=new Geometry("Điểm kéo",new com.jme3.scene.shape.Cylinder(2,6,.14f,.045f,true));
        knob.setMaterial(whiteMaterial());knob.setQueueBucket(RenderQueue.Bucket.Transparent);holder.attachChild(knob);
        var hit=new Geometry("Vùng bắt thước",new Sphere(8,12,.22f));hit.setMaterial(whiteMaterial());hit.setCullHint(Spatial.CullHint.Always);holder.attachChild(hit);
        holder.setLocalTranslation(position);frame.attachChild(holder);
    }
    private void dashed(Node frame,Vector3f from,Vector3f to) {
        float length=from.distance(to);int count=Math.max(1,(int)(length/.22f));
        float[] coords=new float[count*6];
        for(int i=0;i<count;i++) {var a=new Vector3f().interpolateLocal(from,to,(float)i/count);var b=new Vector3f().interpolateLocal(from,to,(i+.55f)/count);
            int n=i*6;coords[n]=a.x;coords[n+1]=a.y;coords[n+2]=a.z;coords[n+3]=b.x;coords[n+4]=b.y;coords[n+5]=b.z;}
        var mesh=new Mesh();mesh.setMode(Mesh.Mode.Lines);mesh.setBuffer(VertexBuffer.Type.Position,3,coords);mesh.updateBound();
        var line=new Geometry("Viền nét đứt",mesh);line.setMaterial(whiteMaterial());line.setQueueBucket(RenderQueue.Bucket.Transparent);frame.attachChild(line);
    }

    private void directionHandle(Node group,Vector3f position,Vector3f direction,String axis,String tint) {
        var holder=new Node("Mũi tên kéo");holder.setUserData("resizeAxis",axis);holder.setUserData("scaledGrip",true);
        var mat=new com.jme3.material.Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color",ColorRGBA.White.clone());
        var shaft=new Geometry("Cán",new com.jme3.scene.shape.Cylinder(2,12,.055f,.35f,true));
        var tip=new Geometry("Đầu mũi tên",new com.jme3.scene.shape.Cylinder(2,16,.001f,.17f,.29f,true,false));
        var rotation=new Quaternion();rotation.lookAt(direction,Math.abs(direction.y)>.9f?Vector3f.UNIT_Z:Vector3f.UNIT_Y);
        shaft.setLocalRotation(rotation);tip.setLocalRotation(rotation);
        tip.setLocalTranslation(direction.mult(.26f));shaft.setMaterial(mat);tip.setMaterial(mat);
        mat.getAdditionalRenderState().setDepthTest(false);mat.getAdditionalRenderState().setDepthWrite(false);
        shaft.setQueueBucket(RenderQueue.Bucket.Transparent);tip.setQueueBucket(RenderQueue.Bucket.Transparent);
        holder.attachChild(shaft);holder.attachChild(tip);holder.setLocalTranslation(position);group.attachChild(holder);
        var hit=new Geometry("Vùng bắt mũi tên",new Sphere(8,12,.20f));hit.setMaterial(whiteMaterial());hit.setCullHint(Spatial.CullHint.Always);holder.attachChild(hit);
    }

    private com.jme3.material.Material whiteMaterial() {
        var mat=new com.jme3.material.Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color",ColorRGBA.White.clone());
        mat.getAdditionalRenderState().setDepthTest(false);mat.getAdditionalRenderState().setDepthWrite(false);return mat;
    }
    private void whiteLine(Node group,float w,float h,float d,float x,float y,float z) {
        var line=art.box(group,w,h,d,x,y,z,"#ffffff");line.setMaterial(whiteMaterial());line.setQueueBucket(RenderQueue.Bucket.Transparent);
    }

    private void previewLand() {
        if(landDraft==null) return;
        preview.detachAllChildren();
        preview.attachChild(TerrainMesh.draftBoundary(art,landDraft));
        refreshHandles();
    }

    private void rotateSelected(float angle) {
        var item=selectedItem();
        if(item==null || !item.kind().footprint()) { message="Chọn căn nhà, tháp, chòi hoặc hồ để xoay.";return; }
        try {
            var rotated=new GladeDesign.Item(item.id(),item.kind(),item.center(),item.width(),item.depth(),
                    item.height(),item.pitched(),item.points(),item.rotation()+angle,item.mount(),item.form());
            commit(design.replace(rotated),"Đã xoay công trình. Đường và cửa được cập nhật theo hướng mới.");
        } catch(IllegalArgumentException outside) { message="Không đủ chỗ xoay. Dời công trình hoặc kéo rộng đất."; }
    }

    private void commit(GladeDesign next,String feedback) {
        commit(next,feedback,false);
    }

    private void commit(GladeDesign next,String feedback,boolean updateCountOnly) {
        buildGuide.observe(design,next);
        cancel();
        designHistory.record(design);
        design=next;autosaveClock=0;
        if(sounds!=null)sounds.place();
        message=feedback;
        rebuild();
        if(updateCountOnly && knownWidth>=720 && knownHeight>=540 && cam.getWidth()==knownWidth && cam.getHeight()==knownHeight)
            hud.stat("CÔNG TRÌNH",design.items().size()+" khối","HOUSE",knownWidth-168,knownHeight-65);
        else rebuildHud();
    }

    private void history(boolean forward) {
        if(pendingWater!=null){cancel();message="Đã hủy nét nước đang chuẩn bị.";return;}
        var next=designHistory.step(design,forward);
        if(next==null)return;
        cancel();
        design=next;autosaveClock=0;
        selected=null;
        rebuild();
        rebuildHud();
    }

    private void rebuild() {
        showDesign(design);
    }
    private void showDesign(GladeDesign displayed) {
        var landscape=displayed.items().stream().filter(i->i.kind()==GladeDesign.Kind.FENCE || i.kind()==GladeDesign.Kind.WATER).toList();
        var flows=SceneRules.of(displayed).flows;
        if(!displayed.land().equals(renderedLand) || displayed.terrain()!=renderedField) {
            if(terrain!=null) terrain.removeFromParent();
            if(renderedLand==null || !displayed.land().equals(renderedLand))backdrop.rebuild(art,displayed.land());
            terrain=art.meadow(displayed);
            terrain.setLocalTranslation(displayed.land().centerX(),0,displayed.land().centerZ());
            updatePlotBoundary();
            rootNode.attachChild(terrain);
            terrain.depthFirstTraversal(part->part.forceRefresh(false,false,true));
            renderedLand=displayed.land();renderedField=displayed.terrain();
            boundCamera(focus);
            updateCamera();
        } else if(!landscape.equals(renderedLandscape) || !flows.equals(renderedFlows))ReactiveFeatures.trimGrass(terrain,displayed);
        renderedLandscape=landscape;renderedFlows=flows;
        sceneRenderer.update(content,art,displayed);
        if(walking){
            content.updateGeometricState();
            walkController=new WalkController(displayed,content);
            walkController.spawn(walkPosition,walkPosition.x,walkPosition.z);
            updateCamera();
        }
        refreshHandles();
    }

    private void action(String id) {
        if(confirmationOpen)return;
        if(managedSession && !approvedAction && Set.of("sample","village","hillsideEast","hillsideWest","clear","collection","load","menu:new","menu:load","menu:exit").contains(id)){
            var discardAction=id;guardDiscard(()->{approvedAction=true;try{action(discardAction);}finally{approvedAction=false;}});return;
        }
        if(sounds!=null)sounds.tap();
        if(id.startsWith("settings:")){setting(id.substring(9));return;}
        if(id.startsWith("menu:")){
            startupError=null;
            switch(id.substring(5)){
                case "continue" -> {if(recovery!=null){replaceWorld(recovery.design(),null);recovery=null;}mainMenuOpen=false;settingsOpen=false;}
                case "new" -> {
                    var emptyWorld = new GladeDesign(List.of(), 2);
                    replaceWorld(emptyWorld, null);
                    savedDesign = emptyWorld;
                    mainMenuOpen = false;
                }
                case "load" -> chooseFile(false);
                case "save" -> chooseFile(true);
                case "settings" -> settingsOpen=true;
                case "help" -> {mainMenuOpen=false;settingsOpen=false;helpOpen=true;}
                case "back" -> {settingsOpen=false;mainMenuOpen=false;}
                case "exit" -> stop();
            }
            rebuildHud();return;
        }
        if(id.equals("mainMenu")){stopCameraDrag();cancel();mainMenuOpen=true;settingsOpen=false;rebuildHud();return;}

        if(walking && (Set.of("architecture","landscape","windows","trees","garden","terrain","water","palette","sample","clear").contains(id) || id.startsWith("tool:") || id.startsWith("house:") || id.startsWith("libraryPick:")))toggleWalk();
        stopCameraDrag();
        cancel();
        if(id.equals("helpStart")){helpOpen=false;blueprintsOpen=false;libraryOpen=false;houseFlyout=false;buildGuide.start();tool=Tool.SELECT;message="Học nhanh đã bật. Thực hành trên cảnh hiện tại; Ctrl+Z để hoàn tác.";rebuildHud();return;}
        if(id.equals("guideHide")){buildGuide.active=false;rebuildHud();return;}
        if(id.startsWith("blueprint:")){blueprintAction(id.substring(10));return;}
        if(id.startsWith("libraryGroup:")) {String group=id.substring(13);if(Set.of("terrain","water").contains(group)){action(group);return;}libraryGroup=group;rebuildHud();return;}
        if(id.equals("libraryClose")) {libraryOpen=false;rebuildHud();return;}
        if(id.startsWith("libraryPick:")) {
            var entry=GladeCatalog.entries(libraryGroup).get(Integer.parseInt(id.substring(12)));
            tool=Tool.valueOf(entry.tool());activeFinish=GladeCatalog.finish(entry);selected=null;libraryOpen=false;
            houseHeight=tool==Tool.DECK && entry.variant()==GladeDesign.STONE_PLATFORM?1.65f:
                    tool==Tool.HOUSE && entry.variant()>=7 && entry.variant()<=9?(entry.variant()==9?2.2f:entry.variant()==8?.85f:.7f):defaultSize(tool)[2];
            if(tool==Tool.WATER)waterRadius=entry.variant()==1?.65f:entry.variant()==2?2.6f:1.4f;
            message=entry.label()+" · Bấm hoặc kéo để đặt.";rebuildHud();return;
        }
        if(id.equals("architecture")){libraryOpen=false;houseFlyout=true;flyoutGroup="architecture";category="architecture";tool=Tool.SELECT;selected=null;rebuildHud();return;}
        if(id.equals("landscape")){tool=Tool.PATH;libraryOpen=false;houseFlyout=false;flyoutGroup="";sceneMenuOpen=false;selected=null;rebuildHud();return;}
        if(id.startsWith("pathMaterial:")) {
            pathFinish=PathSurface.finish(id.substring(13),pathFinish.color());rebuildHud();return;
        }
        if(id.startsWith("pathColor:")) {
            String color=id.substring(10);
            pathFinish=PathSurface.finish(pathFinish.material(),color.equals("reset")?"":color);rebuildHud();return;
        }
        if(id.equals("pathCustomColor") || id.equals("paintPathCustomColor")) {
            choosePathColor(id.equals("paintPathCustomColor"));return;
        }
        if(id.equals("terrain") || id.equals("water") || id.equals("libraryGroup:terrain") || id.equals("libraryGroup:water")) {
            boolean terrain=id.endsWith("terrain");tool=terrain?Tool.TERRAIN:Tool.WATER;libraryOpen=false;houseFlyout=false;flyoutGroup="";selected=null;rebuildHud();return;
        }
        if(Set.of("architectureLibrary","roofs","windows","trees","garden").contains(id)) {
            if(walking)toggleWalk();
            libraryGroup=id.equals("architectureLibrary")?"architecture":id;libraryOpen=true;houseFlyout=false;flyoutGroup="";rebuildHud();return;
        }
        if(id.startsWith("paintTarget:")) {paintTarget=id.substring(12);rebuildHud();return;}
        if(id.equals("paintStyle:east") || id.equals("paintStyle:west")){
            var house=selectedItem();
            if(house!=null && (house.kind().roofHost() || house.stonePlatform())){
                boolean eastern=id.equals("paintStyle:east");
                var styled=house.stonePlatform()?design.styleAssembly(house,eastern):design.styleHouse(house,eastern);
                if(styled!=design)commit(styled,eastern?"Đã áp dụng phong cách Phố đồi.":"Đã áp dụng phong cách Nhà gỗ Âu.");
            }
            return;
        }
        if(id.startsWith("paintMaterial:")) {paint("material",id.substring(14));rebuildHud();return;}
        if(id.startsWith("paintColor:")) {paint("color",id.substring(11));rebuildHud();return;}
        if(id.equals("palette")) {paintTarget="wall";tool=Tool.PAINT;houseFlyout=false;flyoutGroup="";message="Chọn khối để mở vòng vật liệu và màu.";rebuildHud();return;}
        if(id.startsWith("catalog:")) {category=id.substring(8);if(libraryOpen)libraryGroup=category;catalogPage=0;rebuildHud();return;}
        if(id.startsWith("house:")) {id="tool:"+id.substring(6);category="architecture";catalogPage=0;}
        houseFlyout=false;flyoutGroup="";
        if(id.equals("sceneMenu")) {sceneMenuOpen=!sceneMenuOpen;blueprintsOpen=false;rebuildHud();return;}
        if(id.equals("blueprints")) {blueprintsOpen=!blueprintsOpen;sceneMenuOpen=false;libraryOpen=false;helpOpen=false;deleteModuleConfirm="";if(blueprintsOpen)reloadBlueprints();rebuildHud();return;}
        if(id.equals("help")) {helpOpen=!helpOpen;mainMenuOpen=false;settingsOpen=false;libraryOpen=false;sceneMenuOpen=false;blueprintsOpen=false;rebuildHud();return;}
        if(id.equals("helpClose")) {helpOpen=false;rebuildHud();return;}
        sceneMenuOpen=false;
        blueprintsOpen=false;
        if(id.startsWith("tool:")) {
            activeFinish=GladeDesign.Finish.DEFAULT;tool=Tool.valueOf(id.substring(5));
            if(Set.of(Tool.HOUSE,Tool.TOWER,Tool.PENTAGON,Tool.WALL,Tool.FENCE,Tool.DECK,Tool.STAIRS_STONE).contains(tool))activeFinish=GladeCatalog.plain();
            selected=null;
            houseHeight=defaultSize(tool)[2];
            message=tool.kind!=null && tool.kind.mounted()?"Chọn cấu kiện rồi bấm lên mái hoặc mặt tường của một căn nhà.":"Bấm để đặt mẫu, hoặc kéo trên đất để tự chọn kích thước.";
        } else if(Set.of("architecture","structures","roofs","wood","garden","landscape","windows","terrain").contains(id)) {
            category=id;catalogPage=0;tool=Tool.SELECT;
            message="Chọn một cấu kiện. Các trang kế tiếp có thêm mẫu để phối kiến trúc.";
        } else switch(id) {
            case "undo" -> history(false);
            case "redo" -> history(true);
            case "save" -> chooseFile(true);
            case "load" -> chooseFile(false);
            case "sample" -> {commit(GardenScene.create(),"Vườn mơ ước · Mọi công trình đều có thể chọn, kéo và lưu. Giữ phải để xoay, giữ giữa để kéo cảnh.");focus.set(0,1.9f,0);yaw=-.38f;elevation=.43f;distance=30;updateCamera();}
            case "village" -> {commit(RiversideVillage.create(),"Làng ven suối · Chọn và chỉnh từng công trình trong cảnh.");focus.set(0,1.7f,0);yaw=-.15f;elevation=.62f;distance=64;updateCamera();}
            case "hillsideEast", "hillsideWest" -> {
                if(walking)toggleWalk();
                boolean east=id.equals("hillsideEast");
                sceneMenuOpen=false;
                selected=null;tool=Tool.SELECT;
                commit(HillsideDistrict.create(east),east?"Phố dốc Trùng Khánh":"Phố dốc Vieille Ville");
                lighting=east?2:0;
                atmosphere.select(lighting);
                focus.set(1,east?8.5f:5.6f,-1);
                yaw=east?.50f:-.16f;
                elevation=east?.36f:.22f;
                distance=east?70:67;
                updateCamera();
            }
            case "collection" -> {selected=null;commit(GladeDesign.collection(),"Bộ cấu kiện mẫu: thử chọn, chỉnh, di chuyển nhà để xem các chi tiết đi cùng.");}
            case "clear" -> commit(new GladeDesign(List.of(),design.land(),design.terrain()),"Thung lũng trống đã sẵn sàng.");
            case "expand" -> {
                tool=Tool.LAND;
                category="landscape";catalogPage=0;
                selected=null;
                message="Kéo nút vàng ở một cạnh đất. Cạnh đối diện sẽ đứng yên.";
                focusSelected();
            }
            case "previous" -> catalogPage=Math.max(0,catalogPage-1);
            case "next" -> catalogPage=Math.min((categoryTools().length-1)/6,catalogPage+1);
            case "copy" -> copySelected();
            case "focus" -> focusSelected();
            case "viewAll" -> {
                var previous=selected;selected=null;focusSelected();selected=previous;
            }
            case "rotate-" -> rotateSelected(-15);
            case "rotate+" -> rotateSelected(15);
            case "walk" -> toggleWalk();
            case "photo" -> {photoMode=true;brushOutline.setCullHint(Spatial.CullHint.Always);updatePlotBoundary();refreshHandles();rebuildHud();return;}
            case "photoTake" -> {if(photoMode && photoFrames<0){photoFrames=2;hud.node().setCullHint(Spatial.CullHint.Always);}return;}
            case "photoBack" -> {leavePhoto();return;}
            case "cameraSave" -> {
                try{new CameraBookmark(focus,yaw,elevation,distance).save(currentFile);message="Đã lưu góc nhìn.";}
                catch(IOException error){message="Không lưu được góc nhìn.";PlayerFiles.error("Lưu góc nhìn",error);}
            }
            case "cameraRecall" -> {
                var saved=CameraBookmark.read(currentFile);
                if(saved==null)message="Chưa có góc nhìn. Bấm Lưu góc nhìn trong menu trước.";
                else{
                    if(walking)toggleWalk();
                    focus.set(saved.focus());boundCamera(focus);
                    yaw=FastMath.normalize(saved.yaw(),-FastMath.PI,FastMath.PI);
                    elevation=FastMath.clamp(saved.elevation(),.16f,1.40f);
                    distance=FastMath.clamp(saved.distance(),5,Math.max(design.halfWidth(),design.halfDepth())*3.2f+12);
                    updateCamera();message="Đã trở về góc nhìn đã lưu.";
                }
            }
            case "tools" -> toolsVisible=!toolsVisible;
            case "stack" -> stackMode=!stackMode;
            case "snap" -> snapBlocks=!snapBlocks;
            
            case "height+" -> alter(0.25f,0,0);
            case "height-" -> alter(-0.25f,0,0);
            case "width+" -> alter(0,0.5f,0);
            case "width-" -> alter(0,-0.5f,0);
            case "depth+" -> alter(0,0,0.5f);
            case "depth-" -> alter(0,0,-0.5f);
            case "roof" -> {
                pitched=!pitched;
                alter(0,0,0);
            }
            case "light", "sceneLight" -> {
                lighting=(lighting+1)%3;
                atmosphere.select(lighting);
                message="Ánh sáng: "+CozyAtmosphere.label(lighting);
                if(managedSession){
                    var prefs=PlayerFiles.settings();prefs.setProperty("lighting",Integer.toString(lighting));
                    try{PlayerFiles.saveSettings(prefs);}catch(IOException error){PlayerFiles.error("Lưu ánh sáng",error);}
                }
            }
            default -> {}
        }
        rebuildHud();
    }

    private void leavePhoto(){
        stopCameraDrag();
        photoMode=false;photoFrames=-1;
        updatePlotBoundary();
        hud.node().setCullHint(Spatial.CullHint.Inherit);
        rebuildHud();
    }

    private void updatePlotBoundary(){
        var outline=terrain==null?null:terrain.getChild("Viền khu xây dựng");
        if(outline!=null)outline.setCullHint(photoMode?Spatial.CullHint.Always:Spatial.CullHint.Inherit);
    }

    private void alter(float heightDelta,float widthDelta,float depthDelta) {
        houseHeight=FastMath.clamp(houseHeight+heightDelta,0.5f,160);
        if(selected==null) return;
        var item=design.items().stream().filter(candidate->candidate.id()==selected).findFirst().orElse(null);
        if(item==null) return;
        if(!item.kind().footprint() && !item.kind().stroke()) {
            message="Chọn căn nhà hoặc tường để chỉnh kích thước.";
            return;
        }
        if(item.kind().wallStroke()
                && (widthDelta!=0 || depthDelta!=0)) {
            message="Tường thay đổi chiều cao. Chọn căn nhà để đổi mặt bằng.";
            return;
        }
        float width=FastMath.clamp(item.width()+widthDelta,0.5f,design.land().width());
        float depth=FastMath.clamp(item.depth()+depthDelta,0.5f,design.land().depth());
        var form=item.form();
        if(pitched!=item.pitched()) form=form.roof(pitched?Math.max(.5f,width*.32f):0);
        try {
            commit(design.replace(new GladeDesign.Item(item.id(),item.kind(),item.center(),width,
                    depth,houseHeight,pitched,item.points(),item.rotation(),item.mount(),form)),"Kiến trúc đã cập nhật theo kích thước.");
        } catch(IllegalArgumentException error) { message=error.getMessage(); }
    }

    private boolean paintable(GladeDesign.Kind kind) {
        return kind.building() || kind.name().contains("WALL") || Set.of(GladeDesign.Kind.PATH,GladeDesign.Kind.FENCE,GladeDesign.Kind.BRIDGE,GladeDesign.Kind.DECK,GladeDesign.Kind.PERGOLA,GladeDesign.Kind.BENCH,GladeDesign.Kind.PLANTER,GladeDesign.Kind.WELL,GladeDesign.Kind.STAIRS_WOOD,GladeDesign.Kind.STAIRS_STONE).contains(kind);
    }

    private void paint(String field,String value) {
        var item=selectedItem();if(item==null)return;
        if(!paintable(item.kind())) {message="Chọn công trình, hàng rào hoặc đường đi để đổi vật liệu.";return;}
        if(value.equals("reset")) value="";
        var old=item.form().finish();
        int variant=item.kind()==GladeDesign.Kind.PATH && field.equals("material") && PathSurface.MATERIALS.contains(value)?PathSurface.VARIABLE_WIDTH:old.variant();
        var finish=new GladeDesign.Finish(paintTarget.equals("wall") && field.equals("material")?value:old.material(),
            paintTarget.equals("wall") && field.equals("color")?value:old.color(),
            paintTarget.equals("roof") && field.equals("material")?value:old.roofMaterial(),
            paintTarget.equals("roof") && field.equals("color")?value:old.roofColor(),variant);
        if(finish.equals(old))return;
        var updated=new GladeDesign.Item(item.id(),item.kind(),item.center(),item.width(),item.depth(),item.height(),item.pitched(),item.points(),item.rotation(),item.mount(),item.form().finish(finish));
        commit(design.replace(updated),"Đã cập nhật "+(paintTarget.equals("roof")?"mái":"bề mặt và kết cấu")+" đang chọn. Ctrl+Z để hoàn tác.");
    }

    private void choosePathColor(boolean editing) {
        var item=editing?selectedItem():null;
        if(fileDialogOpen || editing && (item==null || item.kind()!=GladeDesign.Kind.PATH))return;
        var finish=editing?item.form().finish():pathFinish;
        var initial=java.awt.Color.decode(finish.color().isEmpty()?PathSurface.defaultColor(finish.material()):finish.color());
        fileDialogOpen=true;
        SwingUtilities.invokeLater(()->{
            java.awt.Color chosen=null;
            try{chosen=JColorChooser.showDialog(null,"Chọn màu đường",initial,false);}
            finally{
                String tint=chosen==null?"":String.format(Locale.ROOT,"#%06x",chosen.getRGB()&0xffffff);
                enqueue(()->{
                    fileDialogOpen=false;
                    if(tint.isEmpty())return;
                    if(editing && tool==Tool.PAINT && Objects.equals(selected,item.id()))paint("color",tint);
                    else if(!editing && tool==Tool.PATH)pathFinish=PathSurface.finish(pathFinish.material(),tint);
                    rebuildHud();
                });
            }
        });
    }

    private void reloadBlueprints(){
        if(blueprintShelf==null)return;
        try{var listing=blueprintShelf.list();savedModules=listing.entries();skippedModules=listing.skipped();blueprintPage=Math.max(0,Math.min(blueprintPage,Math.max(0,(savedModules.size()-1)/6)));if(savedModules.stream().noneMatch(e->e.id().equals(shelfSelected)))shelfSelected=savedModules.isEmpty()?"":savedModules.getFirst().id();}
        catch(Exception error){message="Không đọc được hộp mẫu: "+error.getMessage();PlayerFiles.error("Đọc hộp bản thiết kế",error);}
    }
    private BlueprintShelf.Entry shelfEntry(){return savedModules.stream().filter(e->e.id().equals(shelfSelected)).findFirst().orElse(null);}
    private void blueprintAction(String command){
        if(command.equals("close")){blueprintsOpen=false;rebuildHud();return;}
        if(command.startsWith("preset:")){
            startModule(ArchitecturePresets.catalog().get(Integer.parseInt(command.substring(7))).module());return;
        }
        if(command.startsWith("pick:")){shelfSelected=command.substring(5);deleteModuleConfirm="";rebuildHud();return;}
        if(command.equals("previous") || command.equals("next")){blueprintPage+=command.equals("next")?1:-1;blueprintPage=Math.max(0,Math.min(blueprintPage,Math.max(0,(savedModules.size()-1)/6)));deleteModuleConfirm="";rebuildHud();return;}
        if(command.equals("save")){nameBlueprint(false);return;}
        if(command.equals("rename")){nameBlueprint(true);return;}
        var entry=shelfEntry();if(entry==null){message="Chọn một mẫu đã lưu trong hộp.";rebuildHud();return;}
        if(command.equals("place")){
            startModule(entry.module());return;
        }
        if(command.equals("delete")){
            if(!deleteModuleConfirm.equals(entry.id())){deleteModuleConfirm=entry.id();message="Bấm Xác nhận xóa để bỏ mẫu trong hộp; công trình đã đặt vẫn giữ.";}
            else try{blueprintShelf.delete(entry.id());deleteModuleConfirm="";reloadBlueprints();message="Đã xóa mẫu trong hộp; công trình trong cảnh vẫn giữ.";}catch(Exception error){message="Không xóa được mẫu: "+error.getMessage();}
            rebuildHud();
        }
    }
    private void startModule(BlueprintModule module){
        blueprintsOpen=false;helpOpen=false;libraryOpen=false;selected=null;tool=Tool.SELECT;sceneMenuOpen=false;
        activeModule=module;modulePreview.clear();buildGhost.hide();
        message="Rê chuột để xem khung mẫu · Bấm trái để đặt · Esc để hủy.";
        updateModulePreview();rebuildHud();
    }
    private void nameBlueprint(boolean rename){
        if(fileDialogOpen)return;
        if(blueprintShelf==null){message="Không mở được thư mục bản thiết kế.";rebuildHud();return;}
        var entry=shelfEntry();BlueprintModule captured;
        try{if(rename){if(entry==null)throw new IllegalArgumentException("Chọn một mẫu để đổi tên.");captured=entry.module();}else{if(selected==null)throw new IllegalArgumentException("Đóng hộp, chọn công trình rồi mở lại để lưu mẫu.");captured=BlueprintModule.capture(design,selected);}}
        catch(Exception error){message=error.getMessage();rebuildHud();return;}
        fileDialogOpen=true;String initial=rename?entry.name():"Công trình của tôi";
        SwingUtilities.invokeLater(()->{
            try{
                String name=JOptionPane.showInputDialog(null,rename?"Tên mới (tối đa 40 ký tự)":"Tên mẫu (gồm khối chọn, tầng và cấu kiện đi cùng)",initial);
                if(name==null)return;
                var saved=rename?blueprintShelf.rename(entry.id(),name):blueprintShelf.save(name,captured);
                enqueue(()->acceptBlueprint(saved,rename));
            }catch(Exception error){enqueue(()->{message="Không lưu được mẫu: "+error.getMessage();rebuildHud();});}
            finally{enqueue(()->fileDialogOpen=false);}
        });
    }
    private void acceptBlueprint(BlueprintShelf.Entry saved,boolean rename){
        shelfSelected=saved.id();deleteModuleConfirm="";reloadBlueprints();for(int i=0;i<savedModules.size();i++)if(savedModules.get(i).id().equals(saved.id()))blueprintPage=i/6;
        if(!rename)buildGuide.mark(BuildGuide.Goal.BLUEPRINT);message=rename?"Đã đổi tên mẫu.":"Đã cất mẫu công trình. Chọn mẫu rồi bấm Đặt mẫu để dùng lại.";rebuildHud();
    }
    private void updateModulePreview(){
        if(activeModule==null || modulePreview==null)return;
        buildGhost.hide();
        if(rotating || panning || mainMenuOpen || settingsOpen || helpOpen || blueprintsOpen || libraryOpen || fileDialogOpen || confirmationOpen || hud.hit(pointer().x,pointer().y)!=null || pointer().y<124){modulePreview.hide();return;}
        var point=buildGround();if(point==null){modulePreview.hide();return;}
        if(modulePreviewSource==design && point.equals(modulePreviewPoint)){modulePreview.setCullHint(Spatial.CullHint.Inherit);return;}
        try{modulePreview.show(activeModule.place(design,point));modulePreviewSource=design;modulePreviewPoint=point;}
        catch(IllegalArgumentException error){modulePreview.hide();}
    }
    private void placeModule(){
        var point=buildGround();if(point==null){outsidePlot();return;}
        try{var placed=activeModule.place(design,point);selected=placed.rootId();tool=Tool.SELECT;commit(placed.scene(),"Đã đặt cả cụm. Kéo khối gốc để dời; Ctrl+Z hoàn tác cả cụm.");}
        catch(IllegalArgumentException error){message=error.getMessage();rebuildHud();}
    }
    private static String shortName(String name,int limit){return name.length()<=limit?name:name.substring(0,limit-1)+"…";}
    private void drawBlueprints(int w,int h){
        int width=700,height=450,x=(w-width)/2,y=Math.max(126,(h-height)/2);hud.layer(3);hud.veil(w,h);hud.layer(4);
        hud.panel("Hộp bản thiết kế",skippedModules>0?"Đã bỏ qua "+skippedModules+" mẫu lỗi; các mẫu còn lại vẫn dùng được.":"Lưu cả thung lũng hoặc cất một công trình để dùng lại.",x,y,width,height);
        hud.layer(5);
        hud.button("blueprint:close","Đóng",x+width-90,y+height-45,72,30,false);
        hud.iconButton("save","Lưu thung lũng","blueprints",x+18,y+height-106,210,40,false);
        hud.iconButton("load","Mở thung lũng","blueprints",x+244,y+height-106,210,40,false);
        hud.iconButton("blueprint:save","Lưu cụm chọn","dockBuild",x+470,y+height-106,210,40,false);
        for(int i=0;i<6;i++){
            int index=blueprintPage*6+i;if(index>=savedModules.size())break;var entry=savedModules.get(index);
            int px=x+20+(i%3)*222,py=y+height-214-(i/3)*101;
            hud.blueprintCard("blueprint:pick:"+entry.id(),shortName(entry.name(),22),entry.module(),px,py,216,85,entry.id().equals(shelfSelected));
        }
        var presets=ArchitecturePresets.catalog();
        for(int i=0;i<presets.size();i++){
            var preset=presets.get(i);
            hud.button("blueprint:preset:"+i,preset.name(),x+20+i*168,y+104,160,29,false);
        }
        var entry=shelfEntry();
        if(entry!=null){
            hud.text(shortName(entry.name(),35)+" · "+entry.module().design().items().size()+" khối / cấu kiện",x+22,y+77,width-44,25,14);
            hud.button("blueprint:place","Đặt mẫu",x+22,y+46,150,36,false);
            hud.button("blueprint:rename","Đổi tên",x+184,y+46,150,36,false);
            hud.button("blueprint:delete",deleteModuleConfirm.equals(entry.id())?"Xác nhận xóa":"Xóa mẫu",x+346,y+46,150,36,false);
        }
        hud.button("blueprint:previous","<",x+530,y+46,40,36,false);
        hud.text((blueprintPage+1)+" / "+Math.max(1,(savedModules.size()+5)/6),x+577,y+52,60,25,13);
        hud.button("blueprint:next",">",x+640,y+46,40,36,false);hud.layer(1);
        hud.layer(5);hud.text(shortName(message,95),x+22,y+10,width-44,24,12);hud.layer(1);
    }
    private void drawBuildGuide(int w,int h){
        int x=w-350,y=h-284;guideBounds=new java.awt.Rectangle(x,y,330,176);hud.layer(4);hud.panel(buildGuide.title(),"Thực hành "+buildGuide.count()+" / 5 · Không thay cảnh của bạn",x,y,330,176);
        hud.layer(5);
        var lines=buildGuide.lines();for(int i=0;i<lines.size();i++)hud.text(lines.get(i),x+14,y+91-i*24,304,25,13);
        hud.button("guideHide",buildGuide.next()==null?"Hoàn tất":"Tạm ẩn",x+206,y+12,108,30,false);hud.layer(1);
    }

    private void captureClipboard(boolean cut) {
        var item=selectedItem();if(item==null){message="Chọn một công trình trước khi sao chép hoặc cắt.";return;}
        clipboardDesign=design;clipboardRoot=item.id();pasteCount=0;
        if(cut){selected=null;commit(design.removeAssembly(item.id()),"Đã cắt công trình. Ctrl+V để dán; Ctrl+Z để hoàn tác.");}
        else message="Đã sao chép công trình và các khối đi cùng. Ctrl+V để dán.";
    }
    private void pasteClipboard() {
        if(clipboardDesign==null || clipboardRoot==null){message="Chưa có công trình trong bộ nhớ sao chép.";return;}
        try {
            var pasted=AssemblyClipboard.paste(clipboardDesign,clipboardRoot,design,pasteCount);
            selected=pasted.selectedId();tool=Tool.SELECT;pasteCount++;
            commit(pasted.design(),"Đã dán công trình. Kéo dấu giữa khối để dời.");
        } catch(IllegalArgumentException error){message=error.getMessage();}
    }
    private void copySelected() {
        var item=design.items().stream().filter(candidate->Objects.equals(candidate.id(),selected)).findFirst().orElse(null);
        if(item==null) { message="Chọn công trình rồi bấm Nhân bản.";return; }
        try {
            var copied=AssemblyDuplicator.duplicate(design,item);
            commit(copied.design(),copied.message());selected=copied.selectedId();
        } catch(IllegalArgumentException error) { message=error.getMessage(); }
    }
    private void focusSelected() {
        if(walking)toggleWalk();
        var item=design.items().stream().filter(candidate->Objects.equals(candidate.id(),selected)).findFirst().orElse(null);
        if(item==null) {
            focus.set(design.land().centerX(),0.65f,design.land().centerZ());
            distance=Math.max(design.halfWidth(),design.halfDepth())*2.3f;
            for(int attempt=0;attempt<16;attempt++) {
                updateCamera();
                boolean fits=true;
                for(float x:new float[]{design.land().minX(),design.land().maxX()})
                    for(float z:new float[]{design.land().minZ(),design.land().maxZ()}) {
                        var point=cam.getScreenCoordinates(new Vector3f(x,0,z));
                        float right=inspectorVisible()?cam.getWidth()-245:cam.getWidth()-40;
                        if(point.x<40 || point.x>right || point.y<(toolsVisible?140:40)
                                || point.y>cam.getHeight()-155) fits=false;
                    }
                if(fits) break;
                distance*=1.12f;
            }
        } else {
            var placement=design.placement(item);
            focus.set(placement.center().x(),placement.elevation()+(item.kind().building()?item.height()*0.45f:0.5f),placement.center().z());
            distance=Math.max(10,Math.max(item.width(),item.depth())*2.5f);
        }
        updateCamera();
    }

    private void chooseFile(boolean save) {
        if(fileDialogOpen || confirmationOpen)return;
        cancel();
        stopCameraDrag();
        fileDialogOpen=true;
        var snapshot=design;
        SwingUtilities.invokeLater(() -> {
            try {
                var chooser = new JFileChooser(PlayerFiles.directory("saves").toFile());
                chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("DreamHouse Glade","glade"));
                chooser.setDialogTitle(save?"Lưu thung lũng":"Mở thung lũng");
                int result=save?chooser.showSaveDialog(null):chooser.showOpenDialog(null);
                if(result!=JFileChooser.APPROVE_OPTION)return;
                var file=chooser.getSelectedFile().toPath();
                if(save) {
                    if(!file.toString().toLowerCase(Locale.ROOT).endsWith(".glade")) file=Path.of(file+".glade");
                    if(Files.exists(file) && JOptionPane.showConfirmDialog(null,"Ghi đè bản thiết kế này?",
                            "Lưu thung lũng",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
                    snapshot.save(file);
                    var savedFile=file;
                    enqueue(() -> acceptWorldSave(snapshot,savedFile));
                } else {
                    var restored=GladeDesign.load(file);
                    var loadedFile=file;
                    enqueue(() -> {replaceWorld(restored,loadedFile);mainMenuOpen=false;settingsOpen=false;message="Đã mở thung lũng.";rebuildHud();});
                }
            } catch(Exception error) {
                PlayerFiles.error("Không xử lý được bản thiết kế",error);
                JOptionPane.showMessageDialog(null,
                        save ? "Lưu thất bại. Hãy thử tên tệp hoặc thư mục khác."
                             : "Không mở được tệp. Cảnh hiện tại vẫn được giữ.",
                        "Bản thiết kế DreamHouse", JOptionPane.ERROR_MESSAGE);
            }finally{enqueue(()->fileDialogOpen=false);}
        });
    }

    private void acceptWorldSave(GladeDesign snapshot,Path file){
        currentFile=file;savedDesign=snapshot;buildGuide.mark(BuildGuide.Goal.SAVE);queueAutosave();message="Đã lưu thung lũng.";rebuildHud();
    }

    private boolean inspectorVisible() { return false; }

    private String activeGroup() {
        if(tool==Tool.TERRAIN || tool.terrainBrush() || tool==Tool.LAND)return "terrain";
        if(tool==Tool.WATER)return "water";
        if(tool==Tool.PAINT)return "palette";
        if(tool==Tool.ERASE)return "tool:ERASE";
        if(tool==Tool.PATH)return "landscape";
        if(tool==Tool.TREE)return "trees";
        if(tool==Tool.FLOWERS || tool==Tool.PLANTER || tool==Tool.BENCH || tool==Tool.PERGOLA || tool==Tool.WELL)return "garden";
        if(tool.kind!=null && tool.kind.mounted())return "windows";
        if(tool.kind!=null)return "architecture";
        return "";
    }

    private String catalogIcon(GladeCatalog.Entry entry) {
        if(entry.tool().equals("HOUSE")) {
            if(entry.variant()==7)return "DECK";
            if(entry.variant()==8)return "CRAFT_TABLE";
            if(entry.variant()==9)return "PERGOLA";
        }
        return entry.tool()+"@"+entry.variant();
    }

    private void drawLibrary(int width,int height) {
        var entries=GladeCatalog.entries(libraryGroup);int columns=4,rows=(entries.size()+3)/4;
        int cw=Math.min(142,(width-100)/4),ch=Math.min(104,(height-180)/Math.max(1,rows));
        int pw=columns*(cw+10)+30,ph=rows*(ch+8)+152,x=(width-pw)/2,y=(height-ph)/2;
        hud.layer(7);hud.veil(width,height);hud.layer(8);
        hud.panel("THƯ VIỆN MẪU","Bấm một mẫu để dựng · Esc để đóng",x,y,pw,ph);
        String[] groups={"architecture","roofs","windows","trees","garden","landscape","terrain","water"};
        String[] names={"Nhà","Mái","Cửa","Cây","Hoa","Đường","Đất","Nước"};int tab=(pw-24)/groups.length;
        for(int i=0;i<groups.length;i++)hud.button("libraryGroup:"+groups[i],names[i],x+12+i*tab,y+ph-91,tab-4,29,libraryGroup.equals(groups[i]));
        for(int i=0;i<entries.size();i++){var entry=entries.get(i);hud.toolCard("libraryPick:"+i,entry.label(),catalogIcon(entry),x+16+i%4*(cw+10),y+12+(rows-1-i/4)*(ch+8),cw,ch,false);}
        hud.button("libraryClose","Đóng",x+pw-88,y+ph-44,74,28,false);hud.layer(1);
    }

    private boolean strokeBrush(){return tool==Tool.PATH || tool==Tool.WATER;}
    private boolean hasStrokePanel(){
        return toolsVisible && (strokeBrush() || tool==Tool.PAINT && selectedItem()!=null && selectedItem().kind()==GladeDesign.Kind.PATH);
    }
    private float strokeSize(){return tool==Tool.WATER?waterRadius:pathWidth;}
    private float minStrokeSize(){return tool==Tool.WATER?.35f:.50f;}
    private float strokeFraction(){return (strokeSize()-minStrokeSize())/(4-minStrokeSize());}
    private void setStrokeSize(float value){
        float size=FastMath.clamp(Math.round(value*20)/20f,minStrokeSize(),4);
        if(size==strokeSize())return;
        if(tool==Tool.WATER)waterRadius=size;else pathWidth=size;
        hud.sliderValue("brushWidth",strokeFraction());
    }
    private void dragBrush(float x){
        var bounds=hud.bounds("brushWidth");if(bounds==null)return;
        float fraction=FastMath.clamp((x-bounds.x-12)/(bounds.width-24),0,1);
        setStrokeSize(FastMath.interpolateLinear(fraction,minStrokeSize(),4));
    }
    private void strokePanel(int width,boolean editing,GladeDesign.Finish finish){
        int w=404,x=(width-w)/2,y=124;
        boolean water=tool==Tool.WATER;
        int h=water?46:120;
        hud.layer(2);
        hud.panel("","",x,y,w,h);hud.block("brushPanel",x,y,w,h);
        if(!editing){
            hud.text("Độ rộng",x+12,y+(water?9:82),74,27,13);
            hud.slider("brushWidth",x+91,y+(water?7:81),w-106,strokeFraction());
        } else hud.text("Bề mặt đường",x+12,y+85,170,26,14);
        if(!water){
            String materialPrefix=editing?"paintMaterial:":"pathMaterial:",colorPrefix=editing?"paintColor:":"pathColor:";
            var materials=new ArrayList<>(PathSurface.MATERIALS);
            if(editing)materials.addAll(List.of("stone","wood"));
            int cell=(w-22)/materials.size();
            for(int i=0;i<materials.size();i++){
                String material=materials.get(i);
                hud.button(materialPrefix+material,materialName(material),x+12+i*cell,y+43,cell-5,32,material.equals(finish.material()));
            }
            for(int i=0;i<PathSurface.COLORS.size();i++){
                String color=PathSurface.COLORS.get(i);
                hud.circleChip(colorPrefix+color,color,"","",x+69+i*37,y+7,29,color.equals(finish.color()));
            }
            hud.text("Màu",x+12,y+9,52,24,13);
            hud.circleChip(colorPrefix+"reset","#f6f0d8","","↺",x+300,y+7,29,finish.color().isEmpty());
            boolean custom=!finish.color().isEmpty() && !PathSurface.COLORS.contains(finish.color());
            hud.button(editing?"paintPathCustomColor":"pathCustomColor","Tự chọn",x+336,y+7,58,29,custom);
        }
        hud.layer(1);
    }

    private void paintPalette(int w,int h) {
        var item=selectedItem();if(item==null || !paintable(item.kind()))return;
        if(item.kind()==GladeDesign.Kind.PATH){if(toolsVisible)strokePanel(w,true,item.form().finish());return;}
        var placement=design.placement(item);
        var center=cam.getScreenCoordinates(new Vector3f(placement.center().x(),placement.elevation()+item.height()*.5f,placement.center().z()));
        int cx=(int)FastMath.clamp(center.x,250,w-245),cy=(int)FastMath.clamp(center.y,330,Math.max(330,h-205));
        hud.layer(4);
        boolean roofAvailable=item.kind()==GladeDesign.Kind.WELL || item.kind().building() && (visibleRoofRise(item,design)>.02f);
        if(!roofAvailable)paintTarget="wall";
        String bodyLabel=item.kind()==GladeDesign.Kind.PATH?"Đường":item.kind()==GladeDesign.Kind.FENCE?"Rào":item.kind()==GladeDesign.Kind.BRIDGE?"Cầu":item.kind().building() || item.kind().name().contains("WALL")?"Tường":"Khối";
        var finish=item.form().finish();boolean roof=paintTarget.equals("roof");
        hud.circleChip("paintTarget:wall","#ddc8a4","brick",bodyLabel,cx-230,cy-75,52,!roof);
        if(roofAvailable)hud.circleChip("paintTarget:roof","#aebabe","tile","Mái",cx-220,cy-145,48,roof);
        String[] materials=roof?new String[]{"original","tile","slate","wood","metal"}:new String[]{"original","plaster","brick","stone","wood"};
        String[] tints=roof?new String[]{"#e6dcc1","#c68562","#879ba4","#bfa077","#a8b5b5"}:new String[]{"#e6dcc1","#e9dfbf","#c88b72","#a6aea2","#bfa077"};
        for(int i=0;i<materials.length;i++) {
            double angle=Math.toRadians(208+i*30);int x=cx+(int)(163*Math.cos(angle)),y=cy+(int)(152*Math.sin(angle));
            hud.circleChip("paintMaterial:"+materials[i],tints[i],materials[i],i==0?"Gốc":"",x-22,y-22,44,materials[i].equals(roof?finish.roofMaterial():finish.material()));
        }
        String[] colors={"#f4e6c6","#c9ae73","#c27f63","#ae7783","#9e84af","#7298b8","#86aaa0","#99ad77","#d4ac6e","#a4a398","#6b7777","#4f5663"};
        for(int i=0;i<colors.length;i++) {
            double angle=Math.toRadians(-73+i*13.2);int x=cx+(int)(212*Math.cos(angle)),y=cy+(int)(212*Math.sin(angle));
            hud.circleChip("paintColor:"+colors[i],colors[i],"","",x-17,y-17,35,colors[i].equals(roof?finish.roofColor():finish.color()));
        }
        hud.circleChip("paintColor:reset","#f6f0d8","","↺",cx+55,cy-204,35,(roof?finish.roofColor():finish.color()).isEmpty());
        hud.text((roof?"MÁI":bodyLabel.toUpperCase(Locale.ROOT))+" · Vật liệu & màu",cx-130,cy-201,220,26,12);
        if(item.kind().roofHost() || item.stonePlatform()){
            var attached=design.attachedRoof(item.id());
            hud.button("paintStyle:east","Trùng Khánh",cx-142,cy+62,104,32,design.hillside(item) || attached!=null && attached.kind()==GladeDesign.Kind.ROOF_CURVED);
            hud.button("paintStyle:west","Vieille Ville",cx-29,cy+62,116,32,false);
        }
        hud.layer(1);
    }

    private void updateHint() {
        if(hud==null)return;
        if(photoMode){contextHints.detachAllChildren();lastHint="";return;}
        String hover=hud.hit(inputManager.getCursorPosition().x,inputManager.getCursorPosition().y);
        String hint;
        if(cam.getWidth()<720 || cam.getHeight()<540 || libraryOpen || blueprintsOpen || helpOpen || mainMenuOpen || settingsOpen || confirmationOpen){
            contextHints.detachAllChildren();lastHint="";return;
        }
        if(hover!=null && hover.startsWith("paintMaterial:")) hint="Vật liệu: "+materialName(hover.substring(14))+" · Bấm để áp dụng";
        else if(hover!=null && hover.startsWith("paintColor:")) hint="Bấm màu để tô "+(paintTarget.equals("roof")?"mái":"bề mặt")+" · Ctrl+Z để hoàn tác";
        else if(activeModule!=null)hint="Rê để xem khung mẫu · Bấm trái đặt cả cụm · Esc hủy · Giữ phải xoay cảnh";
        else if(walking) hint="Đi bộ: WASD / mũi tên · Giữ phải để nhìn quanh · Esc: trở lại xây dựng";
        else if(rotating) hint="Kéo để xoay cảnh · Thả chuột để dừng";
        else if(resizeAxis!=null) hint=switch(resizeAxis) {case ROTATE->"Rê quanh khối để xoay · Thả để chốt";case ROOF->"Kéo lên tạo mái · Kéo xuống hết để bỏ mái";case ROOF_LEFT,ROOF_RIGHT->"Kéo ngang để co giãn riêng mái";case BASE->"Kéo lên nâng chân trụ · Kéo xuống để hạ";case BOTTOM->"Kéo điểm dưới để đổi cốt đáy · Chạm điểm trên sẽ thành tấm phẳng";case ELEVATION->"Kéo điểm giữa để nâng / hạ cả vật thể";default->"Kéo để đổi kích thước · Esc để hủy";};
        else if(tool==Tool.PAINT) hint=selectedItem()==null?"Bấm đường, công trình hoặc đồ trang trí để đổi vật liệu":"Bấm vật liệu hoặc màu để áp dụng · Esc: chọn khối";
        else if(tool==Tool.TERRAIN) hint="Giữ trái: đắp đất · Giữ phải: hạ đất · Kéo nút trắng ở mép để nới đất";
        else if(tool.terrainBrush()) hint="Giữ chuột trái và rê để "+tool.label.toLowerCase(Locale.ROOT)+" · [ / ]: cỡ bút";
        else if(tool==Tool.WATER) hint="Bấm để đổ nước · Giữ và rê để vẽ dòng · Kéo thanh xanh để đổi độ rộng";
        else if(tool==Tool.PATH) hint="Giữ trái và rê để vẽ đường · Kéo thanh xanh để đổi độ rộng";
        else if(tool==Tool.TREE || tool==Tool.FLOWERS)hint=stroke.size()>=128?"Thả chuột để chốt nét và tiếp tục tô":"Bấm hoặc giữ rê để tô cây / hoa · Ctrl+Z hoàn tác cả nét";
        else if(tool==Tool.LAND) hint="Kéo nút trắng ở mép đất để mở rộng hoặc thu đất";
        else if(lastSnap!=null && !lastSnap.lines().isEmpty() && (drawing || previewStructure()))hint="Đã khớp mép / tâm · Giữ Alt để đặt tự do · Esc: hủy";
        else if(moving!=null) hint="Kéo để dời khối · Alt: đặt tự do · Esc: hủy";
        else if(tool==Tool.SELECT) hint=selected==null?"WASD: di chuyển · Giữ phải: xoay · Giữ giữa: kéo cảnh":"Kéo trái khối đã chọn để dời · Kéo chuột phải để xoay cảnh";
        else hint="Bấm hoặc kéo để dựng "+tool.label.toLowerCase(Locale.ROOT)+" · Esc: chọn / xoay cảnh";
        int bottom=hasStrokePanel()?258:toolsVisible?127:24;
        String key=bottom+":"+cam.getWidth()+":"+hint;
        if(Objects.equals(lastHint,key) && contextHints.getQuantity()>0)return;
        lastHint=key;contextHints.detachAllChildren();
        var tip=new GladeHud(assetManager);tip.layer(6);tip.tooltip(hint,20,bottom,Math.min(cam.getWidth()-40,560));
        contextHints.attachChild(tip.node());
    }
    private void updateFeedback(float dt){
        if(!Objects.equals(noticedMessage,message)){noticedMessage=message;noticeTime=5;}
        else noticeTime=Math.max(0,noticeTime-dt);
        boolean hidden=noticeTime<=0 || helpOpen || blueprintsOpen || libraryOpen || mainMenuOpen || settingsOpen || confirmationOpen || fileDialogOpen || photoMode || cam.getWidth()<720 || cam.getHeight()<540;
        feedbackToast.setCullHint(hidden?Spatial.CullHint.Always:Spatial.CullHint.Inherit);if(hidden)return;
        int width=Math.min(cam.getWidth()-40,680);int bottom=hasStrokePanel()?307:toolsVisible?182:24;String key=width+":"+bottom+":"+message;
        if(!key.equals(noticeKey)){noticeKey=key;feedbackToast.detachAllChildren();var notification=new GladeHud(assetManager);notification.layer(6);notification.notice(message,20,bottom,width);feedbackToast.attachChild(notification.node());}
    }
    private String materialName(String value) {return switch(value){case "original"->"Ban đầu";case "plaster"->"Vữa";case "brick"->"Gạch";case "stone"->"Đá";case "wood"->"Gỗ";case "tile"->"Ngói";case "slate"->"Đá lợp";case "metal"->"Kim loại";case "dirt"->"Đất";case "concrete"->"Xi măng";case "gravel"->"Sỏi";default->value;};}

    private void rebuildHud() {
        if(hud==null) return;
        guideBounds=null;
        int w=cam.getWidth(),h=cam.getHeight();
        if(w<720 || h<540){knownWidth=w;knownHeight=h;hud.clear();hud.panel("Cửa sổ quá nhỏ","Phóng to cửa sổ để tiếp tục chơi",8,8,Math.max(200,w-16),110);return;}
        knownWidth=w;knownHeight=h;
        hud.clear();
        if(photoMode){
            hud.iconButton("photoTake","Chụp ảnh","photo",w-172,18,154,46,false);
            hud.button("photoBack","Thoát chụp ảnh",18,22,160,36,false);
            hud.text("Giữ phải để xoay · Lăn chuột để thu phóng · Esc để thoát",18,65,440,28,13);
            return;
        }
        hud.brand(18,h-80);
        hud.launcher("tools","Hộp dụng cụ","tools",18,h-180,toolsVisible);
        hud.launcher("blueprints","Hộp bản thiết kế","blueprints",122,h-180,blueprintsOpen);
        hud.stat("DIỆN TÍCH",String.format(Locale.ROOT,"%.1f × %.1f",design.land().width(),design.land().depth()),"LAND",w-330,h-65);
        hud.stat("CÔNG TRÌNH",design.items().size()+" khối","HOUSE",w-168,h-65);
        var chosen=selectedItem();
        if(tool==Tool.SELECT && chosen!=null && !sceneMenuOpen && !libraryOpen && !blueprintsOpen && !helpOpen && !mainMenuOpen && !settingsOpen){
            String label=Arrays.stream(Tool.values()).filter(candidate->candidate.kind==chosen.kind())
                    .findFirst().map(candidate->candidate.label).orElse(chosen.kind().name());
            hud.text("Đang chọn · "+label+" #"+chosen.id(),w-300,h-96,280,27,13);
        }
        flyoutBounds=null;
        if(toolsVisible) {
            String[] groups={"architecture","landscape","windows","palette","tool:ERASE","terrain","garden","trees","water","photo","sceneLight"};
            String[] names={"Xây dựng","Con đường","Cửa sổ","Bảng màu","Tẩy","Địa hình","Hoa","Cây cối","Nước","Máy ảnh","Thời gian"};
            String[] icons={"dockBuild","dockPath","dockWindow","palette","ERASE","RAISE","dockFlower","TREE","dockWater","photo","light"};
            String[] colors={"#dc9b68","#da9b85","#e3c467","#e9d6ae","#dca184","#b6bc8f","#b4c57c","#94ad76","#8bb7c0","#9ba8a8","#b4d0df"};
            int tw=Math.min(94,(w-36)/groups.length-6),left=(w-(tw+6)*groups.length+6)/2;
            for(int i=0;i<groups.length;i++) hud.categoryTile(groups[i],names[i],icons[i],colors[i],left+i*(tw+6),16,tw,100,
                    groups[i].equals(activeGroup()) || groups[i].equals(flyoutGroup));
            if(houseFlyout && !flyoutGroup.isEmpty()) {
                hud.layer(4);
                var anchor=hud.bounds(flyoutGroup);
                boolean narrow=h<650;int columns=narrow?6:3,cw=narrow?76:110,ch=narrow?78:86;
                var available=categoryTools();int count=Math.min(6,available.length),rows=(count+columns-1)/columns;
                int pw=columns*(cw+8)+16,ph=rows*(ch+8)+54;
                int px=Math.max(18,Math.min(w-pw-18,anchor==null?left:anchor.x)),py=172;
                flyoutBounds=new java.awt.Rectangle(px,py,pw,ph);
                hud.panel("","",px,py,pw,ph);
                hud.text(flyoutGroup.equals("architecture")?"Khối cơ bản":flyoutGroup.equals("landscape")?"Đường đi":"Chọn mẫu",px+12,py+ph-35,pw-24,28,14);
                if(flyoutGroup.equals("architecture")){
                    hud.button("architectureLibrary","Thư viện",px+pw-146,py+ph-36,80,27,false);
                    hud.button("roofs","Mái",px+pw-59,py+ph-36,47,27,false);
                }
                for(int i=0;i<count;i++) {var option=available[i];hud.toolCard("house:"+option.name(),option.label,option.name(),
                    px+8+i%columns*(cw+8),py+8+(rows-1-i/columns)*(ch+8),cw,ch,tool==option);}
                hud.layer(1);
            }
        }
        hud.iconButton("walk",walking?"Về xây dựng":"Đi bộ","walk",w-154,toolsVisible?132:24,136,46,walking);
        if(toolsVisible && strokeBrush() && !walking && !helpOpen && !sceneMenuOpen && !blueprintsOpen && !libraryOpen && !mainMenuOpen && !settingsOpen)
            strokePanel(w,false,pathFinish);
        if(tool==Tool.PAINT && selectedItem()!=null && !walking && !helpOpen && !sceneMenuOpen && !blueprintsOpen && !libraryOpen && !mainMenuOpen && !settingsOpen)
            paintPalette(w,h);
        hud.iconButton("mainMenu","Menu","help",18,h-280,118,42,mainMenuOpen);
        hud.menuButton(18,h-230,sceneMenuOpen);
        if(design.items().isEmpty() && !mainMenuOpen && !settingsOpen)hud.iconButton("sample","Vườn mơ ước","dockFlower",18,h-340,174,48,false);
        if(sceneMenuOpen) {
            hud.layer(4);
            int py=h-512;
            hud.panel("","",82,py,318,310);
            String[] ids={"sample","village","clear","viewAll","help","cameraSave","cameraRecall","hillsideEast","hillsideWest"};
            String[] labels={"Cảnh mẫu","Làng ven suối","Đất trống","Toàn cảnh","Hướng dẫn","Lưu góc nhìn","Về góc đã lưu","Trùng Khánh","Vieille Ville"};
            String[] icons={"dockBuild","dockPath","LAND","viewAll","help","photo","viewAll","ROOF_CURVED","HOUSE"};
            String[] tints={"#dbac82","#9cc2ab","#b2c590","#9cbfc0","#e3cd91","#dfbd94","#9cbfc0","#cbb189","#dcaa86"};
            for(int i=0;i<ids.length;i++) hud.categoryTile(ids[i],labels[i],icons[i],tints[i],92+i%3*102,py+6+(2-i/3)*99,96,96,false);
            hud.layer(1);
        }
        if(blueprintsOpen)drawBlueprints(w,h);
        if(libraryOpen) drawLibrary(w,h);
        if(helpOpen) {
            int helpWidth=560,helpHeight=360;
            int helpX=(w-helpWidth)/2,helpY=(h-helpHeight)/2;
            hud.helpPanel(helpX,helpY,helpWidth,helpHeight);
            hud.layer(6);
            hud.button("helpClose","Đóng",helpX+helpWidth-112,helpY+18,88,36,false);
            hud.button("helpStart","Học qua thao tác",helpX+24,helpY+18,190,36,false);
            hud.layer(1);
        }
        if(buildGuide.active && !(tool==Tool.PAINT && selected!=null) && !houseFlyout && !helpOpen && !blueprintsOpen && !libraryOpen && !mainMenuOpen && !settingsOpen)drawBuildGuide(w,h);
        if(mainMenuOpen || settingsOpen)drawMainMenu(w,h);
        refreshHandles();
    }

    private boolean unsaved(){return managedSession && design!=savedDesign;}
    private void guardDiscard(Runnable next){
        if(confirmationOpen || fileDialogOpen)return;
        if(!unsaved()){next.run();return;}
        confirmationOpen=true;stopCameraDrag();
        SwingUtilities.invokeLater(()->{
            int answer=JOptionPane.showOptionDialog(null,"Bạn có thay đổi chưa lưu. Tiếp tục sẽ bỏ phiên hiện tại. Chỉ bản phục hồi đã ghi thành công mới có thể mở lại.","Thay đổi chưa lưu",JOptionPane.DEFAULT_OPTION,JOptionPane.WARNING_MESSAGE,null,new String[]{"Giữ lại","Tiếp tục"},"Giữ lại");
            enqueue(()->{confirmationOpen=false;if(answer==1){queueAutosave();next.run();}});
        });
    }
    private void replaceWorld(GladeDesign restored,Path file){
        cancel();stopCameraDrag();if(walking)toggleWalk();
        design=restored;savedDesign=file==null?null:restored;currentFile=file;autosavedDesign=null;autosaveClock=0;recovery=null;selected=null;tool=Tool.SELECT;designHistory.clear();rebuild();rebuildHud();
    }
    private void queueAutosave(){
        if(sessionStore==null)return;var snapshot=design;long request=++autosaveRequest;autosavePending=true;autosaveClock=0;autosaveInterval=0;
        sessionStore.save(snapshot).whenComplete((file,error)->enqueue(()->{
            if(request!=autosaveRequest)return;autosavePending=false;
            if(error==null)autosavedDesign=snapshot;
            else if(!(error instanceof java.util.concurrent.CancellationException)){PlayerFiles.error("Tự lưu thất bại",error);message="Tự lưu chưa thành công. Hãy dùng Lưu thiết kế.";}
        }));
    }
    private void updateSession(float tpf){
        if(!managedSession || sessionStore==null)return;autosaveClock+=tpf;autosaveInterval+=tpf;
        if(!autosavePending && unsaved() && design!=autosavedDesign && (autosaveClock>3 && !drawing || autosaveInterval>30))queueAutosave();
    }
    @Override public void requestClose(boolean esc){if(managedSession){if(pendingWater!=null)cancel();guardDiscard(()->{if(unsaved())queueAutosave();stop();});}else super.requestClose(esc);}
    @Override public void destroy(){waterWorker.shutdownNow();if(sounds!=null)sounds.stop();if(sessionStore!=null)sessionStore.close();super.destroy();}
    @Override public void handleError(String message,Throwable error){if(!launchReady)LaunchSignals.publish("FAILED");PlayerFiles.error(message,error);if(sessionStore!=null && design!=null)sessionStore.save(design);super.handleError(message,error);}
    private void drawMainMenu(int w,int h){
        hud.layer(9);hud.veil(w,h);hud.layer(10);int pw=Math.min(480,w-36),ph=Math.min(480,h-30),x=(w-pw)/2,y=(h-ph)/2;
        String subtitle=settingsOpen?"Thay đổi được lưu trên máy của bạn":startupError!=null?startupError:"Xây thung lũng theo cách của bạn";
        hud.panel(settingsOpen?"CÀI ĐẶT":"DREAMHOUSE",subtitle,x,y,pw,ph);
        String[] ids,labels;
        if(settingsOpen){ids=new String[]{"settings:quality","settings:volume","settings:ambient","settings:sensitivity","settings:resolution","settings:fullscreen","menu:back"};labels=new String[]{"Đồ họa: "+(qualityLow?"Nhẹ":"Đẹp"),"Âm lượng: "+Math.round(volume*100)+"%","Âm thiên nhiên: "+(ambientEnabled?"Bật":"Tắt"),"Độ nhạy camera: "+String.format(Locale.ROOT,"%.1f",cameraSensitivity),"Cửa sổ: "+cam.getWidth()+" × "+cam.getHeight(),fullscreen?"Về cửa sổ":"Toàn màn hình","Trở lại chơi"};}
        else{ids=new String[]{"menu:continue","menu:new","menu:load","menu:save","menu:settings","menu:help","menu:exit"};labels=new String[]{recovery!=null?"Phục hồi phiên gần nhất":"Tiếp tục chơi","Thế giới mới","Mở bản thiết kế","Lưu thiết kế"+(unsaved()?" *":""),"Cài đặt","Hướng dẫn chơi","Thoát"};}
        int bh=42,gap=9;for(int i=0;i<ids.length;i++)hud.button(ids[i],labels[i],x+28,y+ph-105-i*(bh+gap),pw-56,bh,false);
        hud.layer(1);
    }
    private void applyQuality(){
        if(shadow==null || effects==null)return;
        if(qualityLow){viewPort.removeProcessor(shadow);viewPort.removeProcessor(effects);}
        else{if(!viewPort.getProcessors().contains(shadow))viewPort.addProcessor(shadow);if(!viewPort.getProcessors().contains(effects))viewPort.addProcessor(effects);}
    }
    private void setting(String name){
        var p=PlayerFiles.settings();
        switch(name){
            case "quality" -> {qualityLow=!qualityLow;applyQuality();}
            case "volume" -> {volume=volume>=.99f?0:Math.min(1,volume+.25f);if(sounds!=null){sounds.setVolume(volume);sounds.ambient(ambientEnabled);}else message="Mở lại game để bật thiết bị âm thanh.";}
            case "ambient" -> {ambientEnabled=!ambientEnabled;if(sounds!=null)sounds.ambient(ambientEnabled);}
            case "sensitivity" -> cameraSensitivity=cameraSensitivity>=1.9f?.5f:cameraSensitivity+.5f;
            case "resolution" -> {
                if(context instanceof com.jme3.system.lwjgl.LwjglWindow win){
                    p.setProperty("resolution",GladeLauncher.nextResolution(p.getProperty("resolution",cam.getWidth()+" × "+cam.getHeight())));
                    var size=GladeLauncher.resolution(p);
                    if(fullscreen)org.lwjgl.glfw.GLFW.glfwSetWindowMonitor(win.getWindowHandle(),0,40,40,size.width,size.height,0);
                    else org.lwjgl.glfw.GLFW.glfwSetWindowSize(win.getWindowHandle(),size.width,size.height);
                    fullscreen=false;
                }
            }
            case "fullscreen" -> {
                if(context instanceof com.jme3.system.lwjgl.LwjglWindow win){
                    if(fullscreen){
                        var size=GladeLauncher.resolution(p);
                        org.lwjgl.glfw.GLFW.glfwSetWindowMonitor(win.getWindowHandle(),0,40,40,size.width,size.height,0);
                        fullscreen=false;
                    }else{
                        long monitor=org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor();
                        var mode=org.lwjgl.glfw.GLFW.glfwGetVideoMode(monitor);
                        if(mode!=null){
                            org.lwjgl.glfw.GLFW.glfwSetWindowMonitor(win.getWindowHandle(),monitor,0,0,mode.width(),mode.height(),mode.refreshRate());
                            fullscreen=true;
                        }
                    }
                }
            }
        }
        p.setProperty("volume",Float.toString(volume));p.setProperty("ambient",Boolean.toString(ambientEnabled));p.setProperty("cameraSensitivity",Float.toString(cameraSensitivity));p.setProperty("lowQuality",Boolean.toString(qualityLow));
        try{PlayerFiles.saveSettings(p);}catch(Exception e){PlayerFiles.error("Không lưu được cài đặt",e);}rebuildHud();
    }

    private void updateCamera() {
        if(cam.getWidth()<1 || cam.getHeight()<1) return;
        cam.setFrustumPerspective(38,(float)cam.getWidth()/cam.getHeight(),0.1f,3000);
        if(walking){
            cam.setLocation(walkPosition);
            cam.lookAtDirection(new Vector3f(-FastMath.sin(yaw)*FastMath.cos(elevation),-FastMath.sin(elevation),-FastMath.cos(yaw)*FastMath.cos(elevation)),Vector3f.UNIT_Y);
            return;
        }
        cam.setLocation(focus.add(CameraController.orbitOffset(yaw,elevation,distance)));
        cam.lookAt(focus,Vector3f.UNIT_Y);updateHandleScales();
    }
    private void updateHandleScales(){
        if(cam==null || cam.getHeight()<1 || objectHandles.getQuantity()==0)return;
        objectHandles.updateGeometricState();
        objectHandles.depthFirstTraversal(part->{if(Boolean.TRUE.equals(part.getUserData("scaledGrip"))){float unit=2*cam.getLocation().distance(part.getWorldTranslation())*FastMath.tan(19*FastMath.DEG_TO_RAD)/cam.getHeight();part.setLocalScale(FastMath.clamp(6*unit/.17f,.08f,8));}});
    }

    private void toggleWalk(){
        stopCameraDrag();cancel();cameraKeys.clear();cameraVelocity.set(0,0,0);
        if(!walking){
            orbitDistance=distance;orbitElevation=elevation;
            content.updateGeometricState();
            walkController=new WalkController(design,content);
            walkController.spawn(walkPosition,focus.x+FastMath.sin(yaw)*3,focus.z+FastMath.cos(yaw)*3);
            elevation=.04f;
            walking=true;tool=Tool.SELECT;selected=null;houseFlyout=false;
        }else{walking=false;walkController=null;focus.set(walkPosition.x,.65f,walkPosition.z);distance=orbitDistance;elevation=orbitElevation;}
        updateCamera();rebuildHud();
    }
    private void boundCamera(Vector3f target){
        CameraController.constrain(target,design.land());
    }
    private void moveCamera(float tpf){
        float dt=Math.min(tpf,.05f);boolean blocked=control || drawing || pendingLeft || helpOpen || libraryOpen || blueprintsOpen || sceneMenuOpen || mainMenuOpen || settingsOpen || confirmationOpen || fileDialogOpen;
        var forward=cam.getDirection().clone();forward.y=0;forward.normalizeLocal();var right=cam.getLeft().negate();right.y=0;right.normalizeLocal();
        var direction=new Vector3f();
        if(!blocked){if(cameraKeys.contains("cameraForward"))direction.addLocal(forward);if(cameraKeys.contains("cameraBack"))direction.subtractLocal(forward);if(cameraKeys.contains("cameraRight"))direction.addLocal(right);if(cameraKeys.contains("cameraLeft"))direction.subtractLocal(right);}
        float speed=walking?3.2f:FastMath.clamp(distance*.28f,2,35);
        if(direction.lengthSquared()>0)direction.normalizeLocal().multLocal(speed);
        if(blocked)cameraVelocity.set(0,0,0);else cameraVelocity.interpolateLocal(direction,1-(float)Math.exp(-dt*12));
        if(cameraVelocity.lengthSquared()<.0001f){cameraVelocity.set(0,0,0);return;}
        var shift=cameraVelocity.mult(dt);
        if(walking)walkController.move(walkPosition,shift);
        else{focus.addLocal(shift);boundCamera(focus);}
        updateCamera();
    }

    private boolean previewStructure(){return Set.of(Tool.HOUSE,Tool.TOWER,Tool.PENTAGON,Tool.DECK,Tool.STAIRS_STONE).contains(tool);}
    private void idleGhost() {
        if(activeModule!=null){updateModulePreview();return;}
        if(buildGhost==null)return;
        if(walking){buildGhost.hide();if(snapGuide!=null)snapGuide.hide();return;}
        if(photoMode || !previewStructure() || libraryOpen || sceneMenuOpen || blueprintsOpen || helpOpen || rotating || panning || mainMenuOpen || settingsOpen){buildGhost.hide();if(!drawing && snapGuide!=null)snapGuide.hide();return;}
        if(drawing)return;
        var cursor=pointer();if(cursor.y<124 || cursor.y>cam.getHeight()-100 || hud.hit(cursor.x,cursor.y)!=null){buildGhost.hide();if(snapGuide!=null)snapGuide.hide();return;}
        var support=stackMode?stackTarget():null;
        var point=support==null?buildGround():supportPoint(support);
        boolean outside=point==null;
        if(outside){
            point=rawGround();
            if(point==null){buildGhost.hide();if(snapGuide!=null)snapGuide.hide();return;}
        }
        var size=defaultSize(tool);
        if(!outside)point=new GladeDesign.Point(FastMath.clamp(point.x(),design.land().minX()+size[0]/2,design.land().maxX()-size[0]/2),FastMath.clamp(point.z(),design.land().minZ()+size[1]/2,design.land().maxZ()-size[1]/2));
        float roof=tool.kind.building()?size[0]*(tool==Tool.TOWER?.60f:.32f):0;
        var item=new GladeDesign.Item(design.nextId(),tool.kind,point,size[0],size[1],houseHeight,roof>0,List.of(),0,null,new GladeDesign.Form(0,roof,0,support==null?-1:support.id()).finish(activeFinish));
        if(outside){
            if(snapGuide!=null)snapGuide.hide();
            buildGhost.show(item,design,BuildGhost.State.BLOCKED);
        }else{
            var snapped=snapPlacement(item);
            buildGhost.show(snapped,design,ghostState());
        }
    }

    private BuildGhost.State ghostState(){
        return lastSnap!=null && lastSnap.neighbor()>=0?BuildGhost.State.SNAPPED:BuildGhost.State.READY;
    }

    @Override public void simpleUpdate(float tpf) {
        finishWater();
        if(!launchReady && elapsed>.12f){launchReady=true;launchReadyAt=elapsed;LaunchSignals.publish("READY");}
        if(atmosphere!=null)atmosphere.update(tpf,distance);
        updateSession(tpf);
        moveCamera(tpf);
        if(backdrop!=null){backdrop.follow(cam.getLocation());backdrop.syncColor(viewPort.getBackgroundColor());}
        elapsed+=tpf;ghostClock+=tpf;if(ghostClock>.05f){ghostClock=0;idleGhost();}
        if(adjustingBrush)dragBrush(inputManager.getCursorPosition().x);
        updateLeftGesture();
        updateHint();
        updateFeedback(tpf);
        if(hud!=null) hud.update(tpf,inputManager.getCursorPosition().x,inputManager.getCursorPosition().y);
        if(hud!=null && toolsVisible && !adjustingBrush && !drawing && !rotating && !pendingLeft && !helpOpen && !sceneMenuOpen && !blueprintsOpen && !libraryOpen && !mainMenuOpen && !settingsOpen && !confirmationOpen) {
            var cursor=pointer();String hovered=null;
            for(String group:List.of("architecture","windows","garden")) {
                var bounds=hud.bounds(group);if(bounds!=null && bounds.contains(cursor.x,cursor.y)) hovered=group;
            }
            if(hovered!=null) {
                houseHoverClock=0;
                if(!houseFlyout || !hovered.equals(flyoutGroup)) {houseFlyout=true;flyoutGroup=hovered;category=hovered;rebuildHud();}
            } else if(houseFlyout && flyoutBounds!=null && flyoutBounds.contains(cursor.x,cursor.y)) houseHoverClock=0;
            else if(houseFlyout && (houseHoverClock+=tpf)>.3f) {houseFlyout=false;flyoutGroup="";rebuildHud();}
        }
        previewClock+=tpf;
        brushClock+=tpf;
        brushCursorClock+=tpf;
        if(brushCursorClock>.07f) {
            brushCursorClock=0;brushOutline.setCullHint(Spatial.CullHint.Always);
            if(!photoMode && (tool.terrainBrush() || strokeBrush()) && !adjustingBrush && !walking && !libraryOpen && !rotating && !helpOpen && !sceneMenuOpen && !blueprintsOpen && !mainMenuOpen && !settingsOpen && inputManager.getCursorPosition().y>124 && hud.hit(inputManager.getCursorPosition().x,inputManager.getCursorPosition().y)==null) {
                var point=buildGround();
                if(point!=null) {
                    float radius=tool==Tool.PATH?pathWidth/2:tool==Tool.WATER?waterRadius:brushRadius;
                    if(brushRing==null){
                        brushRing=new Geometry("Bán kính cọ");
                        brushRing.rotate(-FastMath.HALF_PI,0,0);
                        var mat=whiteMaterial();mat.getAdditionalRenderState().setDepthTest(false);brushRing.setMaterial(mat);
                        brushRing.setQueueBucket(RenderQueue.Bucket.Transparent);brushOutline.attachChild(brushRing);
                    }
                    if(brushRingRadius!=radius){brushRing.setMesh(new Torus(64,6,.024f,radius));brushRingRadius=radius;}
                    var field=terrainDraft==null?design.terrain():terrainDraft;
                    brushRing.setLocalTranslation(point.x(),field.height(point.x(),point.z())+.06f,point.z());
                    brushOutline.setCullHint(Spatial.CullHint.Inherit);
                }
            }
        }
        if(drawing && terrainDraft!=null && brushClock>.06f) {sculpt(Math.min(.2f,brushClock)*1.4f);brushClock=0;dirtyPreview=false;}
        if(cam.getWidth()!=knownWidth || cam.getHeight()!=knownHeight) { stopCameraDrag();updateCamera();rebuildHud(); }
        if(drawing && terrainDraft==null && dirtyPreview && previewClock>0.065f) {
            previewClock=0;
            dirtyPreview=false;
            if(landEdge!=null) {
                updateLandDraft();
                previewLand();
            } else if(resizeAxis!=null) {
                updateResizeDraft();
                if(resizedItem!=null) {
                    previewItem(resizedItem,resizeScene(resizedItem));
                    refreshHandles();
                }
            } else {
                appendStroke();
                try {
                    if((tool==Tool.TREE || tool==Tool.FLOWERS) && moving==null) {
                        var nature=natureDrafts();var all=new ArrayList<>(design.items());all.addAll(nature);var temporary=new GladeDesign(all,design.land(),design.terrain());
                        preview.detachAllChildren();for(var plant:nature)preview.attachChild(art.render(plant,temporary));
                    } else {
                    var item=draft();
                    if(item!=null) {
                        if(tool==Tool.PATH && moving==null){
                            preview.detachAllChildren();
                            preview.attachChild(art.renderPath(item,design));
                        }else if(tool==Tool.WATER && moving==null){
                            preview.detachAllChildren();
                            var waterPreview=WaterMesh.preview(art,item,design);
                            var at=design.placement(item);
                            waterPreview.setLocalTranslation(at.center().x(),at.elevation(),at.center().z());
                            preview.attachChild(waterPreview);
                        }else{
                            var temporary=moving==null?design.add(item):design.replace(item);
                            if(moving==null && previewStructure()){preview.detachAllChildren();buildGhost.show(item,temporary,ghostState());}else previewItem(item,temporary);
                            refreshHandles();
                        }
                    }
                    }
                } catch(IllegalArgumentException ignored) { preview.detachAllChildren(); }
            }
        }
        if(animatedWaterScene!=design){
            animatedWaterScene=design;
            if(waterTerrain!=design.terrain()){waterBirth.clear();waterOrigins.clear();waterTerrain=design.terrain();}
            var sources=new HashSet<Long>();for(var source:design.items())if(WaterFlow.source(source)){
                sources.add(source.id());if(!Objects.equals(waterOrigins.put(source.id(),source.center()),source.center()))waterBirth.put(source.id(),elapsed);
            }
            waterBirth.keySet().retainAll(sources);waterOrigins.keySet().retainAll(sources);
        }
        for(var root:content.getChildren()) {
            Long id=root.getUserData("itemId");if(Boolean.TRUE.equals(root.getUserData("paintedWater")))root.depthFirstTraversal(part->SoftWater.animate(part,elapsed));
            Float birth=id==null?null:waterBirth.get(id);if(birth==null)continue;
            root.depthFirstTraversal(part->{SoftWater.animate(part,elapsed);Float delay=part.getUserData("flowDelay");if(delay!=null)part.setCullHint(elapsed-birth>=delay?Spatial.CullHint.Inherit:Spatial.CullHint.Always);
                Vector3f origin=part.getUserData("duckOrigin");if(origin!=null)part.setLocalTranslation(origin.add(FastMath.sin(elapsed*.5f)*.07f,FastMath.sin(elapsed*2)*.018f,FastMath.cos(elapsed*.5f)*.06f));});
        }
        if(photoFrames>=0) {
            photoFrames--;
            if(photoFrames==0) screenshots.takeScreenshot();
            if(photoFrames==-1) {hud.node().setCullHint(Spatial.CullHint.Inherit);contextHints.setCullHint(Spatial.CullHint.Inherit);}
        }
        if(Boolean.getBoolean("dreamhouse.smoke") && elapsed>3 && !smokeCaptured) {
            smokeCaptured=true;
            System.out.println("SCENE_READY · "+design.items().size()+" objects");
        }
        if(Boolean.getBoolean("dreamhouse.probe") && launchReady && elapsed-launchReadyAt>2.5f){System.out.println("PACKAGED_READY objects="+design.items().size()+" audio="+(audioRenderer!=null));stop();}
        if(Boolean.getBoolean("dreamhouse.smoke") && elapsed>4.5f) stop();
    }

    private void appendStroke() {
        if((tool.kind==null || !tool.kind.stroke() && tool!=Tool.WATER && tool!=Tool.TREE && tool!=Tool.FLOWERS || tool==Tool.BRIDGE) || current==null || stroke.isEmpty()) return;
        var previous=stroke.getLast();
        float distance=previous.distance(current);
        float spacing=tool==Tool.TREE?2.1f:tool==Tool.FLOWERS?.65f:.18f;
        int steps=(int)(distance/spacing);int limit=tool==Tool.TREE || tool==Tool.FLOWERS?128:4096;
        for(int i=1;i<=steps && stroke.size()<limit;i++) {
            float fraction=i*spacing/distance;
            stroke.add(new GladeDesign.Point(FastMath.interpolateLinear(fraction,previous.x(),current.x()),
                    FastMath.interpolateLinear(fraction,previous.z(),current.z())));
        }
    }
}

