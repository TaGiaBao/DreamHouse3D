package vn.dreamhouse.glade;

import java.util.*;

final class BuildGuide {
    enum Goal { BUILD, SHAPE, PAINT, BLUEPRINT, SAVE }
    private final EnumSet<Goal> completed=EnumSet.noneOf(Goal.class);
    boolean active;
    void start(){completed.clear();active=true;}
    void mark(Goal goal){if(active)completed.add(goal);}
    int count(){return completed.size();}
    Goal next(){return Arrays.stream(Goal.values()).filter(g->!completed.contains(g)).findFirst().orElse(null);}
    String title(){var next=next();return next==null?"Bạn đã sẵn sàng xây tự do":switch(next){case BUILD->"1. Dựng căn nhà đầu tiên";case SHAPE->"2. Thử biến đổi khối";case PAINT->"3. Chọn vật liệu của bạn";case BLUEPRINT->"4. Cất mẫu công trình";case SAVE->"5. Lưu thung lũng";};}
    List<String> lines(){var next=next();return next==null?List.of("Tiếp tục sáng tạo theo ý bạn.","F1 để xem lại hướng dẫn bất cứ lúc nào."):switch(next){
        case BUILD->List.of("Bấm Xây dựng → chọn Khối vuông.","Bấm đất để đặt; kéo để chọn kích thước.");
        case SHAPE->List.of("Chọn nhà, kéo mũi tên hoặc điểm trắng.","Kéo mái xuống hết để thử dạng sân thượng.");
        case PAINT->List.of("Bấm Bảng màu, rồi bấm vào căn nhà.","Chọn vật liệu hoặc một màu bạn thích.","Esc để xem bước tiếp theo.");
        case BLUEPRINT->List.of("Chọn nhà → mở Hộp bản thiết kế.","Bấm Lưu cụm chọn và đặt tên cho mẫu.");
        case SAVE->List.of("Mở Hộp bản thiết kế → Lưu thung lũng.","Chọn chỗ lưu; tự lưu không thay lưu chủ động.");
    };}
    void observe(GladeDesign before,GladeDesign after){
        if(!active)return;var previous=new HashMap<Long,GladeDesign.Item>();before.items().forEach(i->previous.put(i.id(),i));
        for(var item:after.items()){
            var old=previous.get(item.id());if(old==null){if(item.kind().building())mark(Goal.BUILD);continue;}
            if(!old.form().finish().equals(item.form().finish()))mark(Goal.PAINT);
            if(item.kind().footprint() && (old.width()!=item.width() || old.depth()!=item.depth() || old.height()!=item.height() || !old.form().equals(item.form()) && old.form().finish().equals(item.form().finish()) || old.rotation()!=item.rotation()))mark(Goal.SHAPE);
        }
    }
}
