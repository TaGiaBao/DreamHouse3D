package vn.dreamhouse.glade;

import com.jme3.asset.AssetManager;
import com.jme3.material.*;
import com.jme3.scene.*;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.*;
import com.jme3.texture.plugins.AWTLoader;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

final class GladeHud {
    private final AssetManager assets;
    private final Node node=new Node("Giao diện");
    private final Map<String,Rectangle> buttons=new LinkedHashMap<>();
    private float layer=1;
    void layer(float value) {layer=value;}

    GladeHud(AssetManager assets) { this.assets=assets; }
    Node node() { return node; }
    Rectangle bounds(String id) {return buttons.get(id);}
    void clear() { node.detachAllChildren();buttons.clear();layer=1; }
    void block(String id,int x,int y,int width,int height) {buttons.put(id,new Rectangle(x,y,width,height));}

    void slider(String id,int x,int y,int width,float fraction) {
        buttons.put(id,new Rectangle(x,y,width,32));
        sliderValue(id,fraction);
    }

    void sliderValue(String id,float fraction) {
        var area=buttons.get(id);if(area==null)return;
        node.detachChildNamed(id);
        var image=new BufferedImage(area.width,area.height,BufferedImage.TYPE_INT_ARGB);
        var g=graphics(image);
        g.setColor(new Color(66,52,39));g.fillRoundRect(0,0,area.width,area.height,18,18);
        int thumb=Math.round(12+(area.width-24)*fraction);
        g.setColor(new Color(122,103,80));g.fillRoundRect(10,14,area.width-20,4,4,4);
        g.setColor(new Color(101,200,238));g.fillRoundRect(10,14,thumb-8,4,4,4);
        g.setColor(new Color(136,222,252,94));g.fillOval(thumb-9,7,18,18);
        g.setColor(new Color(108,208,245));g.fillOval(thumb-5,11,10,10);
        g.dispose();surface(image,area.x,area.y,3);
        var slider=node.getChild(node.getQuantity()-1);
        slider.setName(id);slider.setUserData("buttonId",id);
    }

    String hit(float x,float y) {
        var entries=new java.util.ArrayList<>(buttons.entrySet());
        for(int i=entries.size()-1;i>=0;i--) if(entries.get(i).getValue().contains(x,y)) return entries.get(i).getKey();
        return null;
    }

    void panel(String title,String detail,int x,int y,int w,int h) {
        var image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        var pen=graphics(image);
        pen.setColor(new Color(255,247,222,240));
        pen.fillRoundRect(0,0,w,h,24,24);
        pen.setColor(new Color(187,155,106));
        pen.drawRoundRect(1,1,w-3,h-3,24,24);
        pen.setFont(new Font("Segoe UI",Font.BOLD,w<300?16:20));
        pen.setColor(new Color(92,72,42));
        pen.drawString(title,16,29);
        pen.setFont(new Font("Segoe UI",Font.PLAIN,12));
        pen.setColor(new Color(119,104,76));
        pen.drawString(detail,16,50);
        pen.dispose();
        surface(image,x,y);
    }

    void helpPanel(int x,int y,int w,int h) {
        var image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        var g=graphics(image);
        g.setColor(new Color(56,47,33,90));
        g.fillRoundRect(0,8,w,h-8,28,28);
        g.setColor(new Color(255,247,222,250));
        g.fillRoundRect(1,1,w-3,h-14,26,26);
        g.setColor(new Color(187,155,106));
        g.drawRoundRect(1,1,w-3,h-14,26,26);
        g.setFont(new Font("Segoe UI",Font.BOLD,22));
        g.setColor(new Color(92,72,42));
        g.drawString("Hướng dẫn DreamHouse",24,38);
        g.setFont(new Font("Segoe UI",Font.PLAIN,15));
        g.setColor(new Color(91,78,56));
        String[] lines={
                "Bấm các nhóm để mở thư viện; chọn mẫu rồi đặt trên đất.",
                "Bốn mũi tên ngang: rộng/sâu. Mũi tên trên: tạo/bỏ mái.",
                "Điểm trắng: đầu trên/dưới đổi cốt; giữa nâng/hạ cả khối.",
                "Mái có hai tay kéo ngang hai bên; cung ở chân để xoay.",
                "Nút Đi bộ: ngắm gần mặt đất; Esc trở lại xây dựng.",
                "Giữ phải: xoay. Giữ giữa: kéo cảnh. WASD / mũi tên: đi.",
                "Ctrl+C/X/V: sao chép/cắt/dán. Ctrl+Z/Y: hoàn tác/làm lại."
        };
        for(int i=0;i<lines.length;i++) g.drawString(lines[i],24,76+i*31);
        g.setFont(new Font("Segoe UI",Font.BOLD,12));
        g.setColor(new Color(119,104,76));
        g.drawString("F1 mở lại · Học qua thao tác để thực hành từng bước.",24,h-76);
        g.dispose();
        surface(image,x,y,5);
    }

