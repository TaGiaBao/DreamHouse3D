package vn.dreamhouse.glade;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

final class GladeDesign {
    static final int MAX_FILE_BYTES=16*1024*1024,MAX_IMPORTED_ITEMS=10000,MAX_TOTAL_POINTS=250000;

    enum Kind {
        HOUSE, MANOR, CANAL_HOUSE, BRICK_HOUSE, TOWER, PAVILION, BRIDGE, WALL, FENCE, PATH, TREE, FLOWERS, WATER,
        LOG_CABIN, BRICK_WALL, TIMBER_WALL, HEDGE, ROOF_TILE, ROOF_SLATE, CHIMNEY_BRICK, CHIMNEY_STONE,
        WINDOW_ARCH, WINDOW_ROUND, DOOR_ARCH, DOOR_DOUBLE, STAIRS_STONE, STAIRS_WOOD, DECK, PERGOLA,
        PLANTER, WELL, BENCH, PENTAGON, ROOF_FLAT, ROOF_PYRAMID, ROOF_PENTAGON, ROOF_SHED;

        boolean building() { return this==HOUSE || this==MANOR || this==CANAL_HOUSE || this==BRICK_HOUSE || this==TOWER || this==PAVILION || this==LOG_CABIN || this==PENTAGON; }
        boolean footprint() { return !stroke(); }
        boolean stroke() { return this==WALL || this==FENCE || this==BRIDGE || this==PATH || this==BRICK_WALL || this==TIMBER_WALL || this==HEDGE; }
        boolean roof() { return this==ROOF_TILE || this==ROOF_SLATE || RoofProfiles.shaped(this); }
        boolean chimney() { return this==CHIMNEY_BRICK || this==CHIMNEY_STONE; }
        boolean opening() { return this==WINDOW_ARCH || this==WINDOW_ROUND || this==DOOR_ARCH || this==DOOR_DOUBLE; }
        boolean mounted() { return roof() || chimney() || opening(); }
        boolean wallStroke() { return stroke() && this!=BRIDGE && this!=PATH; }
        boolean roofHost() { return building() && this!=TOWER && this!=PAVILION; }
    }

    record Mount(long hostId,float u,float v,float lift) {
        Mount {
            if(hostId<0 || !Float.isFinite(u) || !Float.isFinite(v) || !Float.isFinite(lift)
                    || Math.abs(u)>0.51f || Math.abs(v)>0.51f || lift<0 || lift>2f)
                throw new IllegalArgumentException("Điểm gắn cấu kiện không hợp lệ.");
        }
    }
    record Placement(Point center,float rotation,float elevation,float width,float depth) {}
    record Finish(String material,String color,String roofMaterial,String roofColor,int variant) {
        Finish(String material,String color,String roofMaterial,String roofColor) {this(material,color,roofMaterial,roofColor,0);}
        Finish variant(int value){return new Finish(material,color,roofMaterial,roofColor,value);}
        static final Finish DEFAULT=new Finish("original","","original","");
        Finish {
            if(variant<0 || variant>20 || !Set.of("original","plaster","brick","stone","wood","dirt","concrete","gravel").contains(material)
                || !Set.of("original","tile","slate","wood","metal").contains(roofMaterial)
                || !color.matches("(#[0-9a-fA-F]{6})?") || !roofColor.matches("(#[0-9a-fA-F]{6})?"))
                throw new IllegalArgumentException("Vật liệu hoặc màu không hợp lệ.");
        }
    }
    record Form(float base,float roof,float stilts,long support,float roofFront,float roofBack,float roofLeft,float roofRight,Finish finish) {
        Form(float base,float roof,float stilts,long support) {this(base,roof,stilts,support,0,0);}
        Form(float base,float roof,float stilts,long support,float front,float back) {this(base,roof,stilts,support,front,back,0,0,Finish.DEFAULT);}
        Form base(float value) {return new Form(value,roof,stilts,support,roofFront,roofBack,roofLeft,roofRight,finish);}
        Form roof(float value) {return new Form(base,value,stilts,support,roofFront,roofBack,roofLeft,roofRight,finish);}
        Form stilts(float value) {return new Form(base,roof,value,support,roofFront,roofBack,roofLeft,roofRight,finish);}
        Form support(long value) {return new Form(base,roof,stilts,value,roofFront,roofBack,roofLeft,roofRight,finish);}
        Form sides(float left,float right) {return new Form(base,roof,stilts,support,roofFront,roofBack,left,right,finish);}
        Form ends(float front,float back) {return new Form(base,roof,stilts,support,front,back,roofLeft,roofRight,finish);}
        Form finish(Finish value) {return new Form(base,roof,stilts,support,roofFront,roofBack,roofLeft,roofRight,value);}
        Form {
            if(finish==null || !Float.isFinite(base) || !Float.isFinite(roof) || !Float.isFinite(stilts)
                    || !Float.isFinite(roofFront) || !Float.isFinite(roofBack) || !Float.isFinite(roofLeft) || !Float.isFinite(roofRight)
                    || roofFront<0 || roofBack<0 || roofFront>32 || roofBack>32 || roofLeft<0 || roofRight<0 || roofLeft>32 || roofRight>32
                    || base < -32 || base>160 || roof<0 || roof>32 || stilts<0 || stilts>32 || support< -1)
                throw new IllegalArgumentException("Hình khối không hợp lệ.");
        }
    }
    record Point(float x, float z) {
        Point {
            if (!Float.isFinite(x) || !Float.isFinite(z)
                    || Math.abs(x)>LandBounds.LIMIT_X+0.01f || Math.abs(z)>LandBounds.LIMIT_Z+0.01f)
                throw new IllegalArgumentException("Vị trí nằm ngoài thung lũng.");
        }
        float distance(Point other) { return (float)Math.hypot(x-other.x,z-other.z); }
    }

