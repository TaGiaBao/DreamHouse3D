package vn.dreamhouse.glade;

import java.nio.file.*;
import java.util.*;
import static vn.dreamhouse.glade.GladeDesign.*;

class BlueprintDataCheck {
    static void ok(boolean value,String detail){if(!value)throw new AssertionError(detail);}
    static GladeDesign source(){
        var finish=new Finish("brick","#ae7783","slate","#7298b8",18);
        var house=new Item(10,Kind.HOUSE,new Point(5,-2),4,3,2.6f,true,List.of(),32,null,new Form(.2f,1.4f,.3f,-1).finish(finish));
        var upper=new Item(20,Kind.TOWER,new Point(5,-1.7f),1.4f,1.4f,2.1f,true,List.of(),12,null,new Form(.15f,.8f,0,10).finish(GladeCatalog.plain()));
        var window=new Item(30,Kind.WINDOW_ARCH,house.center(),.9f,.5f,1.08f,true,List.of(),0,new Mount(10,0,.5f,.55f));
        var neighbor=new Item(40,Kind.HOUSE,new Point(-5,0),2,2,2,true,List.of());
        return new GladeDesign(List.of(upper,house,window,neighbor),2);
    }
    static void rejected(Runnable operation,String label){try{operation.run();throw new AssertionError(label);}catch(IllegalArgumentException expected){}}
    public static void main(String[] args)throws Exception{
        var source=source();var module=BlueprintModule.capture(source,30);
        ok(module.design().items().size()==3 && module.root().id()==0 && module.root().center().equals(new Point(0,0)),"window capture resolves host and only descendants");
        ok(module.root().form().finish().equals(source.items().get(1).form().finish()) && module.root().rotation()==32,"finish and rotation preserved");
        ok(BlueprintModule.capture(source,20).root().form().support()==-1,"upper-floor-only template detaches external support");
        var target=new GladeDesign(List.of(new Item(100,Kind.BENCH,new Point(-8,-5),1,1,1,false,List.of())),2);
        var placed=module.place(target,new Point(250,250));
        ok(placed.scene().items().size()==4 && placed.added().size()==3 && !placed.added().contains(100L),"all new IDs are disjoint from target IDs");
        var root=placed.scene().items().stream().filter(i->i.id()==placed.rootId()).findFirst().orElseThrow();
        var top=placed.scene().items().stream().filter(i->i.form().support()==placed.rootId()).findFirst().orElseThrow();
        var opening=placed.scene().items().stream().filter(i->i.mount()!=null).findFirst().orElseThrow();
        ok(opening.mount().hostId()==root.id() && top.form().support()==root.id(),"mount/support IDs remapped even when child precedes parent");
        ok(root.center().equals(placed.anchor()) && Math.abs(top.center().z()-root.center().z()-.3f)<.001f,"whole assembly clamped at land edge with relative offsets");
        ok(Math.abs(placed.scene().placement(top).elevation()-placed.scene().top(root)-.15f)<.001f,"stack keeps relative elevation");
        var points=List.of(new Point(1,1),new Point(2,1.5f),new Point(3,2));var path=new Item(0,Kind.PATH,new Point(1,1),1,1,1,false,points);
        var pathModule=BlueprintModule.capture(new GladeDesign(List.of(path),2),0);var pathPlacement=pathModule.place(new GladeDesign(List.of(),2),new Point(-4,-3));
        ok(pathPlacement.scene().items().getFirst().points().getLast().equals(new Point(-2,-2)),"stroke template translates every control point");
        var preview=new ModulePreview(new com.jme3.asset.DesktopAssetManager(true));preview.show(pathPlacement);preview.updateGeometricState();ok(preview.getQuantity()==1 && ((com.jme3.scene.Geometry)preview.getChild(0)).getMesh().getVertexCount()==4,"stroke preview follows polyline instead of a fixed box");
        rejected(()->module.place(new GladeDesign(List.of(),new LandBounds(-4,4,-3,3)),new Point(Float.NaN,0)),"nonfinite anchor rejected");
        var wide=new Item(0,Kind.HOUSE,new Point(0,0),20,3,2,true,List.of());var wideModule=BlueprintModule.capture(new GladeDesign(List.of(wide),2),0);
        rejected(()->wideModule.place(new GladeDesign(List.of(),new LandBounds(-4,4,-3,3)),new Point(0,0)),"oversize placement rejected without changing scene");
        var many=new ArrayList<Item>();many.add(module.root());for(int i=1;i<=256;i++)many.add(new Item(i,Kind.TOWER,new Point(0,0),1,1,1,false,List.of(),0,null,new Form(0,0,0,0)));
        rejected(()->new BlueprintModule(new GladeDesign(many,2)),"part budget enforced");
        var folder=PlayerFiles.directory("blueprint-data");var shelf=new BlueprintShelf(folder);var saved=shelf.save("Nhà ngói của tôi",module);
        var reopened=new BlueprintShelf(folder);ok(reopened.list().entries().getFirst().module().design().items().equals(module.design().items()),"shelf survives restart and preserves assembly");
        var original=Files.readAllBytes(folder.resolve(saved.id()+".glade"));try{shelf.rename(saved.id()," ");throw new AssertionError("bad name accepted");}catch(IllegalArgumentException expected){}
        ok(Arrays.equals(original,Files.readAllBytes(folder.resolve(saved.id()+".glade"))),"failed rename preserves previous good file");
        var renamed=shelf.rename(saved.id(),"Nhà hoa mới");ok(renamed.id().equals(saved.id()) && reopened.list().entries().getFirst().name().equals("Nhà hoa mới"),"rename is persistent and keeps identity");
        String broken=UUID.randomUUID().toString();Files.writeString(folder.resolve(broken+".glade"),"broken");
        var huge=folder.resolve(UUID.randomUUID()+".glade");try(var file=new java.io.RandomAccessFile(huge.toFile(),"rw")){file.setLength(BlueprintShelf.MAX_BYTES+1L);}
        ok(reopened.list().entries().size()==1 && reopened.list().skipped()==2,"corrupt/oversized entries isolated");
        rejected(()->{try{shelf.delete("../outside");}catch(java.io.IOException e){throw new RuntimeException(e);}},"IDs cannot escape shelf directory");
        shelf.delete(saved.id());ok(reopened.list().entries().isEmpty(),"delete removes only chosen personal template");
        var fullFolder=PlayerFiles.directory("blueprint-full");for(int i=0;i<BlueprintShelf.MAX_ENTRIES;i++)Files.writeString(fullFolder.resolve(UUID.randomUUID()+".glade"),"invalid placeholder");
        try{new BlueprintShelf(fullFolder).save("Không đủ chỗ",module);throw new AssertionError("full shelf accepted another entry");}catch(java.io.IOException expected){}
        var guide=new BuildGuide();guide.observe(new GladeDesign(List.of(),2),source);ok(guide.count()==0,"inactive guide ignores actions");guide.start();guide.observe(new GladeDesign(List.of(),2),source);ok(guide.next()==BuildGuide.Goal.SHAPE,"new building completes build goal");
        var old=source.items().get(1);var moved=new Item(old.id(),old.kind(),new Point(4,-2),old.width(),old.depth(),old.height(),true,List.of(),old.rotation(),null,old.form());guide.observe(source,source.replace(moved));ok(guide.count()==1,"movement alone is not shape editing");
        var changed=new Item(old.id(),old.kind(),old.center(),old.width(),old.depth(),old.height(),true,List.of(),old.rotation(),null,old.form().sides(.3f,.2f));guide.observe(source,source.replace(changed));ok(guide.next()==BuildGuide.Goal.PAINT,"roof-side drag completes shape goal");
        var colored=new Item(old.id(),old.kind(),old.center(),old.width(),old.depth(),old.height(),true,List.of(),old.rotation(),null,old.form().finish(GladeCatalog.plain()));guide.observe(source,source.replace(colored));ok(guide.next()==BuildGuide.Goal.BLUEPRINT,"paint requires actual finish change");
        guide.mark(BuildGuide.Goal.BLUEPRINT);ok(guide.next()==BuildGuide.Goal.SAVE,"saving module is separate from saving world");guide.mark(BuildGuide.Goal.SAVE);ok(guide.next()==null && guide.count()==5,"guide completes five independent real actions");
        System.out.println("BLUEPRINT_DATA_PASS: normalized assembly, detached upper floor, unique/remapped IDs, rotated edge clamp, relative elevation, oversize/budget rejection, persistent atomic shelf/rename/delete, corruption isolation, safe IDs and actionable guide goals; no screenshots");
    }
}
