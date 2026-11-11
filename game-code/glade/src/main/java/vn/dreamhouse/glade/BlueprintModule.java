package vn.dreamhouse.glade;

import java.util.*;
import static vn.dreamhouse.glade.GladeDesign.*;

/** A detached assembly, expressed around its root and independent of the source world's terrain. */
record BlueprintModule(GladeDesign design) {
    static final int MAX_PARTS=256;
    BlueprintModule {
        if(design.items().isEmpty() || design.items().size()>MAX_PARTS)throw new IllegalArgumentException("Một mẫu cần từ 1 đến 256 khối.");
        var root=design.items().stream().filter(i->i.id()==0).findFirst().orElseThrow(()->new IllegalArgumentException("Mẫu thiếu khối gốc."));
        if(root.mount()!=null || root.form().support()>=0 || Math.abs(root.center().x())>.001f || Math.abs(root.center().z())>.001f || design.assemblyIds(0).size()!=design.items().size())throw new IllegalArgumentException("Mẫu phải là một cụm độc lập quanh khối gốc.");
    }
    Item root(){return design.items().stream().filter(i->i.id()==0).findFirst().orElseThrow();}
    static BlueprintModule capture(GladeDesign source,long selected){
        var root=source.items().stream().filter(i->i.id()==selected).findFirst().orElseThrow(()->new IllegalArgumentException("Chọn công trình trước khi lưu mẫu."));
        if(root.mount()!=null){long host=root.mount().hostId();root=source.items().stream().filter(i->i.id()==host).findFirst().orElseThrow();}
        var ids=source.assemblyIds(root.id());if(ids.size()>MAX_PARTS)throw new IllegalArgumentException("Cụm vượt 256 khối. Hãy chọn một phần nhỏ hơn.");
        var mapping=new HashMap<Long,Long>();mapping.put(root.id(),0L);long next=1;for(var item:source.items())if(ids.contains(item.id()) && item.id()!=root.id())mapping.put(item.id(),next++);
        var items=new ArrayList<Item>();float dx=-root.center().x(),dz=-root.center().z();
        for(var item:source.items())if(ids.contains(item.id()))items.add(shift(item,mapping,dx,dz));
        var bounds=bounds(items);float x=Math.max(4,Math.max(-bounds[0],bounds[1])+.1f),z=Math.max(3,Math.max(-bounds[2],bounds[3])+.1f);
        if(x>256 || z>256)throw new IllegalArgumentException("Cụm quá rộng để lưu quanh tâm. Hãy chọn một phần nhỏ hơn.");
        return new BlueprintModule(new GladeDesign(items,new LandBounds(-x,x,-z,z),new TerrainField()));
    }
    record Placement(GladeDesign scene,long rootId,Point anchor,Set<Long> added){}
    Placement place(GladeDesign target,Point requested){
        if(target.items().size()+design.items().size()>GladeDesign.MAX_IMPORTED_ITEMS)throw new IllegalArgumentException("Không đủ chỗ trong ngân sách công trình của thung lũng.");
        if(target.items().stream().mapToLong(i->i.points().size()).sum()+design.items().stream().mapToLong(i->i.points().size()).sum()>GladeDesign.MAX_TOTAL_POINTS)throw new IllegalArgumentException("Mẫu vượt ngân sách điểm vẽ còn lại của thung lũng.");
        var extents=bounds(design.items());float minX=target.land().minX()-extents[0],maxX=target.land().maxX()-extents[1],minZ=target.land().minZ()-extents[2],maxZ=target.land().maxZ()-extents[3];
        if(minX>maxX || minZ>maxZ)throw new IllegalArgumentException("Mẫu lớn hơn mảnh đất. Hãy mở rộng đất trước.");
        var anchor=new Point(Math.max(minX,Math.min(maxX,requested.x())),Math.max(minZ,Math.min(maxZ,requested.z())));
        var mapping=new HashMap<Long,Long>();long next=target.nextId();for(var item:design.items())mapping.put(item.id(),next++);
        var result=new ArrayList<>(target.items());for(var item:design.items())result.add(shift(item,mapping,anchor.x(),anchor.z()));
        return new Placement(new GladeDesign(result,target.land(),target.terrain()),mapping.get(0L),anchor,Set.copyOf(mapping.values()));
    }
    private static Item shift(Item item,Map<Long,Long> ids,float dx,float dz){
        var old=item.mount();Mount mount=old==null?null:new Mount(Objects.requireNonNull(ids.get(old.hostId())),old.u(),old.v(),old.lift());
        var form=item.form().support(ids.getOrDefault(item.form().support(),-1L));
        var points=item.points().stream().map(p->new Point(p.x()+dx,p.z()+dz)).toList();
        return new Item(ids.get(item.id()),item.kind(),new Point(item.center().x()+dx,item.center().z()+dz),item.width(),item.depth(),item.height(),item.pitched(),points,item.rotation(),mount,form);
    }
    private static float[] bounds(List<Item> items){
        float minX=Float.POSITIVE_INFINITY,maxX=-minX,minZ=minX,maxZ=-minX;
        for(var item:items)if(item.mount()==null){float w=item.kind().footprint()?item.halfWidth():0,d=item.kind().footprint()?item.halfDepth():0;minX=Math.min(minX,item.center().x()-w);maxX=Math.max(maxX,item.center().x()+w);minZ=Math.min(minZ,item.center().z()-d);maxZ=Math.max(maxZ,item.center().z()+d);for(var p:item.points()){minX=Math.min(minX,p.x());maxX=Math.max(maxX,p.x());minZ=Math.min(minZ,p.z());maxZ=Math.max(maxZ,p.z());}}
        return new float[]{minX,maxX,minZ,maxZ};
    }
}