    record Item(long id, Kind kind, Point center, float width, float depth, float height,
                boolean pitched, List<Point> points, float rotation,Mount mount,Form form) {
        Item(long id,Kind kind,Point center,float width,float depth,float height,boolean pitched,List<Point> points,float rotation,Mount mount) {
            this(id,kind,center,width,depth,height,pitched,points,rotation,mount,null);
        }
        Item(long id,Kind kind,Point center,float width,float depth,float height,boolean pitched,List<Point> points,float rotation) {
            this(id,kind,center,width,depth,height,pitched,points,rotation,null);
        }
        Item(long id,Kind kind,Point center,float width,float depth,float height,boolean pitched,List<Point> points) {
            this(id,kind,center,width,depth,height,pitched,points,0);
        }
        Item {
            if(id<0 || kind==null || center==null || !Float.isFinite(width) || !Float.isFinite(depth)
                    || !Float.isFinite(height) || width<0.5f || width>512 || depth<0.5f || depth>512
                    || height<0.04f || height>160 || points==null || points.size()>4096 || !Float.isFinite(rotation))
                throw new IllegalArgumentException("Công trình không hợp lệ.");
            points=List.copyOf(points);
            if(form==null) form=new Form(0,kind.building() && pitched?Math.min(32,kind==Kind.TOWER?width*.60f:(width/2+.32f)*.62f):0,0,-1);
            if(kind.building()) pitched=form.roof()>.02f;
            if(form.support()==id) throw new IllegalArgumentException("Khối không thể tự đỡ chính nó.");
            if(mount!=null && (!kind.mounted() || mount.hostId()==id)) throw new IllegalArgumentException("Cấu kiện không thể gắn ở đây.");
            if(kind.mounted() && mount==null) throw new IllegalArgumentException("Hãy đặt cấu kiện lên một căn nhà.");
            rotation=(rotation%360+360)%360;
            if(!kind.footprint() && rotation!=0) throw new IllegalArgumentException("Cấu kiện này xoay bằng hướng nét vẽ.");
            if(kind.stroke() && points.isEmpty())
                throw new IllegalArgumentException("Nét vẽ đang trống.");
        }
        float halfWidth() {
            double angle=Math.toRadians(rotation);
            return (float)(Math.abs(Math.cos(angle))*width+Math.abs(Math.sin(angle))*depth)/2;
        }
        float halfDepth() {
            double angle=Math.toRadians(rotation);
            return (float)(Math.abs(Math.sin(angle))*width+Math.abs(Math.cos(angle))*depth)/2;
        }
        float localX(Point point) {
            double angle=Math.toRadians(rotation);
            return (float)(Math.cos(angle)*(point.x()-center.x())-Math.sin(angle)*(point.z()-center.z()));
        }
        float localZ(Point point) {
            double angle=Math.toRadians(rotation);
            return (float)(Math.sin(angle)*(point.x()-center.x())+Math.cos(angle)*(point.z()-center.z()));
        }
    }

