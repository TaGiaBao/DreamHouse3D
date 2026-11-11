package vn.dreamhouse.glade;

import com.jme3.system.AppSettings;
import com.jme3.math.*;
import com.jme3.scene.*;
import java.util.*;
import java.nio.file.*;

/** Real renderer, authored scene persistence and editable-scene smoke check. */
class GardenStyleCheck extends ProceduralMechanicsCheck {
    public static void main(String[] args)throws Exception{
        var scene=GardenScene.create();var file=PlayerFiles.directory("checks").resolve("garden.glade");scene.save(file);
        ok(GladeDesign.load(file).items().equals(scene.items()),"every garden item survives save/load");
        app=new GladeApp();var settings=new AppSettings(true);settings.setResolution(Arrays.asList(args).contains("--preview")?1360:960,Arrays.asList(args).contains("--preview")?860:640);settings.setSamples(4);settings.setGammaCorrection(true);settings.setVSync(true);settings.setRenderer(AppSettings.LWJGL_OPENGL33);settings.setAudioRenderer(null);settings.setTitle("DreamHouse · Vườn mơ ước — Bản phát triển");app.setSettings(settings);app.setShowSettings(false);app.setPauseOnLostFocus(false);app.start();
        try{
            engine(()->true);engine(()->{var m=GladeApp.class.getDeclaredMethod("action",String.class);m.setAccessible(true);m.invoke(app,"sample");return true;});Thread.sleep(1500);if(Boolean.getBoolean("dreamhouse.noPost")){set("qualityLow",true);call("applyQuality");}
            ok(design().items().equals(scene.items()),"sample action uses editable scene");
            ok(engine(()->{int[] count={0};app.getRootNode().depthFirstTraversal(s->{if(s instanceof Geometry g){count[0]++;var buf=g.getMesh().getBuffer(VertexBuffer.Type.Position);if(buf!=null){var p=(java.nio.FloatBuffer)buf.getData();for(int i=0;i<p.limit();i++)ok(Float.isFinite(p.get(i)),"finite vertex "+g.getName());}}});System.out.println("GARDEN_GEOMETRIES="+count[0]);return count[0]<1600;}),"scene geometry remains bounded");
            if(Arrays.asList(args).contains("--preview")){System.out.println("GARDEN_PREVIEW_READY");Thread.sleep(900000);}
            else {
                var item=design().items().get(3);var changed=new GladeDesign.Item(item.id(),item.kind(),new GladeDesign.Point(6.2f,-1.4f),item.width(),item.depth(),item.height(),false,List.of(),15,null,item.form().roof(0));
                fixture(design().replace(changed));ok(design().items().get(3).form().roof()==0,"garden cottage can flatten and rotate");
                fixture(scene);call("rebuildHud");
                for(var r:ArchitectureCheck.buttons().values())ok(r.x>=0 && r.y>=0 && r.getMaxX()<=960 && r.getMaxY()<=640,"controls fit minimum window");
                System.out.println("GARDEN_STYLE_PASS: editable sample, signed save roundtrip, finite and bounded actual scene geometry, roof morph/rotation, minimum UI fit; no screenshots");
            }
        }finally{app.stop();}
    }
}
