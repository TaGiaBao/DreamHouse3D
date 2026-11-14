package vn.dreamhouse.glade;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.math.Vector3f;
import com.jme3.system.AppSettings;
import java.util.*;
import static vn.dreamhouse.glade.GladeDesign.*;

class WaterDrawingCheck extends ProceduralMechanicsCheck {
    private static void drawBentStroke()throws Exception{
        var a=screen(new Vector3f(-3,0,0));var b=screen(new Vector3f(0,0,0));var c=screen(new Vector3f(0,0,3));
        mouse((int)a.x,(int)a.y,true);mouse((int)b.x,(int)b.y,null);mouse((int)c.x,(int)c.y,null);mouse((int)c.x,(int)c.y,false);
    }
    public static void main(String[] args)throws Exception{
        var empty=new GladeDesign(List.of(),2);var points=List.of(new Point(-3,0),new Point(-1,0),new Point(0,1),new Point(0,3));
        var drawn=WaterGesture.draft(empty,points,points.getLast(),.7f,0);ok(WaterFlow.source(drawn) && drawn.points().equals(points),"free polyline remains saved as one flowing item");
        var flow=WaterFlow.simulate(empty.add(drawn),drawn);ok(flow.at(-2,0)!=null && flow.at(0,2)!=null && flow.at(-3,3)==null,"L-shaped stroke follows line rather than a circle");
        var clicked=WaterGesture.draft(empty,List.of(new Point(2,2)),new Point(2,2),1.2f,0);ok(clicked.points().size()==1 && WaterFlow.source(clicked),"single click remains a source");
        var edge=WaterGesture.draft(empty,List.of(new Point(empty.land().maxX()-.2f,0),new Point(empty.land().maxX()-.2f,2)),new Point(empty.land().maxX()-.2f,2),.7f,0);empty.add(edge);
        var map=new HashMap<TerrainField.Cell,Float>();for(int x=-5;x<=-1;x++)for(int z=-4;z<=4;z++)map.put(new TerrainField.Cell(x,z),3.5f);
        var hill=new GladeDesign(List.of(),empty.land(),new TerrainField(map));var elevated=WaterGesture.draft(hill,List.of(new Point(-1.1f,0)),new Point(-1.1f,0),1,0);hill=hill.add(elevated);var downhill=WaterFlow.simulate(hill,elevated);ok(downhill.cells().values().stream().anyMatch(w->w.level()>3) && downhill.cells().values().stream().anyMatch(w->w.level()<1),"water reaches lower land from plateau");
        var art=new GladeArt(new DesktopAssetManager(true));ok((Integer)WaterFalls.render(art,elevated,hill,downhill).getUserData("waterfallCount")>0,"height transition creates falling ribbons");
        var preview=WaterMesh.preview(art,drawn,empty);
        ok(preview.getTriangleCount()>0 && preview.getTriangleCount()<=2*points.size()+24,
                "preview geometry grows linearly with the number of points");
        var edgePreview=WaterMesh.preview(art,edge,empty);
        var vertices=(java.nio.FloatBuffer)edgePreview.getMesh().getBuffer(com.jme3.scene.VertexBuffer.Type.Position).getData();
        for(int i=0;i<vertices.limit();i+=3)
            ok(vertices.get(i)+edge.center().x()<=empty.land().maxX()+.001f,"preview stays inside the build boundary");
        var platform=new Item(0,Kind.HOUSE,new Point(0,0),3,3,3,false,List.of());var onTop=new Item(1,Kind.WATER,new Point(0,0),.5f,.5f,1,false,List.of(new Point(0,0)),0,null,new Form(3,0,0,-1).finish(Finish.DEFAULT.variant(20)));var rooftop=new GladeDesign(List.of(platform,onTop),2);var roofFlow=WaterFlow.simulate(rooftop,onTop);ok((Integer)WaterFalls.render(art,onTop,rooftop,roofFlow).getUserData("waterfallCount")>0,"water can spill from raised construction");
        var file=PlayerFiles.directory("checks").resolve("drawn-water.glade");hill.add(new Item(2,drawn.kind(),drawn.center(),drawn.width(),drawn.depth(),drawn.height(),false,drawn.points(),0,null,drawn.form())).save(file);ok(GladeDesign.load(file).items().size()==2,"source and drawn line survive signed save");
        app=new GladeApp();var settings=new AppSettings(true);settings.setResolution(1100,720);settings.setRenderer(AppSettings.LWJGL_OPENGL33);settings.setAudioRenderer(null);settings.setTitle("DreamHouse · Kiểm tra vẽ nước và thác");app.setSettings(settings);app.setShowSettings(false);app.setPauseOnLostFocus(false);app.start();
        try{
            engine(()->true);fixture(empty);clickRaw("water");
            var worker=(java.util.concurrent.ExecutorService)field("waterWorker");
            var hold=new java.util.concurrent.CountDownLatch(1);
            worker.submit(()->{hold.await();return null;});
            try{
                drawBentStroke();
                ok(field("pendingWater")!=null && design().items().isEmpty(),"long water prepares in background before entering history");
                chord(com.jme3.input.KeyInput.KEY_Z);
                ok(field("pendingWater")==null && design().items().isEmpty(),"undo cancels pending stroke without editing the scene");
            }finally{hold.countDown();}
            drawBentStroke();
            long until=System.nanoTime()+8_000_000_000L;
            while(design().items().isEmpty() && System.nanoTime()<until)Thread.sleep(30);
            ok(design().items().size()==1 && item(0).points().size()>5 && WaterFlow.source(item(0)),"actual left drag commits one flowing stroke");chord(com.jme3.input.KeyInput.KEY_Z);ok(design().items().isEmpty(),"one undo removes entire stroke");
            click(new Vector3f(1,0,1));ok(design().items().size()==1 && item(0).points().size()==1,"actual click places a single source");fixture(new GladeDesign(List.of(platform),2));clickRaw("water");click(new Vector3f(0,3,0));ok(design().items().size()==2 && design().items().getLast().form().base()>2.9f,"actual click on raised construction places water at contact height");fixture(hill);Thread.sleep(350);ok(tagged("waterfall","true"),"waterfall renders in actual game");
            System.out.println("WATER_DRAWING_PASS: background preparation and cancellation, one-step undo, free flow, downhill falls and save roundtrip");
        }finally{app.stop();}
    }
}