    private final List<Item> items;
    private final LandBounds land;
    private final TerrainField terrain;
    GladeDesign() { this(List.of()); }
    GladeDesign(List<Item> items) { this(items,1); }
    GladeDesign(List<Item> items,int landLevel) { this(items,LandBounds.level(landLevel)); }
    GladeDesign(List<Item> items,LandBounds land) {
        this(items,land,new TerrainField());
    }
    GladeDesign(List<Item> items,LandBounds land,TerrainField terrain) {
        if(land==null) throw new IllegalArgumentException("Mảnh đất không hợp lệ.");
        this.land=land;
        this.terrain=Objects.requireNonNull(terrain);
        this.items=List.copyOf(items);
        if(items.stream().map(Item::id).distinct().count()!=items.size())
            throw new IllegalArgumentException("Danh sách công trình không hợp lệ.");
        var roofHosts=new HashSet<Long>();
        for(var item:items) {
            var visited=new HashSet<Long>();var child=item;
            while(child.form().support()>=0) {
                if(!visited.add(child.id())) throw new IllegalArgumentException("Liên kết chồng khối tạo vòng lặp.");
                long support=child.form().support();
                child=items.stream().filter(other->other.id()==support).findFirst().orElseThrow(()->new IllegalArgumentException("Khối đỡ không tồn tại."));
                if(!child.kind().building() && child.kind()!=Kind.DECK) throw new IllegalArgumentException("Chỉ chồng lên công trình hoặc sàn.");
            }
            if(item.mount()!=null) {
                if(item.kind().roof() && !roofHosts.add(item.mount().hostId()))
                    throw new IllegalArgumentException("Một căn nhà chỉ dùng một bộ mái gắn riêng.");
                var host=items.stream().filter(other->other.id()==item.mount().hostId()).findFirst().orElse(null);
                if(host==null || !host.kind().building() || host.kind()==Kind.PAVILION || item.kind().roof() && !host.kind().roofHost() || host.mount()!=null)
                    throw new IllegalArgumentException("Cấu kiện mất căn nhà chủ.");
                continue;
            }
            if(item.center().x()<land.minX()-0.01f || item.center().x()>land.maxX()+0.01f
                    || item.center().z()<land.minZ()-0.01f || item.center().z()>land.maxZ()+0.01f
                    || item.kind().footprint() && (item.center().x()-item.halfWidth()<land.minX()-0.01f
                    || item.center().x()+item.halfWidth()>land.maxX()+0.01f
                    || item.center().z()-item.halfDepth()<land.minZ()-0.01f
                    || item.center().z()+item.halfDepth()>land.maxZ()+0.01f)
                    || item.points().stream().anyMatch(point->point.x()<land.minX()-0.01f
                    || point.x()>land.maxX()+0.01f || point.z()<land.minZ()-0.01f || point.z()>land.maxZ()+0.01f))
                throw new IllegalArgumentException("Công trình vượt mảnh đất đã mở.");
        }
    }
    List<Item> items() { return items; }
    LandBounds land() { return land; }
    TerrainField terrain() {return terrain;}
    GladeDesign withTerrain(TerrainField field) {return new GladeDesign(items,land,field);}
    boolean hasUpper(long id) {return items.stream().anyMatch(other->other.form().support()==id);}
    float roofRise(Item item) {
        if(hasUpper(item.id())) return 0;
        return item.form().roof();
    }
    float roofLocalHeight(Item host,float x,float z) {
        if(hasUpper(host.id()))return host.height();
        float rise=roofRise(host);var custom=attachedRoof(host.id());
        if(custom!=null){
            if(RoofProfiles.shaped(custom.kind()))return host.height()+RoofProfiles.surface(custom.kind(),host,custom.height(),
                    x-custom.mount().u()*host.width(),z-custom.mount().v()*host.depth(),custom.rotation());
            float span=custom.width()/2+.32f;double angle=Math.toRadians(custom.rotation());
            float across=(float)(Math.cos(angle)*x-Math.sin(angle)*z);
            return host.height()+span*.62f*custom.height()/.5f*Math.max(0,1-Math.abs(across)/span);
        }
        if(host.kind()==Kind.PENTAGON)return host.height()+(rise>.02f?RoofProfiles.surface(Kind.ROOF_PENTAGON,host,rise,x,z,0):0);
        if(host.kind()==Kind.TOWER)return host.height()+rise*Math.max(0,1-(float)Math.sqrt(Math.pow(x/(host.width()/2+.12f),2)+Math.pow(z/(host.depth()/2+.12f),2)));
        var fusion=new BlockFusion(this);
        if(fusion.joined(host.id()))return fusion.surfaceHeight(host,x,z);
        if(host.form().finish().variant()==18) {
            var form=host.form();float w=(host.width()+form.roofLeft()+form.roofRight())/2+.12f,d=(host.depth()+form.roofFront()+form.roofBack())/2+.12f;
            x-=(form.roofRight()-form.roofLeft())/2;z-=(form.roofFront()-form.roofBack())/2;
            float fraction=w>=d?Math.max(Math.abs(z)/d,(Math.abs(x)-(w-d))/d):Math.max(Math.abs(x)/w,(Math.abs(z)-(d-w))/w);
            return host.height()+rise*Math.max(0,1-fraction);
        }
        return host.height()+rise*Math.max(0,1-Math.abs(x)/(host.width()/2+.32f));
    }
    float top(Item item) {return placement(item).elevation()+item.height();}
    float halfWidth() { return land.width()/2; }
    float halfDepth() { return land.depth()/2; }
    long nextId() { return items.stream().mapToLong(Item::id).max().orElse(-1)+1; }

    Placement placement(Item item) {
        if(item.mount()==null) {
            float ground=terrain.height(item.center().x(),item.center().z());
            if(item.form().finish().variant()==18 && item.kind().building() && item.form().support()<0) {
                double angle=Math.toRadians(item.rotation());for(int sx:new int[]{-1,1})for(int sz:new int[]{-1,1}){
                    float x=sx*item.width()*.46f,z=sz*item.depth()*.46f;ground=Math.max(ground,terrain.height(item.center().x()+(float)(Math.cos(angle)*x+Math.sin(angle)*z),item.center().z()+(float)(-Math.sin(angle)*x+Math.cos(angle)*z)));
                }
            }
            float y=ground+item.form().base()+item.form().stilts();
            if(item.form().support()>=0) {
                var support=items.stream().filter(other->other.id()==item.form().support()).findFirst().orElseThrow();
                y=top(support)+item.form().base()+item.form().stilts();
            }
            return new Placement(item.center(),item.rotation(),y,item.width(),item.depth());
        }
        var mount=item.mount();
        var host=items.stream().filter(other->other.id()==mount.hostId()).findFirst().orElseThrow();
        float x=mount.u()*host.width(),z=mount.v()*host.depth();
        double angle=Math.toRadians(host.rotation());
        var center=new Point(host.center().x()+(float)(Math.cos(angle)*x+Math.sin(angle)*z),
                host.center().z()+(float)(-Math.sin(angle)*x+Math.cos(angle)*z));
        float y=mount.lift()*host.height();
        if(item.kind().opening() && mount.lift()>=1) {
            y=roofLocalHeight(host,x,z)+item.height()*.25f;
        }
        if(item.kind().roof()) y=host.height();
        if(item.kind().chimney()) y=roofLocalHeight(host,x,z)-.12f;
        return new Placement(center,host.rotation()+item.rotation(),placement(host).elevation()+Math.max(0,y),
                item.width(),item.depth());
    }

