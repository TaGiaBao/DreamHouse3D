package vn.dreamhouse.glade;

import com.jme3.math.FastMath;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashMap;

final class AssemblyDuplicator {
    record Copy(GladeDesign design,long selectedId,String message) {}
    static Copy duplicate(GladeDesign design,GladeDesign.Item item) {
        if(item.mount()!=null) {
            if(item.kind().roof()) throw new IllegalArgumentException("Mỗi nhà dùng một bộ mái. Chọn bộ mái khác để thay thế.");
            var mount=item.mount();
            float u=mount.u(),v=mount.v();
            if(Math.abs(v)>.49f) u=FastMath.clamp(u+(u>.2f?-.2f:.2f),-.4f,.4f);
            else v=FastMath.clamp(v+(v>.2f?-.2f:.2f),-.4f,.4f);
            var copy=new GladeDesign.Item(design.nextId(),item.kind(),item.center(),item.width(),item.depth(),item.height(),
                    item.pitched(),item.points(),item.rotation(),new GladeDesign.Mount(mount.hostId(),u,v,mount.lift()),item.form());
            return new Copy(design.add(copy),copy.id(),"Đã nhân bản cấu kiện trên cùng căn nhà.");
        }
        float minX=item.center().x(),maxX=minX,minZ=item.center().z(),maxZ=minZ;
        if(item.kind().footprint()) {
            minX-=item.halfWidth();maxX+=item.halfWidth();
            minZ-=item.halfDepth();maxZ+=item.halfDepth();
        }
        for(var point:item.points()) {
            minX=Math.min(minX,point.x());maxX=Math.max(maxX,point.x());
            minZ=Math.min(minZ,point.z());maxZ=Math.max(maxZ,point.z());
        }
        float dx=Math.min(1.6f,design.land().maxX()-maxX);
        if(dx<0.25f) dx=Math.max(-1.6f,design.land().minX()-minX);
        float dz=Math.min(0.6f,design.land().maxZ()-maxZ);
        if(dz<0.1f) dz=Math.max(-0.6f,design.land().minZ()-minZ);
        var points=new ArrayList<GladeDesign.Point>();
        for(var point:item.points()) points.add(new GladeDesign.Point(point.x()+dx,point.z()+dz));

        var copy=new GladeDesign.Item(design.nextId(),item.kind(),
                new GladeDesign.Point(item.center().x()+dx,item.center().z()+dz),item.width(),item.depth(),
                item.height(),item.pitched(),points,item.rotation(),null,item.form());
        var next=design.add(copy);
        var pending=new ArrayDeque<Long>();pending.add(item.id());var mapped=new HashMap<Long,Long>();mapped.put(item.id(),copy.id());
        while(!pending.isEmpty()) {
            long parent=pending.removeFirst(),newParent=mapped.get(parent);
            for(var child:design.items()) if(!mapped.containsKey(child.id()) && (child.form().support()==parent || child.mount()!=null && child.mount().hostId()==parent)) {
                var mount=child.mount();var form=child.form();
                if(mount!=null) mount=new GladeDesign.Mount(newParent,mount.u(),mount.v(),mount.lift());
                if(form.support()>=0) form=form.support(newParent);
                var center=new GladeDesign.Point(child.center().x()+dx,child.center().z()+dz);
                var cloned=new GladeDesign.Item(next.nextId(),child.kind(),center,child.width(),child.depth(),child.height(),child.pitched(),child.points(),child.rotation(),mount,form);
                next=next.add(cloned);mapped.put(child.id(),cloned.id());pending.add(child.id());
            }
        }
        return new Copy(next,copy.id(),"Đã nhân bản nhà cùng các cấu kiện gắn kèm.");


    }
}
