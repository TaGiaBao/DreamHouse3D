package vn.dreamhouse.glade;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import java.util.*;
class ResourceCacheCheck {
    public static void main(String[]args)throws Exception{
        var art=new GladeArt(new DesktopAssetManager(true));Material retained=art.material("#deaa88");var before=((ColorRGBA)retained.getParam("Diffuse").getValue()).clone();
        for(int i=0;i<3000;i++)art.material(String.format(Locale.ROOT,"#%06x",i));
        var field=GladeArt.class.getDeclaredField("materials");field.setAccessible(true);var cache=(Map<?,?>)field.get(art);
        if(cache.size()>512 || !retained.getParam("Diffuse").getValue().equals(before))throw new AssertionError("material cache grows or mutates a retained scene material");
        var restored=art.material("#deaa88");if(!restored.getParam("Diffuse").getValue().equals(before))throw new AssertionError("evicted material cannot be recreated correctly");
        var river = new GladeDesign.Item(0, GladeDesign.Kind.WATER, new GladeDesign.Point(0, 0),
                4, 2, .7f, false, List.of(new GladeDesign.Point(-2, 0), new GladeDesign.Point(0, 0), new GladeDesign.Point(2, 0)));
        var roadStart = new GladeDesign.Point(0, .83f);
        var road = new GladeDesign.Item(1, GladeDesign.Kind.PATH, roadStart,
                1, 1, 1, false, List.of(roadStart, new GladeDesign.Point(2, .83f)));
        var scene = new GladeDesign(List.of(river), 2);
        var renderer = new SceneRenderer();
        var root = new Node();
        renderer.update(root, art, scene);
        int beforeRoad = ((Node) root.getChild(0)).getChild("Bờ suối") instanceof Node bank ? bank.getTriangleCount() : 0;
        renderer.update(root, art, scene.add(road));
        int withRoad = ((Node) root.getChild(0)).getChild("Bờ suối") instanceof Node bank ? bank.getTriangleCount() : 0;
        renderer.update(root, art, scene);
        int restoredBank = ((Node) root.getChild(0)).getChild("Bờ suối") instanceof Node bank ? bank.getTriangleCount() : 0;
        if (!(beforeRoad > withRoad && beforeRoad == restoredBank)) {
            throw new AssertionError("riverbank decor must rebuild when an adjacent road is added or removed");
        }
        System.out.println("RESOURCE_CACHE_PASS: material cache bounded to 512 and riverbank rebuilt when path changes; no screenshots");
    }
}