    boolean customRoof(long hostId) {
        return attachedRoof(hostId)!=null;
    }
    Item attachedRoof(long hostId) {
        return items.stream().filter(item->item.kind().roof() && item.mount()!=null && item.mount().hostId()==hostId).findFirst().orElse(null);
    }

    boolean blocksOpening(Item host,float x,float z,float elevation,float clearance) {
        var hostPlacement=placement(host);double angle=Math.toRadians(host.rotation());
        var world=new Point(host.center().x()+(float)(Math.cos(angle)*x+Math.sin(angle)*z),host.center().z()+(float)(-Math.sin(angle)*x+Math.cos(angle)*z));
        float worldY=hostPlacement.elevation()+elevation;
        for(var neighbor:items) if(neighbor.id()!=host.id() && StructureShell.solid(neighbor)) {
            var base=placement(neighbor).elevation();float nx=neighbor.localX(world),nz=neighbor.localZ(world);
            boolean inside=neighbor.kind()==Kind.TOWER?Math.pow(nx/(neighbor.width()/2+.02f),2)+Math.pow(nz/(neighbor.depth()/2+.02f),2)<=1
                    :Math.abs(nx)<=neighbor.width()/2+.03f && Math.abs(nz)<=neighbor.depth()/2+.03f;
            if(inside && worldY>base+.05f && worldY<base+neighbor.height()-.05f) return true;
        }
        for(var part:items) if(part.kind().opening() && part.mount()!=null && part.mount().hostId()==host.id() && !AttachmentRules.hidden(part,this)) {
            var mount=part.mount();
            float dx=x-mount.u()*host.width(),dz=z-mount.v()*host.depth();
            boolean door=part.kind()==Kind.DOOR_ARCH || part.kind()==Kind.DOOR_DOUBLE;
            float y=mount.lift()*host.height()+(door?part.height()/2:0);
            float radius=part.width()/2+clearance;
            if(dx*dx+dz*dz<radius*radius && Math.abs(elevation-y)<part.height()/2+clearance) return true;
        }
        return false;
    }

    GladeDesign add(Item item) {
        var next=new ArrayList<>(items);
        if(item.kind().roof() && item.mount()!=null)
            next.removeIf(old->old.kind().roof() && old.mount()!=null && old.mount().hostId()==item.mount().hostId());
        next.add(item);
        return new GladeDesign(next,land,terrain);
    }

    GladeDesign replace(Item item) {
        var next=new ArrayList<>(items);
        var original=items.stream().filter(old->old.id()==item.id()).findFirst().orElseThrow();
        var queue=new ArrayDeque<Item[]>();queue.add(new Item[]{original,item});
        while(!queue.isEmpty()) {
            var pair=queue.removeFirst();var old=pair[0];var updated=pair[1];
            for(int i=0;i<next.size();i++) if(next.get(i).id()==updated.id()) {next.set(i,updated);break;}
            double angle=Math.toRadians(updated.rotation()-old.rotation());
            for(var child:items) if(child.form().support()==old.id()) {
                float dx=child.center().x()-old.center().x(),dz=child.center().z()-old.center().z();
                var center=new Point(updated.center().x()+(float)(Math.cos(angle)*dx+Math.sin(angle)*dz),
                        updated.center().z()+(float)(-Math.sin(angle)*dx+Math.cos(angle)*dz));
                var moved=new Item(child.id(),child.kind(),center,child.width(),child.depth(),child.height(),child.pitched(),child.points(),
                        child.rotation()+updated.rotation()-old.rotation(),child.mount(),child.form());
                queue.addLast(new Item[]{child,moved});
            }
        }
        return new GladeDesign(next,land,terrain);
    }

    GladeDesign remove(long id) {
        var removed=assemblyIds(id);
        return new GladeDesign(items.stream().filter(item->!removed.contains(item.id())).toList(),land,terrain);
    }
    Set<Long> assemblyIds(long id) {
        var removed=new HashSet<Long>();removed.add(id);
        boolean changed;
        do {changed=false;for(var item:items) if((item.mount()!=null && removed.contains(item.mount().hostId()) || removed.contains(item.form().support())) && removed.add(item.id())) changed=true;} while(changed);
        return removed;
    }

