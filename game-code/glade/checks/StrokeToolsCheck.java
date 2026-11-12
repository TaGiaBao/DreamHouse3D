package vn.dreamhouse.glade;

import com.jme3.collision.CollisionResults;
import com.jme3.input.KeyInput;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import java.util.List;
import static vn.dreamhouse.glade.GladeDesign.*;

class StrokeToolsCheck extends ProceduralMechanicsCheck {
    public static void main(String[] args)throws Exception {
        var empty=new GladeDesign(List.of(),2);
        var points=List.of(new Point(-2,0),new Point(-1,0),new Point(0,0),new Point(1,0),new Point(2,0));
        for(var stroke:List.of(List.of(new Point(0,0)),points)){
            var narrow=WaterGesture.draft(empty,stroke,stroke.getLast(),.35f,0);
            var wide=WaterGesture.draft(empty,stroke,stroke.getLast(),4,0);
            ok(WaterFlow.simulate(empty.add(wide),wide).cells().size()>2*WaterFlow.simulate(empty.add(narrow),narrow).cells().size(),
                    "width changes actual flowing water, including click sources");
        }
        app=new GladeApp();var settings=new AppSettings(true);settings.setResolution(1100,720);
        settings.setRenderer(AppSettings.LWJGL_OPENGL33);settings.setAudioRenderer(null);
        app.setSettings(settings);app.setShowSettings(false);app.setPauseOnLostFocus(false);app.start();
        try {
            engine(()->true);set("qualityLow",true);call("applyQuality");fixture(empty);
            clickRaw("water");slider(true);
            ok((Float)field("waterRadius")==4f && design().items().isEmpty(),"slider reaches max without painting a source");
            clickRaw("landscape");slider(false);
            ok((Float)field("pathWidth")==.5f && (Float)field("waterRadius")==4f,"cọ đường và nước có độ rộng riêng");
            clickRaw("pathMaterial:concrete");clickRaw("pathColor:#84918d");
            draw(-4,4,4);
            var first=design().items().getFirst();
            ok(first.width()==.5f && first.form().finish().material().equals("concrete") && first.form().finish().color().equals("#84918d"),"road carries chosen width, texture and color");
            slider(true);clickRaw("pathMaterial:gravel");clickRaw("pathColor:reset");draw(-4,4,-3);
            var wide=design().items().getLast();
            ok(wide.width()==4 && wide.form().finish().material().equals("gravel"),"wide gravel road is one stroke");
            ok(engine(()->{
                var f=GladeApp.class.getDeclaredField("content");f.setAccessible(true);
                var root=(Node)f.get(app);var hits=new CollisionResults();
                root.collideWith(new Ray(new Vector3f(0,8,-1.6f),new Vector3f(0,-1,0)),hits);
                return hits.size()>0 && "gravel".equals(hits.getClosestCollision().getGeometry().getUserData("paintMaterial"));
            }),"visible mesh has full physical width and gravel material");
            clickRaw("palette");click(new Vector3f(0,.04f,-3));clickRaw("paintMaterial:brick");clickRaw("paintColor:#b87157");
            ok(design().items().getLast().form().finish().material().equals("brick") && design().items().getLast().width()==4,"palette changes existing road without losing width");
            chord(KeyInput.KEY_Z);ok(design().items().getLast().form().finish().color().isEmpty(),"undo only the last recolor");
            click(new Vector3f(0,.04f,-3));
            clickRaw("paintPathCustomColor");customColor();
            ok(design().items().getLast().form().finish().color().equals("#59746b"),"custom picker applies an arbitrary color");
            var file=PlayerFiles.directory("checks").resolve("stroke-tools.glade");
            design().save(file);ok(GladeDesign.load(file).items().equals(design().items()),"width and finish survive save and load");
            System.out.println("STROKE_TOOLS_PASS: independent sliders, water spread, road geometry, editing, undo and save");
        }finally{app.stop();}
    }

    private static void slider(boolean wider)throws Exception {
        var area=ArchitectureCheck.buttons().get("brushWidth");ok(area!=null,"slider visible");
        int middle=(int)area.getCenterX(),y=(int)area.getCenterY();
        int edge=wider?area.x+area.width+10:area.x-10;
        mouse(middle,y,true);mouse(edge,y,null);mouse(edge,y,false);
    }

    private static void draw(float x1,float x2,float z)throws Exception {
        var a=screen(new Vector3f(x1,0,z));var b=screen(new Vector3f(x2,0,z));
        mouse((int)a.x,(int)a.y,true);mouse((int)b.x,(int)b.y,null);mouse((int)b.x,(int)b.y,false);
    }

    private static void customColor()throws Exception {
        for(int i=0;i<25;i++){
            var accepted=new java.util.concurrent.atomic.AtomicBoolean();
            javax.swing.SwingUtilities.invokeAndWait(()->{
                for(var window:java.awt.Window.getWindows()){
                    if(!(window instanceof javax.swing.JDialog dialog) || !dialog.isShowing() || !dialog.getTitle().equals("Chọn màu đường"))continue;
                    var chooser=chooser(dialog);
                    if(chooser==null)continue;
                    chooser.setColor(java.awt.Color.decode("#59746b"));
                    dialog.getRootPane().getDefaultButton().doClick();
                    accepted.set(true);
                    return;
                }
            });
            if(accepted.get()){engine(()->true);return;}
            Thread.sleep(100);
        }
        throw new AssertionError("custom path color dialog did not appear");
    }

    private static javax.swing.JColorChooser chooser(java.awt.Container root) {
        if(root instanceof javax.swing.JColorChooser match)return match;
        for(var child:root.getComponents())if(child instanceof java.awt.Container container){
            var match=chooser(container);if(match!=null)return match;
        }
        return null;
    }
}