    void workbench(String detail,int x,int y,int w,int h) {
        var image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        var g=graphics(image);
        g.setColor(new Color(56,47,33,65));g.fillRoundRect(0,7,w,h-7,30,30);
        g.setPaint(new GradientPaint(0,0,Color.decode("#b68c5b"),0,h,Color.decode("#79593f")));
        g.fillRoundRect(0,0,w,h-8,30,30);
        g.setPaint(new GradientPaint(0,4,Color.decode("#fff6dd"),0,h,Color.decode("#e6d6b1")));
        g.fillRoundRect(4,4,w-8,h-17,25,25);
        g.setColor(new Color(255,255,242,200));g.drawRoundRect(7,7,w-15,h-24,22,22);
        g.setColor(Color.decode("#e1cda6"));g.fillRoundRect(14,49,w-28,h-91,16,16);
        if(w>=1068) {
            GladeIcons.paint(g,"tools",15,6,38);
            g.setFont(new Font("Segoe UI",Font.BOLD,16));g.setColor(Color.decode("#675239"));g.drawString("HỘP DỤNG CỤ",59,30);
        }
        g.setFont(new Font("Segoe UI",Font.PLAIN,12));g.setColor(Color.decode("#786b52"));
        while(g.getFontMetrics().stringWidth(detail)>w-45 && detail.length()>4) detail=detail.substring(0,detail.length()-4)+"…";
        g.drawString(detail,21,h-23);
        g.dispose();surface(image,x,y);
    }

    void toolCard(String id,String label,String icon,int x,int y,int w,int h,boolean selected) {
        var image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        var g=graphics(image);
        g.setColor(new Color(76,53,31,50));g.fillRoundRect(2,6,w-4,h-6,19,19);
        g.setColor(Color.decode(selected?"#527c55":"#b39973"));g.fillRoundRect(1,1,w-2,h-7,19,19);
        g.setPaint(new GradientPaint(0,4,Color.decode(selected?"#dfefbc":"#fff9e9"),0,h-8,
                Color.decode(selected?"#a5c584":"#eddfc2")));
        g.fillRoundRect(4,4,w-8,h-14,15,15);
        g.setColor(new Color(255,255,248,210));g.drawRoundRect(6,6,w-13,h-19,13,13);
        int size=Math.min(72,h-33);
        GladeIcons.paint(g,icon,(w-size)/2,5,size);
        g.setFont(new Font("Segoe UI",Font.BOLD,12));g.setColor(Color.decode(selected?"#35583c":"#64553c"));
        while(g.getFontMetrics().stringWidth(label)>w-12 && g.getFont().getSize()>9)
            g.setFont(g.getFont().deriveFont((float)g.getFont().getSize()-1));
        var metrics=g.getFontMetrics();
        g.drawString(label,(w-metrics.stringWidth(label))/2,h-19);
        if(selected) {
            g.setColor(Color.decode("#527c55"));g.fillOval(w-22,9,13,13);
            g.setColor(Color.decode("#f3f6d8"));g.setStroke(new BasicStroke(1.8f));
            g.drawLine(w-19,15,w-16,18);g.drawLine(w-16,18,w-12,12);
        }
        g.dispose();surface(image,x,y);buttons.put(id,new Rectangle(x,y,w,h));
        node.getChild(node.getQuantity()-1).setUserData("buttonId",id);
    }