    GladeDesign resizeLand(LandBounds.Edge edge,float coordinate) {
        if(!Float.isFinite(coordinate)) throw new IllegalArgumentException("Tọa độ mép đất không hợp lệ.");
        float usedMinX=Float.POSITIVE_INFINITY,usedMaxX=Float.NEGATIVE_INFINITY;
        float usedMinZ=Float.POSITIVE_INFINITY,usedMaxZ=Float.NEGATIVE_INFINITY;
        for(var item:items) {
            if(item.mount()!=null) continue;
            float x=item.kind().footprint()?item.halfWidth():0;
            float z=item.kind().footprint()?item.halfDepth():0;
            usedMinX=Math.min(usedMinX,item.center().x()-x);
            usedMaxX=Math.max(usedMaxX,item.center().x()+x);
            usedMinZ=Math.min(usedMinZ,item.center().z()-z);
            usedMaxZ=Math.max(usedMaxZ,item.center().z()+z);
            for(var point:item.points()) {
                usedMinX=Math.min(usedMinX,point.x());usedMaxX=Math.max(usedMaxX,point.x());
                usedMinZ=Math.min(usedMinZ,point.z());usedMaxZ=Math.max(usedMaxZ,point.z());
            }
        }
        float minX=land.minX(),maxX=land.maxX(),minZ=land.minZ(),maxZ=land.maxZ();
        switch(edge) {
            case LEFT -> minX=clamp(coordinate,-LandBounds.LIMIT_X,Math.min(maxX-8,usedMinX));
            case RIGHT -> maxX=clamp(coordinate,Math.max(minX+8,usedMaxX),LandBounds.LIMIT_X);
            case BACK -> minZ=clamp(coordinate,-LandBounds.LIMIT_Z,Math.min(maxZ-6,usedMinZ));
            case FRONT -> maxZ=clamp(coordinate,Math.max(minZ+6,usedMaxZ),LandBounds.LIMIT_Z);
        }
        return new GladeDesign(items,new LandBounds(minX,maxX,minZ,maxZ),terrain);
    }

    private static float clamp(float value,float minimum,float maximum) {
        return Math.max(minimum,Math.min(maximum,value));
    }

    boolean nearPath(float x,float z,float radius) {
        for(var item:items) if(item.kind()==Kind.PATH){
            float edge=Math.max(.10f,radius+(PathSurface.width(item)-1)/2);
            for(var point:item.points()) if(Math.hypot(x-point.x(),z-point.z())<edge) return true;
        }
        return false;
    }

    static GladeDesign sample() {
        var house=new Item(0,Kind.HOUSE,new Point(0,0),4.4f,3.2f,2.3f,true,List.of());
        var path=new ArrayList<Point>();
        for(float z=1.5f;z<=5.5f;z+=0.28f) path.add(new Point(0.3f+(float)Math.sin(z)*0.25f,z));
        var design=new GladeDesign(List.of(house,new Item(1,Kind.PATH,path.getFirst(),1,1,1,true,path)));
        for(var point:List.of(new Point(-3.8f,-2.5f),new Point(4.4f,-2.8f),new Point(5.3f,2.6f)))
            design=design.add(new Item(design.nextId(),Kind.TREE,point,1,1,1,true,List.of()));
        for(var point:List.of(new Point(-3,1.8f),new Point(2.8f,2.3f),new Point(3.6f,-0.9f)))
            design=design.add(new Item(design.nextId(),Kind.FLOWERS,point,1,1,1,true,List.of()));
        return design;
    }

    static GladeDesign village() {
        var items=new ArrayList<Item>();
        var trees=List.of(new Point(-3.8f,-4.2f),new Point(8,-3.5f),new Point(8,3.8f));
        int treeIndex=0;
        for(var item:sample().items()) {
            if(item.kind()==Kind.TREE)
                items.add(new Item(item.id(),item.kind(),trees.get(treeIndex++),item.width(),item.depth(),
                        item.height(),item.pitched(),item.points()));
            else items.add(item);
        }
        items.add(new Item(8,Kind.MANOR,new Point(-6,-1),4,3,3.8f,true,List.of()));
        items.add(new Item(9,Kind.TOWER,new Point(6,-2.8f),3,3,4.4f,true,List.of()));
        items.add(new Item(10,Kind.PAVILION,new Point(5.5f,3.8f),2.8f,2.6f,1.9f,true,List.of()));
        items.add(new Item(11,Kind.WATER,new Point(-4.6f,4),4.4f,2.2f,1,true,List.of()));
        items.add(new Item(12,Kind.BRIDGE,new Point(-4.6f,4),1.4f,1,0.8f,true,
                List.of(new Point(-7,4),new Point(-2.2f,4))));
        items.add(new Item(13,Kind.FENCE,new Point(5,-5.5f),1,1,0.9f,true,
                List.of(new Point(3.7f,-5.5f),new Point(8,-5.5f))));
        items.add(new Item(14,Kind.CANAL_HOUSE,new Point(-6,-5.8f),2.7f,2.3f,3.6f,true,List.of()));
        items.add(new Item(15,Kind.BRICK_HOUSE,new Point(-1.8f,-5.8f),3.1f,2.7f,4.6f,true,List.of()));
        return new GladeDesign(items,2);
    }

