package vn.dreamhouse.glade;

import java.awt.*;
import java.awt.geom.*;

final class GladeIcons {
    private GladeIcons() {}
    static void paint(Graphics2D source,String key,int x,int y,int size) {
        int variant=0;
        if(key.contains("@")){var parts=key.split("@",2);key=parts[0];variant=Integer.parseInt(parts[1]);}
        var g=(Graphics2D)source.create();
        g.translate(x,y);g.scale(size/100.0,size/100.0);
        g.setStroke(new BasicStroke(2.2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g.setColor(new Color(63,57,37,30));g.fillOval(15,80,72,12);
        if(key.equals("TREE") && variant>0) {
            round(g,46,52,10,37,4,variant==2?"#ded9c4":"#96724d");
            if(variant==1 || variant==5) {
                for(int i=0;i<3;i++){int cx=50,half=variant==5?16:34-i*7,yy=66-i*19;
                    poly(g,i%2==0?"#74965c":"#91b17a",cx-half,yy,cx,yy-34,cx+half,yy);}
            } else {
                String light=variant==4?"#edc0cf":variant==6?"#e5c37e":"#b4ca81",dark=variant==4?"#bd8ca8":variant==6?"#b18e53":"#6f955a";
                if(variant==7){round(g,29,76,44,16,5,"#c3a875");round(g,48,39,7,39,3,"#98764f");for(int i=0;i<3;i++)orb(g,16+i*20,25-i%2*13,33,light,dark);}
                else {orb(g,17,27,44,light,dark);orb(g,39,19,43,light,dark);orb(g,30,8,40,light,dark);}
                if(variant==3)for(int i=0;i<7;i++)orb(g,29+i%3*15,24+i/3*16,10,"#e7a48a","#b9614c");
                if(variant==2){g.setColor(Color.decode("#8b8c7b"));g.drawLine(47,66,52,65);g.drawLine(49,78,55,77);}
            }
            g.dispose();return;
        }
        if(key.equals("FLOWERS") && variant>0) {
            for(int i=0;i<5;i++){int stemX=22+i*14,yy=25+i%2*14;round(g,stemX,yy,3,58-yy,2,"#8aa55d");orb(g,stemX-8,yy-10,20,variant==1?"#bfacd4":variant==2?"#f0d98e":"#e5a6b5",variant==1?"#8b72a5":variant==2?"#c5a353":"#ba7e95");}
            g.dispose();return;
        }
        switch(key) {
            case "walk" -> {
                orb(g,42,9,20,"#f7e7bd","#ae8b60");
                g.setColor(Color.decode("#806e4e"));g.setStroke(new BasicStroke(9,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                g.drawLine(50,35,43,58);g.drawLine(47,43,30,54);g.drawLine(48,39,65,49);g.drawLine(43,58,28,82);g.drawLine(43,58,62,79);
            }
            case "viewAll" -> {
                g.setColor(Color.decode("#567976"));g.setStroke(new BasicStroke(5));
                g.drawRoundRect(18,19,65,61,8,8);g.drawLine(30,31,17,18);g.drawLine(70,31,83,18);g.drawLine(30,68,17,81);g.drawLine(70,68,83,81);
                house(g,"HOUSE");
            }
            case "help" -> {
                orb(g,16,10,72,"#fff3c6","#d0b47c");g.setFont(new Font("Segoe UI",Font.BOLD,57));g.setColor(Color.decode("#856d49"));g.drawString("?",35,68);
            }
            case "dockBuild" -> {
                house(g,"MANOR");
                round(g,9,34,22,52,4,"#f5dfad");poly(g,"#ba7751",5,36,20,7,35,36);
                round(g,15,47,7,15,3,"#749e9d");
                g.setColor(Color.decode("#8e6849"));g.drawLine(10,37,10,82);g.drawLine(30,37,30,82);
            }
            case "dockPath" -> {
                var path=new Path2D.Float();path.moveTo(77,12);path.curveTo(101,33,4,33,26,49);path.curveTo(35,57,88,53,75,67);path.curveTo(67,77,30,80,17,89);
                g.setStroke(new BasicStroke(13,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(Color.decode("#95744c"));g.draw(path);
                g.setStroke(new BasicStroke(8,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setPaint(new GradientPaint(0,0,Color.decode("#ffe7a6"),0,95,Color.decode("#dbb879")));g.draw(path);
            }
            case "dockWater" -> {
                var drop=new Path2D.Float();drop.moveTo(50,7);drop.curveTo(45,28,22,47,23,64);drop.curveTo(23,96,78,96,78,64);drop.curveTo(78,47,56,25,50,7);
                g.setColor(Color.decode("#4c8d9e"));g.setStroke(new BasicStroke(3));g.draw(drop);
                g.setPaint(new GradientPaint(28,25,Color.decode("#dcf6ed"),72,90,Color.decode("#4b9bb3")));g.fill(drop);
                g.setColor(new Color(255,255,248,210));g.fillOval(31,49,10,23);g.fillOval(43,78,22,4);
            }
            case "dockWindow" -> {
                round(g,15,17,72,65,5,"#926744");round(g,22,24,58,49,3,"#83b5c0");
                g.setPaint(new GradientPaint(24,25,Color.decode("#d1e9dd"),77,72,Color.decode("#6a9fa8")));g.fillRect(24,26,53,43);
                round(g,47,24,7,49,2,"#dab485");round(g,22,45,58,6,2,"#dab485");round(g,12,12,78,10,4,"#c79a68");round(g,17,76,68,12,4,"#bd8659");
                for(int i=0;i<5;i++){orb(g,21+i*12,69-i%2*4,13,"#a4bd70","#73965b");orb(g,23+i*12,65-i%2*4,9,i%2==0?"#efbaaa":"#e6d492","#b18e6e");}
            }
            case "dockFlower" -> {
                round(g,48,46,6,43,3,"#688e47");
                poly(g,"#6e9c4e",49,80,18,57,22,78,49,90);poly(g,"#94b567",51,81,82,53,76,79,51,90);
                g.setPaint(new GradientPaint(25,13,Color.decode("#f6f4be"),72,61,Color.decode("#c7db91")));g.fillOval(20,16,62,39);
                g.setColor(Color.decode("#88a866"));g.setStroke(new BasicStroke(3));g.drawOval(29,24,43,22);g.drawArc(37,29,24,13,0,290);
            }
            case "palette" -> {
                g.setColor(Color.decode("#ba946a"));g.fillOval(12,15,77,70);
                g.setColor(Color.decode("#f0d7a2"));g.fillOval(15,13,72,66);
                for(int i=0;i<5;i++) orb(g,24+i*10,24+(i%2)*22,13,new String[]{"#e7bd64","#d97965","#82ad81","#82bac2","#b293bd"}[i],"#9a7958");
                g.rotate(.55,66,55);round(g,62,39,9,48,4,"#a57c51");orb(g,57,26,19,"#f6ecd3","#c7b18d");
            }
            case "RAISE","LOWER","LEVEL" -> {
                poly(g,"#a5845d",12,57,53,31,88,54,88,70,46,91,12,73);
                poly(g,"#a5bb78",12,57,53,31,88,54,46,76);
                if(key.equals("LEVEL")) {round(g,25,32,50,7,3,"#fff2ce");round(g,25,44,50,5,3,"#e2cfaa");}
                else {
                    boolean up=key.equals("RAISE");round(g,46,20,9,35,3,"#fff2ce");
                    if(up) poly(g,"#fff2ce",34,27,50,10,66,27);else poly(g,"#fff2ce",34,45,50,62,66,45);
                }
            }
            case "blueprints" -> {
                round(g,22,16,55,27,12,"#5e7c74");
                round(g,14,35,75,47,12,"#6d9691");
                round(g,15,30,73,28,12,"#9dbfb0");
                round(g,23,35,57,10,6,"#c8d8bc");round(g,43,50,17,24,4,"#dfc38b");
                round(g,35,68,33,16,3,"#ece0b5");
                g.setColor(Color.decode("#8baea4"));g.drawLine(41,75,63,75);
            }
            case "hideRoof" -> {
                g.setColor(Color.decode("#c2a177"));g.fillOval(9,31,82,40);
                g.setColor(Color.decode("#f8edc9"));g.fillOval(15,35,70,32);
                orb(g,37,37,27,"#a5bc9a","#5e7d6b");
                g.setColor(Color.decode("#e0c595"));g.setStroke(new BasicStroke(6));g.drawLine(23,79,77,20);
            }
            case "HOUSE","MANOR","CANAL_HOUSE","BRICK_HOUSE","LOG_CABIN","architecture","structures" -> {
                house(g,key);
                if(key.equals("LOG_CABIN")) {g.setColor(Color.decode("#95744e"));for(int i=0;i<5;i++) g.drawLine(24,53+i*5,53,62+i*5);}
            }
            case "PENTAGON" -> {
                poly(g,"#c6a478",19,42,46,53,46,81,19,68);
                poly(g,"#ad805b",46,53,79,37,79,65,46,81);
                poly(g,"#f1d2a1",19,42,35,23,57,17,79,37,46,53);
                poly(g,"#6e9890",25,51,35,56,35,66,25,61);
            }
            case "ROOF_FLAT" -> {
                poly(g,"#a88d70",12,51,51,66,85,48,85,58,51,76,12,61);
                poly(g,"#e4cba5",12,51,46,31,85,48,51,66);
            }
            case "ROOF_CURVED" -> {
                poly(g,"#3e4b51",8,65,20,58,48,23,66,40,88,52,78,68,49,78,23,73);
                poly(g,"#74848a",8,64,20,57,48,23,48,66,23,72);
                g.setColor(Color.decode("#a9b7b8"));
                g.drawLine(48,23,66,40);
                g.drawArc(10,45,40,25,205,112);
            }
            case "ROOF_SHED" -> {
                poly(g,"#ac6947",15,56,59,80,88,65,43,40);
                poly(g,"#db9462",15,51,59,75,88,60,43,35);
                g.setColor(Color.decode("#f2b885"));
                for(int i=0;i<4;i++)g.drawLine(20+i*7,54-i*4,63+i*7,77-i*4);
            }
            case "ROOF_PYRAMID","ROOF_PENTAGON" -> {
                if(key.equals("ROOF_PENTAGON")){
                    poly(g,"#dc9561",13,54,30,39,50,15,53,70);
                    poly(g,"#ae6344",50,15,86,46,72,61,53,70);
                    poly(g,"#bd7652",50,15,72,30,86,46);
                }else{
                    poly(g,"#e3a377",14,56,45,20,52,74);
                    poly(g,"#b66b48",45,20,86,52,52,74);
                }
            }
            case "ROOF_TILE","ROOF_SLATE" -> {
                boolean slate=key.equals("ROOF_SLATE");
                poly(g,slate?"#5b737c":"#a96947",10,63,46,25,88,42,54,83);
                for(int row=0;row<5;row++) for(int col=0;col<4;col++) {
                    int a=13+row*7+col*10,b=62-row*7+col*4;
                    poly(g,slate?(col%2==0?"#8fa6aa":"#78939c"):(col%2==0?"#e1a06e":"#cb8757"),a,b,a+6,b-7,a+17,b-3,a+11,b+4);
                }
                g.setColor(Color.decode(slate?"#b4c8c2":"#e9b987"));g.drawLine(46,24,88,41);
            }
            case "CHIMNEY_BRICK","CHIMNEY_STONE" -> {
                boolean brick=key.equals("CHIMNEY_BRICK");
                poly(g,brick?"#b47c60":"#b2ab92",32,33,59,25,73,35,73,79,47,89,32,78);
                for(int row=0;row<5;row++) {g.setColor(Color.decode("#e0c9a6"));g.drawLine(34,43+row*7,47,48+row*7);g.drawLine(47,48+row*7,71,39+row*7);}
                poly(g,"#dbc6a2",27,32,58,21,78,32,47,44);
                poly(g,"#4f5950",35,32,58,25,69,31,47,39);
                for(int i=0;i<3;i++) {g.setColor(new Color(246,240,220,220-i*35));g.fillOval(46+i*5,14-i*9,13-i*2,13-i*2);}
            }
            case "WINDOW_ARCH","WINDOW_ROUND","DOOR_ARCH","DOOR_DOUBLE" -> {
                boolean round=key.equals("WINDOW_ROUND"),door=key.startsWith("DOOR");
                g.setPaint(new GradientPaint(20,20,Color.decode("#e6cda1"),80,80,Color.decode("#b18b59")));
                if(round) g.fillOval(17,19,67,67);else g.fillRoundRect(25,13,52,76,25,25);
                g.setPaint(new GradientPaint(30,20,Color.decode(door?"#91ad8e":"#afd4d0"),80,90,Color.decode(door?"#4e7e71":"#648f9d")));
                if(round) g.fillOval(25,27,51,51);else g.fillRoundRect(32,21,38,61,19,19);
                g.setColor(Color.decode("#ecd8ad"));g.drawLine(50,29,50,76);
                if(!door) g.drawLine(31,52,70,52);
                if(key.equals("WINDOW_ARCH")) {round(g,15,38,10,42,3,"#77966d");round(g,77,38,10,42,3,"#77966d");}
                if(door) {orb(g,57,57,6,"#f4d27b","#c89a49");round(g,22,84,58,6,3,"#cdbd96");}
                if(key.equals("DOOR_DOUBLE")) {round(g,36,43,10,29,3,"#789d82");round(g,55,43,10,29,3,"#789d82");}
            }
            case "STAIRS_STONE","STAIRS_WOOD" -> {
                boolean wood=key.equals("STAIRS_WOOD");
                for(int i=0;i<5;i++) {
                    int a=13+i*9,b=75-i*11;
                    poly(g,wood?"#9c7951":"#a3a18b",a,b,a+28,b+9,a+28,b+17,a,b+8);
                    poly(g,wood?"#d3b181":"#d8cfb0",a,b,a+17,b-8,a+45,b,a+28,b+9);
                }
                if(wood) {g.setColor(Color.decode("#9c7951"));g.setStroke(new BasicStroke(4));g.drawLine(17,53,53,14);g.drawLine(17,53,17,77);g.drawLine(53,14,53,39);}
            }
            case "DECK","PERGOLA" -> {
                poly(g,"#a67e54",13,66,55,45,88,65,47,87);
                for(int i=0;i<6;i++) {g.setColor(Color.decode("#e0be89"));g.drawLine(18+i*6,66+i*3,57+i*5,48+i*3);}
                for(int[] p:new int[][]{{18,64},{77,64},{47,80}}) round(g,p[0],p[1],5,13,2,"#986e48");
                if(key.equals("PERGOLA")) {
                    for(int[] p:new int[][]{{18,25},{77,25},{47,41}}) round(g,p[0],p[1],5,43,2,"#b18a59");
                    for(int i=0;i<5;i++) {g.setColor(Color.decode("#cba474"));g.setStroke(new BasicStroke(5));g.drawLine(12+i*9,26-i*4,45+i*9,46-i*4);}
                    for(int i=0;i<4;i++) orb(g,30+i*11,17+i%2*8,14,"#b7cc80","#7d9e5a");
                }
            }
            case "CRAFT_TABLE" -> {
                for(int[] leg:new int[][]{{21,48},{72,42},{44,67},{80,57}})round(g,leg[0],leg[1],6,29,2,"#94704b");
                poly(g,"#c39b69",12,45,58,25,91,44,45,68);
                poly(g,"#aa8055",12,45,45,68,91,44,91,53,45,77,12,54);
                g.setColor(Color.decode("#e3c18b"));for(int i=0;i<5;i++)g.drawLine(21+i*7,43+i*4,60+i*6,27+i*4);
            }
            case "PLANTER" -> {
                poly(g,"#ac8158",16,52,66,43,87,57,87,77,37,89,16,74);
                poly(g,"#735f44",19,51,65,46,82,56,37,68);
                for(int i=0;i<5;i++) {int a=26+i*11,b=44+i%2*8;g.setColor(Color.decode("#7e9d60"));g.drawLine(a,b,a,67);
                    for(int petal=0;petal<5;petal++) {g.setColor(Color.decode(i%2==0?"#e7a39c":"#e7c36f"));g.fillOval(a-5+(int)(Math.cos(petal*1.257)*6),b-5+(int)(Math.sin(petal*1.257)*6),10,10);}}
            }
            case "WELL" -> {
                round(g,26,53,49,32,12,"#b9ad8c");g.setColor(Color.decode("#e3d3ac"));g.fillOval(23,47,55,24);
                g.setColor(Color.decode("#6f9799"));g.fillOval(32,52,37,14);
                round(g,28,22,5,39,3,"#9f784a");round(g,69,22,5,39,3,"#9f784a");
                poly(g,"#ce895c",17,29,49,7,84,29,51,41);poly(g,"#edba83",17,29,49,7,51,41);
            }
            case "BENCH" -> {
                for(int i=0;i<3;i++) {g.setColor(Color.decode("#c49b65"));g.setStroke(new BasicStroke(6));g.drawLine(20,38+i*9,78,24+i*9);}
                for(int a:new int[]{26,70}) {round(g,a,49,6,32,2,"#8f6b47");}
                poly(g,"#ddb985",16,59,70,45,88,57,33,73);
                g.setColor(Color.decode("#9c774f"));g.drawLine(24,61,77,49);g.drawLine(28,66,81,54);
            }
            case "HEDGE" -> {
                for(int i=0;i<4;i++) orb(g,10+i*17,36-i*4,34,"#b3c977","#698e50");
            }
            case "TOWER" -> tower(g);
            case "PAVILION" -> {
                for(int a:new int[]{26,69}) { round(g,a,37,6,44,2,"#b78a55"); }
                poly(g,"#c9a36e",15,78,64,68,89,79,40,92);
                poly(g,"#b86443",12,39,50,12,89,39,51,54);
                poly(g,"#edaa69",12,39,50,12,51,54);
            }
            case "TREE","landscape" -> {
                round(g,46,48,10,37,5,"#9b704a");
                for(int[] p:new int[][]{{24,30,38},{46,22,39},{35,9,37},{15,43,35},{48,41,37}})
                    orb(g,p[0],p[1],p[2],"#b4cc6e","#5e914f");
            }
            case "FLOWERS" -> {
                for(int i=0;i<3;i++) {
                    int cx=29+i*22,cy=43-i%2*17;
                    g.setColor(Color.decode("#6b914e"));g.setStroke(new BasicStroke(5));g.drawLine(cx,cy,cx,81);
                    g.fillOval(cx-17,cy+19,19,8);
                    for(int p=0;p<5;p++) {
                        g.setColor(Color.decode(i%2==0?"#e79583":"#ecc471"));
                        g.fillOval(cx-7+(int)(Math.cos(p*1.257)*9),cy-7+(int)(Math.sin(p*1.257)*9),14,14);
                    }
                    orb(g,cx-5,cy-5,10,"#fff1a3","#d8a344");
                }
            }
            case "WATER" -> {
                g.setColor(Color.decode("#b5a17b"));g.fillOval(10,32,82,48);
                g.setPaint(new GradientPaint(0,35,Color.decode("#a1d7d0"),0,80,Color.decode("#4a99ab")));
                g.fillOval(14,35,74,39);g.setColor(Color.decode("#dcf5e6"));g.drawArc(26,44,42,18,10,140);
                g.setColor(Color.decode("#739c57"));g.fillOval(62,41,17,10);
            }
            case "LAND" -> {
                poly(g,"#a17a53",13,48,55,29,88,49,88,66,47,85,13,67);
                poly(g,"#a8c776",13,48,55,29,88,49,47,68);
                g.setColor(Color.decode("#f4f1ce"));g.drawLine(30,48,68,48);g.drawLine(48,37,48,61);
                poly(g,"#f4f1ce",24,48,34,42,34,54);poly(g,"#f4f1ce",75,48,65,42,65,54);
            }
            case "PATH" -> {
                for(int i=0;i<5;i++) {
                    int a=23+(i%2)*20,b=71-i*13;
                    poly(g,"#a29c81",a,b,a+28,b-5,a+36,b+3,a+7,b+10);
                    poly(g,"#d6d1ae",a,b-3,a+28,b-8,a+36,b,a+7,b+7);
                }
            }
            case "WALL","BRICK_WALL","TIMBER_WALL","FENCE","BRIDGE" -> {
                boolean stone=key.equals("WALL") || key.equals("BRICK_WALL");
                if(key.equals("BRIDGE")) {
                    for(int i=0;i<7;i++) poly(g,"#d4ad77",13+i*10,65-i/2,23+i*10,62-i/2,23+i*10,75-i/2,13+i*10,78-i/2);
                }
                if(stone) {
                    round(g,17,33,68,44,4,"#aba58e");
                    for(int row=0;row<3;row++) for(int col=0;col<3;col++)
                        round(g,18+col*22+(row%2)*2,35+row*13,20,11,3,key.equals("BRICK_WALL")?((row+col)%2==0?"#d8a17e":"#b98060"):(row+col)%2==0?"#d8ceb0":"#c1b69a");
                    round(g,13,28,75,10,3,"#e5d8b8");
                } else {
                    if(key.equals("TIMBER_WALL")) round(g,22,30,58,44,3,"#be9a6a");
                    for(int i=0;i<3;i++) round(g,20+i*27,26-i*3,7,53,3,"#a77a4d");
                    round(g,19,38,63,6,3,"#dcb980");round(g,19,59,63,6,3,"#c69a64");
                }
            }
            case "ERASE" -> {
                var t=g.getTransform();g.rotate(-0.55,50,50);
                round(g,28,22,43,59,8,"#a2695c");round(g,29,23,41,35,7,"#eda795");
                round(g,29,57,41,20,5,"#f7e9c8");g.setTransform(t);
            }
            case "SELECT" -> {
                poly(g,"#7b674b",29,12,76,53,55,58,43,81);
                poly(g,"#fff2c9",32,15,72,51,53,53,43,74);
                round(g,63,65,24,7,3,"#c7a66a");round(g,72,56,7,25,3,"#c7a66a");
            }
            case "tools" -> {
                round(g,33,16,34,24,9,"#704c35");round(g,39,21,22,14,5,"#e8c895");
                round(g,12,34,76,50,13,"#80523a");round(g,13,29,74,32,12,"#bb8554");
                round(g,17,35,66,9,6,"#d4a16d");round(g,44,51,14,20,4,"#edc46e");
            }
            case "photo" -> {
                round(g,33,22,28,17,6,"#b78d61");round(g,13,33,76,50,12,"#b98d63");
                round(g,17,36,68,12,6,"#dfbd8b");orb(g,33,41,36,"#9ec5bd","#425e61");
                orb(g,42,48,16,"#e2f1dc","#75a4a7");
            }
            case "light" -> {
                for(int i=0;i<8;i++) { double a=i*Math.PI/4;g.setColor(Color.decode("#e5b458"));g.setStroke(new BasicStroke(5));
                    g.drawLine(50+(int)(Math.cos(a)*31),49+(int)(Math.sin(a)*31),50+(int)(Math.cos(a)*40),49+(int)(Math.sin(a)*40)); }
                orb(g,27,26,46,"#fff1ab","#e3b452");
            }
            default -> {
                round(g,20,25,61,56,8,"#b3c2a4");round(g,27,31,47,43,5,"#f2e4bd");
                g.setColor(Color.decode("#7b997e"));g.drawLine(36,44,66,44);g.drawLine(36,54,59,54);
            }
        }
        g.dispose();
    }

    private static void house(Graphics2D g,String key) {
        boolean canal=key.equals("CANAL_HOUSE"),brick=key.equals("BRICK_HOUSE");
        boolean tall=canal || brick || key.equals("MANOR");
        int top=tall?32:45;
        poly(g,brick?"#a9765c":canal?"#71b4ae":"#d9bb85",22,top,61,top-10,81,top+3,81,78,43,90,22,77);
        poly(g,brick?"#b98064":canal?"#91c7b9":"#f1d7a4",22,top,55,top+9,55,86,22,77);
        if(!key.equals("HOUSE")) {
            poly(g,brick?"#607e88":"#c47c53",16,top+2,39,top-26,61,top-10,85,top+1,56,top+14);
            poly(g,brick?"#7e9a9d":"#e6a16d",16,top+2,39,top-26,56,top+14);
        } else poly(g,"#ead7af",20,top,60,top-11,83,top+3,55,top+13);
        g.setColor(Color.decode(brick?"#e0c6a2":"#f9e4b7"));
        g.drawLine(23,top+6,54,top+16);
        for(int row=0;row<(tall?2:1);row++) {
            round(g,29,top+12+row*16,10,13,3,"#5d827d");
            round(g,63,top+8+row*17,10,13,3,"#f0d8a1");round(g,65,top+10+row*17,6,9,2,"#648e99");
        }
        round(g,41,65,11,20,5,"#5c7f70");
        if(brick) for(int y=top+8;y<78;y+=8) {g.setColor(Color.decode("#d1a488"));g.drawLine(57,y,79,y-6);}
        if(!key.equals("HOUSE")) round(g,65,top-18,8,22,2,brick?"#a96951":"#b7aa8b");
    }
    private static void tower(Graphics2D g) {
        round(g,27,31,47,52,12,"#b6ad91");
        g.setColor(Color.decode("#e0d2b1"));g.fillOval(27,24,47,18);
        for(int row=0;row<4;row++) {g.setColor(Color.decode("#938e79"));g.drawLine(29,43+row*10,72,43+row*10);}
        poly(g,"#63818b",18,33,50,5,82,33,72,40,29,40);
        poly(g,"#9bb5b2",18,33,50,5,48,39);
        round(g,44,51,13,17,6,"#5c8d98");round(g,43,70,14,17,6,"#776951");
    }
    private static void orb(Graphics2D g,int x,int y,int size,String light,String dark) {
        g.setPaint(new RadialGradientPaint(new Point2D.Float(x+size*.33f,y+size*.26f),size*.8f,
                new float[]{0,1},new Color[]{Color.decode(light),Color.decode(dark)}));
        g.fillOval(x,y,size,size);
    }
    private static void round(Graphics2D g,int x,int y,int w,int h,int r,String color) {
        g.setColor(Color.decode(color));g.fillRoundRect(x,y,w,h,r,r);
    }
    private static void poly(Graphics2D g,String color,int... points) {
        var path=new Path2D.Float();path.moveTo(points[0],points[1]);
        for(int i=2;i<points.length;i+=2) path.lineTo(points[i],points[i+1]);path.closePath();
        g.setColor(Color.decode(color));g.fill(path);
        g.setColor(new Color(78,61,42,95));g.draw(path);
    }
}