    void veil(int width,int height) {
        var image=new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);image.setRGB(0,0,new Color(39,48,32,100).getRGB());surface(image,0,0);
        node.getChild(node.getQuantity()-1).setLocalScale(width,height,1);
    }

    void circleChip(String id,String tint,String pattern,String caption,int x,int y,int size,boolean selected) {
        var image=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);var g=graphics(image);
        g.setColor(new Color(40,45,29,70));g.fillOval(1,4,size-2,size-4);
        g.setColor(selected?new Color(255,255,238):new Color(231,228,201));g.fillOval(0,0,size-1,size-4);
        g.setColor(Color.decode(tint));g.fillOval(4,4,size-9,size-12);
        var clip=g.getClip();g.clip(new java.awt.geom.Ellipse2D.Float(4,4,size-9,size-12));
        g.setColor(new Color(70,65,45,100));g.setStroke(new BasicStroke(1.7f));
        if(pattern.equals("brick") || pattern.equals("stone") || pattern.equals("tile") || pattern.equals("slate")) {
            for(int row=0;row<4;row++){int yy=6+row*9;g.drawLine(3,yy,size-3,yy);for(int xx=4+row%2*9;xx<size;xx+=18)g.drawLine(xx,yy,xx,yy+9);}
        } else if(pattern.equals("wood")) {for(int xx=8;xx<size;xx+=9){g.drawLine(xx,2,xx,size);g.drawArc(xx-4,8,9,22,60,260);}}
        g.setClip(clip);
        if(!caption.isEmpty()){g.setFont(new Font("Segoe UI",Font.BOLD,Math.max(9,size/5)));g.setColor(new Color(65,65,45));g.drawString(caption,(size-g.getFontMetrics().stringWidth(caption))/2,size/2+3);}
        g.dispose();surface(image,x,y);buttons.put(id,new Rectangle(x,y,size,size));node.getChild(node.getQuantity()-1).setUserData("buttonId",id);
    }
    void tooltip(String text,int x,int y,int width) {
        var image=new BufferedImage(width,45,BufferedImage.TYPE_INT_ARGB);var g=graphics(image);
        g.setColor(new Color(44,59,43,210));g.fillRoundRect(0,0,width,43,16,16);g.setColor(new Color(255,249,222));g.setFont(new Font("Segoe UI",Font.PLAIN,13));
        g.drawString(text,12,27);g.dispose();surface(image,x,y);
    }
    void notice(String text,int x,int y,int width){
        var image=new BufferedImage(width,66,BufferedImage.TYPE_INT_ARGB);var g=graphics(image);
        g.setColor(new Color(255,246,218,245));g.fillRoundRect(0,0,width,64,18,18);g.setColor(new Color(177,143,96));g.drawRoundRect(1,1,width-3,61,17,17);
        g.setColor(new Color(87,71,46));g.setFont(new Font("Segoe UI",Font.PLAIN,13));var lines=new java.util.ArrayList<String>();String line="";
        for(String word:text.split("\\s+")){String next=line.isEmpty()?word:line+" "+word;if(g.getFontMetrics().stringWidth(next)>width-28 && !line.isEmpty()){lines.add(line);line=word;}else line=next;}if(!line.isEmpty())lines.add(line);
        for(int i=0;i<Math.min(3,lines.size());i++)g.drawString(lines.get(i),14,20+i*18);
        g.dispose();surface(image,x,y);node.setUserData("feedback",text);
    }

    void categoryTile(String id,String label,String icon,String tint,int x,int y,int w,int h,boolean selected) {
        var image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);var g=graphics(image);
        var color=Color.decode(tint);
        g.setColor(new Color(62,42,24,65));g.fillRoundRect(1,7,w-2,h-7,30,30);
        g.setPaint(new GradientPaint(0,1,new Color(165,119,66),0,h,new Color(107,75,43)));g.fillRoundRect(0,0,w,h-7,28,28);
        g.setColor(new Color(236,201,142));g.fillRoundRect(2,2,w-4,h-12,26,26);
        g.setPaint(new GradientPaint(0,4,new Color(255,250,227),0,h,new Color(226,203,155)));g.fillRoundRect(5,5,w-10,h-18,23,23);
        g.setPaint(new GradientPaint(0,8,new Color(255,249,224),w,h-18,new Color((color.getRed()+255)/2,(color.getGreen()+245)/2,(color.getBlue()+220)/2)));
        g.fillRoundRect(8,8,w-16,h-24,20,20);
        g.setColor(new Color(255,255,241,210));g.drawRoundRect(7,7,w-15,h-23,21,21);
        int size=Math.min(w-12,h-27);GladeIcons.paint(g,icon,(w-size)/2,4,size);
        g.setFont(new Font("Segoe UI",Font.BOLD,w<78?10:12));g.setColor(Color.decode("#493d30"));
        g.drawString(label,(w-g.getFontMetrics().stringWidth(label))/2,h-17);
        if(selected) {g.setColor(new Color(111,144,75));g.setStroke(new BasicStroke(3));g.drawRoundRect(3,3,w-7,h-12,24,24);}
        g.dispose();surface(image,x,y);buttons.put(id,new Rectangle(x,y,w,h));
        node.getChild(node.getQuantity()-1).setUserData("buttonId",id);
    }

    void blueprintCard(String id,String label,BlueprintModule module,int x,int y,int w,int h,boolean selected){
        var image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        var g=graphics(image);
        g.setColor(Color.decode(selected?"#bdd298":"#eadbbc"));
        g.fillRoundRect(0,0,w,h-4,18,18);
        g.setColor(Color.decode(selected?"#5a7c49":"#ac916c"));
        g.drawRoundRect(1,1,w-3,h-6,17,17);
        var scene=module.design();
        float scale=Math.min(16,64/Math.max(scene.land().width(),scene.land().depth()));
        for(var item:scene.items()){
            var position=scene.placement(item);
            var material=item.form().finish();
            String tint=item.mount()!=null?"#8e6b4d":material.color().isEmpty()?"#c3885e":material.color();
            int cx=Math.round(46+position.center().x()*scale),cy=Math.round(40+position.center().z()*scale-position.elevation()*scale*.45f);
            int iw=Math.max(3,Math.round(item.width()*scale)),ih=Math.max(3,Math.round(item.depth()*scale*.65f));
            g.setColor(Color.decode(tint));g.fillRoundRect(cx-iw/2,cy-ih/2,iw,ih,4,4);
            g.setColor(new Color(86,58,35,130));g.drawRoundRect(cx-iw/2,cy-ih/2,iw,ih,4,4);
        }
        g.setColor(Color.decode("#493d30"));
        g.setFont(new Font("Segoe UI",Font.BOLD,11));
        while(g.getFontMetrics().stringWidth(label)>w-91 && label.length()>2)label=label.substring(0,label.length()-2)+"…";
        g.drawString(label,83,32);
        g.setFont(new Font("Segoe UI",Font.PLAIN,11));
        g.drawString(scene.items().size()+" cấu kiện",83,51);
        g.dispose();surface(image,x,y);buttons.put(id,new Rectangle(x,y,w,h));
        node.getChild(node.getQuantity()-1).setUserData("buttonId",id);
    }

    void iconButton(String id,String label,String icon,int x,int y,int w,int h,boolean selected) {
        button(id,"",x,y,w,h,selected);
        var image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        var g=graphics(image);int size=Math.min(36,h-7);
        GladeIcons.paint(g,icon,7,1,size);
        g.setFont(new Font("Segoe UI",Font.BOLD,12));g.setColor(Color.decode("#615039"));
        g.drawString(label,size+10,(h+g.getFontMetrics().getAscent())/2-5);
        g.dispose();surface(image,x,y);
        node.getChild(node.getQuantity()-1).setUserData("buttonId",id);
    }

    void menuButton(int x,int y,boolean open) {
        button("sceneMenu","",x,y,52,40,open);
        var image=new BufferedImage(52,40,BufferedImage.TYPE_INT_ARGB);
        var g=graphics(image);
        g.setColor(Color.decode("#6c573b"));
        g.setStroke(new BasicStroke(3,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        for(int line=0;line<3;line++) g.drawLine(16,11+line*6,36,11+line*6);
        g.dispose();surface(image,x,y);
        node.getChild(node.getQuantity()-1).setUserData("buttonId","sceneMenu");
    }

    void brand(int x,int y) {
        var image=new BufferedImage(340,72,BufferedImage.TYPE_INT_ARGB);
        var g=graphics(image);
        g.setColor(new Color(62,40,22,65));g.fillRoundRect(2,8,278,53,26,26);
        g.setPaint(new GradientPaint(0,0,new Color(186,135,77),0,58,new Color(101,64,36)));g.fillRoundRect(0,2,278,53,24,24);
        g.setStroke(new BasicStroke(2));g.setColor(new Color(233,191,125));g.drawRoundRect(3,5,271,46,20,20);
        var font=new Font("Georgia",Font.BOLD,35);var glyph=font.createGlyphVector(g.getFontRenderContext(),"DreamHouse");var shape=glyph.getOutline(12,40);
        g.setStroke(new BasicStroke(4,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(new Color(91,57,32));g.draw(shape);
        g.setPaint(new GradientPaint(0,13,new Color(255,251,226),0,42,new Color(243,216,163)));g.fill(shape);
        g.setFont(new Font("Segoe UI",Font.BOLD,9));g.setColor(new Color(83,78,49));g.drawString("XÂY NÊN GÓC BÌNH YÊN CỦA BẠN",14,69);
        g.dispose();surface(image,x,y);
    }

    void launcher(String id,String label,String icon,int x,int y,boolean selected) {
        toolCard(id,label,icon,x,y,96,94,selected);
    }

    void stat(String label,String value,String icon,int x,int y) {
        node.detachChildNamed(label);
        int w=152,h=49;
        var image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);var g=graphics(image);
        g.setColor(new Color(84,64,42,45));g.fillRoundRect(1,4,w-2,h-4,25,25);
        g.setColor(Color.decode("#b79c70"));g.fillRoundRect(1,1,w-2,h-5,25,25);
        g.setPaint(new GradientPaint(0,2,Color.decode("#fff5d4"),0,h,Color.decode("#e8cb92")));
        g.fillRoundRect(3,3,w-6,h-10,22,22);
        GladeIcons.paint(g,icon,6,4,36);
        g.setColor(Color.decode("#7b694b"));g.setFont(new Font("Segoe UI",Font.BOLD,9));g.drawString(label,48,17);
        g.setColor(Color.decode("#594a33"));g.setFont(new Font("Segoe UI",Font.BOLD,13));g.drawString(value,48,33);
        g.dispose();surface(image,x,y);
        node.getChild(node.getQuantity()-1).setName(label);
    }

    void update(float tpf,float mouseX,float mouseY) {
        String hover=hit(mouseX,mouseY);
        for(var child:node.getChildren()) {
            String id=child.getUserData("buttonId");
            if(id==null) continue;
            float target=id.equals(hover)?1.035f:1;
            float scale=child.getLocalScale().x;
            scale+=(target-scale)*Math.min(1,tpf*18);
            var bounds=buttons.get(id);
            child.setLocalScale(scale,scale,1);
            Float base=child.getUserData("hudLayer");
            float depth=id.equals("helpClose")?6:(base==null?1:base)+(id.equals(hover)?.2f:0);
            child.setLocalTranslation(bounds.x-bounds.width*(scale-1)/2,bounds.y-bounds.height*(scale-1)/2,depth);
        }
    }

    void text(String text,int x,int y,int w,int h,int size) {
        var image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        var pen=graphics(image);
        pen.setFont(new Font("Segoe UI",Font.BOLD,size));
        pen.setColor(new Color(255,246,219));
        pen.drawString(text,2,size+3);
        pen.setColor(new Color(78,93,65));
        pen.drawString(text,1,size+1);
        pen.dispose();
        surface(image,x,y);
    }

    void button(String id,String label,int x,int y,int w,int h,boolean selected) {
        var image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        var pen=graphics(image);
        pen.setColor(new Color(83,61,35,70));
        pen.fillRoundRect(1,5,w-2,h-5,20,20);
        pen.setColor(new Color(120,89,50));
        pen.fillRoundRect(1,1,w-2,h-6,19,19);
        pen.setColor(new Color(229,198,142));
        pen.fillRoundRect(3,3,w-6,h-10,17,17);
        pen.setPaint(new GradientPaint(0,4,selected?new Color(218,237,167):new Color(255,248,221),
                0,h-8,selected?new Color(150,187,103):new Color(234,213,172)));
        pen.fillRoundRect(5,5,w-10,h-15,15,15);
        pen.setColor(new Color(255,255,237,180));
        pen.drawRoundRect(6,6,w-13,h-18,14,14);
        pen.setFont(new Font("Segoe UI",Font.BOLD,h>60 && w>140?15:13));
        pen.setColor(new Color(86,75,44));
        var metrics=pen.getFontMetrics();
        String[] lines=label.split("\n");
        for(int i=0;i<lines.length;i++) {
            int lineY=(h-lines.length*metrics.getHeight())/2+metrics.getAscent()+i*metrics.getHeight()-3;
            pen.drawString(lines[i],(w-metrics.stringWidth(lines[i]))/2,lineY);
        }
        pen.dispose();
        surface(image,x,y);
        buttons.put(id,new Rectangle(x,y,w,h));
        node.getChild(node.getQuantity()-1).setUserData("buttonId",id);
    }

    private static Graphics2D graphics(BufferedImage image) {
        var graphics=image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return graphics;
    }

    private void surface(BufferedImage image,int x,int y) {
        surface(image,x,y,layer);
    }

    private void surface(BufferedImage image,int x,int y,float z) {
        var texture=new Texture2D(new AWTLoader().load(image,true));
        texture.setMagFilter(Texture.MagFilter.Bilinear);
        texture.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        var material=new Material(assets,"Common/MatDefs/Misc/Unshaded.j3md");
        material.setTexture("ColorMap",texture);
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        var quad=new Geometry("Nút",new Quad(image.getWidth(),image.getHeight()));
        quad.setMaterial(material);
        quad.setLocalTranslation(x,y,z);quad.setUserData("hudLayer",z);
        node.attachChild(quad);
    }
}