    static GladeDesign collection() {
        var house=new Item(0,Kind.HOUSE,new Point(-5,-2),4.4f,3.2f,2.6f,true,List.of());
        var cabin=new Item(1,Kind.LOG_CABIN,new Point(4,-3.3f),4.2f,3.2f,2.6f,true,List.of());
        var items=new ArrayList<Item>(List.of(house,cabin));
        items.add(new Item(2,Kind.ROOF_SLATE,house.center(),house.width(),house.depth(),.5f,true,List.of(),0,new Mount(0,0,0,0)));
        items.add(new Item(3,Kind.ROOF_TILE,cabin.center(),cabin.width(),cabin.depth(),.5f,true,List.of(),0,new Mount(1,0,0,0)));
        items.add(new Item(4,Kind.CHIMNEY_BRICK,house.center(),.5f,.5f,1.25f,true,List.of(),0,new Mount(0,.25f,-.2f,0)));
        items.add(new Item(5,Kind.WINDOW_ROUND,house.center(),.85f,.5f,.85f,true,List.of(),270,new Mount(0,-.5f,0,.66f)));
        items.add(new Item(6,Kind.DOOR_DOUBLE,cabin.center(),1.5f,.5f,2.0f,true,List.of(),0,new Mount(1,0,.5f,0)));
        items.add(new Item(7,Kind.DECK,new Point(4,.2f),4.6f,2,.65f,true,List.of()));
        items.add(new Item(8,Kind.STAIRS_WOOD,new Point(4,2.3f),1.6f,2.2f,.65f,true,List.of()));
        items.add(new Item(9,Kind.PERGOLA,new Point(9,2.5f),3,2.8f,2.4f,true,List.of()));
        items.add(new Item(10,Kind.BENCH,new Point(9,1.8f),2,.8f,1.1f,true,List.of()));
        items.add(new Item(11,Kind.WELL,new Point(-.5f,2.5f),1.5f,1.5f,2.1f,true,List.of()));
        items.add(new Item(12,Kind.PLANTER,new Point(-7,1),2,1,.5f,true,List.of()));
        items.add(new Item(13,Kind.PLANTER,new Point(-3,1),2,1,.5f,true,List.of()));
        items.add(new Item(14,Kind.STAIRS_STONE,new Point(-5,2.4f),1.6f,2.4f,.8f,true,List.of()));
        items.add(new Item(15,Kind.BRICK_WALL,new Point(-9,-7),1,1,1.3f,true,
                List.of(new Point(-10,-6),new Point(-8,-7.4f),new Point(-3,-7.4f))));
        items.add(new Item(16,Kind.TIMBER_WALL,new Point(7,-7.4f),1,1,1.4f,true,
                List.of(new Point(2,-7.4f),new Point(10,-7.4f),new Point(11,-6))));
        items.add(new Item(17,Kind.HEDGE,new Point(-10,3),1,1,1.1f,true,
                List.of(new Point(-10,-2),new Point(-10,4),new Point(-7,5))));
        items.add(new Item(18,Kind.CHIMNEY_STONE,cabin.center(),.5f,.5f,1.0f,true,List.of(),0,new Mount(1,.28f,.15f,0)));
        items.add(new Item(19,Kind.WINDOW_ARCH,cabin.center(),.75f,.5f,.9f,true,List.of(),270,new Mount(1,-.5f,0,.65f)));
        items.add(new Item(20,Kind.DOOR_ARCH,house.center(),1.12f,.5f,1.8f,true,List.of(),0,new Mount(0,0,.5f,0)));
        return new GladeDesign(items,2);
    }

    static GladeDesign mechanicsSample() {
        var items=new ArrayList<Item>();var plain=GladeCatalog.plain();
        items.add(new Item(0,Kind.HOUSE,new Point(-3,-1),4,3,2.6f,true,List.of(),0,null,new Form(0,1.5f,0,-1).finish(plain)));
        items.add(new Item(1,Kind.HOUSE,new Point(0,-1),3,3,2.6f,true,List.of(),0,null,new Form(0,1.2f,0,-1).finish(plain)));
        items.add(new Item(2,Kind.TOWER,new Point(5,-3),2.7f,2.7f,4,true,List.of(),0,null,new Form(0,1.8f,0,-1).finish(plain)));
        items.add(new Item(3,Kind.WINDOW_ARCH,new Point(-3,-1),.9f,.5f,1.08f,true,List.of(),0,new Mount(0,0,.5f,.55f)));
        items.add(new Item(4,Kind.WINDOW_ARCH,new Point(-3,-1),.9f,.5f,1.08f,true,List.of(),0,new Mount(0,.28f,.5f,.55f)));
        items.add(new Item(5,Kind.FLOWERS,new Point(-5.3f,-1),1,1,.45f,false,List.of()));
        items.add(new Item(6,Kind.WATER,new Point(5,4),.5f,.5f,1,false,List.of(new Point(5,4)),0,null,new Form(0,0,0,-1).finish(Finish.DEFAULT.variant(20))));
        items.add(samplePath(7,-3,-7,-3,5));items.add(samplePath(8,0,4,11,4));
        var fence=List.of(new Point(-13,4),new Point(-8,4),new Point(-8,9),new Point(-13,9),new Point(-13,4));items.add(new Item(9,Kind.FENCE,fence.getFirst(),1,1,1,false,fence,0,null,new Form(0,0,0,-1).finish(plain)));
        items.add(samplePath(10,-15,7,-6,7));items.add(new Item(11,Kind.WINDOW_ARCH,new Point(0,-1),.9f,.5f,1.08f,true,List.of(),0,new Mount(1,-.2f,0,1)));
        items.add(new Item(12,Kind.HOUSE,new Point(9,-6),3.4f,3,1.6f,false,List.of(),0,null,new Form(0,0,0,-1).finish(plain)));
        var terrain=new TerrainField().brush(5,4,1.8f,1.7f,null).brush(9,-6,2.2f,2.4f,null);
        return new GladeDesign(items,new LandBounds(-17,17,-12.5f,12.5f),terrain);
    }
    private static Item samplePath(long id,float x1,float z1,float x2,float z2) {
        var points=new ArrayList<Point>();for(int i=0;i<=80;i++)points.add(new Point(x1+(x2-x1)*i/80,z1+(z2-z1)*i/80));return new Item(id,Kind.PATH,points.getFirst(),1,1,1,false,points);
    }

