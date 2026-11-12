package vn.dreamhouse.glade;
import java.util.*;
/** Catalog templates describe original procedural assets, not images copied from other games. */
final class GladeCatalog {
 record Entry(String tool,String label,int variant){}
 static Entry e(String tool,String label){return new Entry(tool,label,0);}
 static Entry v(String tool,String label,int variant){return new Entry(tool,label,variant);}
 static List<Entry> entries(String group){return switch(group){
  case "architecture" -> List.of(e("HOUSE","Khối vuông"),e("TOWER","Khối trụ"),e("PENTAGON","Khối ngũ giác"),e("WALL","Tường"),e("FENCE","Hàng rào"),e("DECK","Nền / khối craft"),e("STAIRS_STONE","Bậc thang"));
  case "windows" -> List.of(e("WINDOW_ARCH","Cửa sổ vòm"),e("WINDOW_ROUND","Cửa sổ tròn"),e("DOOR_ARCH","Cửa vòm"),e("DOOR_DOUBLE","Cửa đôi"),e("CHIMNEY_BRICK","Ống khói vuông"),e("CHIMNEY_STONE","Ống khói thấp"));
  case "trees" -> List.of(e("TREE","Cây tán tròn"),v("TREE","Cây thông",1),v("TREE","Bạch dương",2),v("TREE","Cây táo",3),v("TREE","Anh đào",4),v("TREE","Bách thon",5),v("TREE","Cây mùa thu",6),v("TREE","Bonsai",7));
  case "garden" -> List.of(e("FLOWERS","Hoa đồng nội"),v("FLOWERS","Oải hương",1),v("FLOWERS","Hoa vàng",2),v("FLOWERS","Hoa hồng",3),e("PLANTER","Bồn hoa gỗ"),v("PLANTER","Bồn hoa hồng",3),e("WELL","Giếng nước"),e("BENCH","Ghế tựa"),e("PERGOLA","Giàn hoa"),e("HEDGE","Rào cây"),v("TREE","Bonsai",7),e("DECK","Sàn sân vườn"));
  case "structures" -> List.of(e("WALL","Tường đá"),e("BRICK_WALL","Tường gạch"),e("TIMBER_WALL","Vách gỗ"),e("FENCE","Hàng rào gỗ"),e("HEDGE","Hàng rào cây"),e("STAIRS_STONE","Cầu thang đá"));
  case "wood" -> List.of(e("LOG_CABIN","Nhà gỗ"),e("DECK","Sàn gỗ"),e("PAVILION","Chòi gỗ"),e("PERGOLA","Giàn hoa"),e("STAIRS_WOOD","Cầu thang gỗ"),e("BRIDGE","Cầu gỗ"),e("BENCH","Ghế sân vườn"),v("HOUSE","Khối craft gỗ",7),v("HOUSE","Bàn craft gỗ",8),v("HOUSE","Giàn craft gỗ",9));
  case "roofs" -> List.of(e("ROOF_FLAT","Mái bằng"),e("ROOF_PYRAMID","Mái chóp bốn dốc"),e("ROOF_PENTAGON","Mái ngũ giác"),e("ROOF_TILE","Mái ngói hai dốc"),e("ROOF_SLATE","Mái đá hai dốc"),v("ROOF_TILE","Mái gỗ",1),v("ROOF_SLATE","Mái kim loại",2),e("CHIMNEY_BRICK","Ống khói gạch"),e("CHIMNEY_STONE","Ống khói đá"),e("ROOF_SHED","Mái một dốc"));
  case "landscape" -> List.of(e("PATH","Đường đá sáng"),v("PATH","Đường gạch ấm",1),v("PATH","Đường đá xám",2),v("PATH","Đường sân vườn",3));
  case "terrain" -> List.of(e("TERRAIN","Đắp / hạ đất"));
  case "water" -> List.of(e("WATER","Đổ nguồn nước"));
  default -> List.of();
 };}
 static GladeDesign.Finish plain(){return new GladeDesign.Finish("plaster","#eadbb4","tile","#c58b62",18);}
 static GladeDesign.Finish finish(Entry entry){int v=entry.variant();if(Set.of("HOUSE","TOWER","PENTAGON","WALL","FENCE","DECK","STAIRS_STONE").contains(entry.tool()) && v==0)return plain();if(Set.of("TERRAIN","RAISE","LOWER","LAND").contains(entry.tool()))return GladeDesign.Finish.DEFAULT;
  if(v>=7 && v<=9 && entry.tool().equals("HOUSE"))return new GladeDesign.Finish("wood","","original","",v);
  if(entry.tool().startsWith("ROOF") && v>0)return new GladeDesign.Finish("original","",v==1?"wood":"metal","",v);
  if(GladeDesign.Kind.valueOf(entry.tool()).building() && v>0)return new GladeDesign.Finish("plaster",new String[]{"","#e9d7ad","#99bdb3","#caa3ad"}[Math.min(v,3)],"original","",v);
  return GladeDesign.Finish.DEFAULT.variant(v);
 }
}