    static GladeDesign evolutionSample() {
        var items=new ArrayList<Item>();
        items.add(new Item(0,Kind.HOUSE,new Point(-5,-2),4,3,2.3f,true,List.of(),0,null,new Form(0,1.4f,0,-1)));
        items.add(new Item(1,Kind.HOUSE,new Point(-1,-2),4,3,2.3f,true,List.of(),0,null,new Form(0,1.4f,0,-1)));
        items.add(new Item(2,Kind.BRICK_HOUSE,new Point(6,-3),4.4f,4.0f,1.8f,false,List.of(),0,null,new Form(0,0,0,-1)));
        items.add(new Item(3,Kind.TOWER,new Point(6,-3),2.8f,2.8f,3.1f,true,List.of(),0,null,new Form(0,1.7f,0,2)));
        items.add(new Item(4,Kind.LOG_CABIN,new Point(-6,5),3.6f,2.8f,2.3f,true,List.of(),0,null,new Form(0,1.3f,1.5f,-1)));
        items.add(new Item(5,Kind.STAIRS_WOOD,new Point(-6,7.6f),1.3f,2.4f,1.5f,false,List.of()));
        items.add(new Item(6,Kind.WELL,new Point(8,6),1.5f,1.5f,2.1f,true,List.of()));
        items.add(new Item(7,Kind.TREE,new Point(10,5),1,1,1,true,List.of()));
        items.add(new Item(8,Kind.FLOWERS,new Point(6.5f,5),1,1,1,true,List.of()));
        var points=new ArrayList<Point>();
        for(float z=-.5f;z<=5;z+=.25f) points.add(new Point(-3+(float)Math.sin(z)*.5f,z));
        items.add(new Item(9,Kind.PATH,new Point(-3,0),1,1,1,true,points));
        var field=new TerrainField();
        for(int i=0;i<10;i++) field=field.brush(8,6,4,.42f,null);
        for(int i=0;i<5;i++) field=field.brush(-11,-6,3.4f,.40f,null);
        return new GladeDesign(items,LandBounds.level(3),field);
    }

    void save(Path file) throws IOException { save(file,0); }
    void saveRecovery(Path file,long sequence)throws IOException {if(sequence<=0)throw new IOException("Chỉ mục phục hồi không hợp lệ.");save(file,sequence);}
    private void save(Path file,long sequence) throws IOException {
        if(items.size()>MAX_IMPORTED_ITEMS || items.stream().mapToInt(i->i.points().size()).sum()>MAX_TOTAL_POINTS)throw new IOException("Bản thiết kế vượt ngân sách lưu an toàn: 10.000 công trình / 250.000 điểm vẽ.");
        String terrainData=terrain.encode();TerrainField.decode(terrainData);
        var values=new Properties();
        values.setProperty("version","glade-9");
        values.setProperty("terrain",terrainData);
        values.setProperty("land",land.minX()+","+land.maxX()+","+land.minZ()+","+land.maxZ());
        values.setProperty("count",Integer.toString(items.size()));
        if(sequence>0)values.setProperty("recovery.sequence",Long.toString(sequence));
        for(int i=0;i<items.size();i++) {
            var item=items.get(i);
            String prefix="item."+i+".";
            values.setProperty(prefix+"id",Long.toString(item.id()));
            values.setProperty(prefix+"kind",item.kind().name());
            var form=item.form();values.setProperty(prefix+"form",form.base()+","+form.roof()+","+form.stilts()+","+form.support()+","+form.roofFront()+","+form.roofBack()+","+form.roofLeft()+","+form.roofRight());
            var finish=form.finish();values.setProperty(prefix+"finish",finish.material()+","+finish.color()+","+finish.roofMaterial()+","+finish.roofColor()+","+finish.variant());
            if(item.mount()!=null) {
                var mount=item.mount();
                values.setProperty(prefix+"mount",mount.hostId()+","+mount.u()+","+mount.v()+","+mount.lift());
            }
            values.setProperty(prefix+"shape",item.center().x()+","+item.center().z()+","
                    +item.width()+","+item.depth()+","+item.height()+","+item.pitched()+","+item.rotation());
            values.setProperty(prefix+"points",item.points().stream().map(p->p.x()+","+p.z())
                    .collect(java.util.stream.Collectors.joining(";")));
        }
        WorldFiles.write(file,values);
    }

    static GladeDesign load(Path file) throws IOException {return decode(WorldFiles.read(file));}
    static GladeDesign decode(Properties values)throws IOException {
        String version=values.getProperty("version");
        if(!Set.of("glade-1","glade-2","glade-3","glade-4","glade-5","glade-6","glade-7","glade-8","glade-9").contains(version==null?"":version))
            throw new IllegalArgumentException("Tệp không đúng định dạng Glade.");
        boolean current=Set.of("glade-3","glade-4","glade-5","glade-6","glade-7","glade-8","glade-9").contains(version);
        LandBounds land;
        if(current) {
            String[] bounds=values.getProperty("land","").split(",",-1);
            if(bounds.length!=4) throw new IllegalArgumentException("Mép đất lưu không hợp lệ.");
            land=new LandBounds(Float.parseFloat(bounds[0]),Float.parseFloat(bounds[1]),
                    Float.parseFloat(bounds[2]),Float.parseFloat(bounds[3]));
        } else {
            int landLevel="glade-1".equals(version)?1:Integer.parseInt(values.getProperty("landLevel",""));
            land=LandBounds.level(landLevel);
        }
        int count=Integer.parseInt(values.getProperty("count","-1"));
        if(count<0 || count>MAX_IMPORTED_ITEMS) throw new IllegalArgumentException("Tệp phải có từ 0 đến 10.000 công trình.");
        int totalPoints=0;
        var items=new ArrayList<Item>();
        for(int i=0;i<count;i++) {
            String prefix="item."+i+".";
            var shape=values.getProperty(prefix+"shape","").split(",",-1);
            if(shape.length!=(current?7:6) || !Set.of("true","false").contains(shape[5]))
                throw new IllegalArgumentException("Hình dạng không hợp lệ.");
            var points=new ArrayList<Point>();
            String stroke=values.getProperty(prefix+"points","");
            if(stroke.length()>262144)throw new IllegalArgumentException("Nét vẽ trong tệp quá dài.");
            if(!stroke.isEmpty()) for(var point:stroke.split(";",-1)) {
                var coordinates=point.split(",",-1);
                if(coordinates.length!=2 || points.size()>=4096) throw new IllegalArgumentException("Nét vẽ không hợp lệ.");
                points.add(new Point(Float.parseFloat(coordinates[0]),Float.parseFloat(coordinates[1])));
            }
            totalPoints+=points.size();if(totalPoints>MAX_TOTAL_POINTS)throw new IllegalArgumentException("Tệp có quá nhiều điểm vẽ; tối đa 250.000 điểm.");
            Mount mount=null;
            if(Set.of("glade-4","glade-5","glade-6","glade-7","glade-8","glade-9").contains(version) && values.containsKey(prefix+"mount")) {
                var fields=values.getProperty(prefix+"mount").split(",",-1);
                if(fields.length!=4) throw new IllegalArgumentException("Điểm gắn không hợp lệ.");
                mount=new Mount(Long.parseLong(fields[0]),Float.parseFloat(fields[1]),Float.parseFloat(fields[2]),Float.parseFloat(fields[3]));
            }
            Form form=null;
            if(Set.of("glade-6","glade-7","glade-8","glade-9").contains(version)) {
                var f=values.getProperty(prefix+"form","").split(",");
                if(f.length!=(Set.of("glade-8","glade-9").contains(version)?8:version.equals("glade-7")?6:4)) throw new IllegalArgumentException("Dữ liệu mặt trên/dưới không hợp lệ.");
                form=new Form(Float.parseFloat(f[0]),Float.parseFloat(f[1]),Float.parseFloat(f[2]),Long.parseLong(f[3]),f.length>=6?Float.parseFloat(f[4]):0,f.length>=6?Float.parseFloat(f[5]):0,f.length==8?Float.parseFloat(f[6]):0,f.length==8?Float.parseFloat(f[7]):0,Finish.DEFAULT);
                if(Set.of("glade-8","glade-9").contains(version)){var paint=values.getProperty(prefix+"finish", "original,,original,").split(",",-1);if(paint.length!=4 && paint.length!=5)throw new IllegalArgumentException("Bảng màu lưu không hợp lệ.");form=form.finish(new Finish(paint[0],paint[1],paint[2],paint[3],paint.length==5?Integer.parseInt(paint[4]):0));}
            }
            items.add(new Item(Long.parseLong(values.getProperty(prefix+"id")),Kind.valueOf(values.getProperty(prefix+"kind","")),
                    new Point(Float.parseFloat(shape[0]),Float.parseFloat(shape[1])),Float.parseFloat(shape[2]),
                    Float.parseFloat(shape[3]),Float.parseFloat(shape[4]),Boolean.parseBoolean(shape[5]),points,
                    current?Float.parseFloat(shape[6]):0,mount,form));
        }
        if("glade-4".equals(version)) {
            for(int i=0;i<items.size();i++) {
                var item=items.get(i);
                if(item.kind().roof() && item.mount()!=null) {
                    var host=items.stream().filter(other->other.id()==item.mount().hostId()).findFirst().orElseThrow();
                    items.set(i,new Item(item.id(),item.kind(),item.center(),host.width(),host.depth(),item.height(),
                            item.pitched(),item.points(),item.rotation(),item.mount()));
                }
            }
        }
        return new GladeDesign(items,land,TerrainField.decode(values.getProperty("terrain","")));
    }
}
