package vn.dreamhouse.glade;
import com.jme3.math.*;
import com.jme3.system.AppSettings;
import java.util.*;
class RightOrbitCheck extends ProceduralMechanicsCheck {
 public static void main(String[] args)throws Exception {
  app=new GladeApp();var settings=new AppSettings(true);settings.setResolution(1360,860);settings.setRenderer(AppSettings.LWJGL_OPENGL33);settings.setAudioRenderer(null);settings.setTitle("DreamHouse · Kiểm tra chuột phải xoay cảnh");app.setSettings(settings);app.setShowSettings(false);app.setPauseOnLostFocus(false);app.start();
  try {engine(()->true);Thread.sleep(400);fixture(new GladeDesign(List.of(block(0,0,0,4,3,2.6f,1.4f)),2));esc();
   float yaw=(Float)field("yaw"),pitch=(Float)field("elevation");var focus=((Vector3f)field("focus")).clone();
   mouse(1050,600,true);Thread.sleep(200);mouse(1180,680,null);mouse(1180,680,false);
   ok((Float)field("yaw")==yaw && (Float)field("elevation")==pitch,"left blank drag cannot orbit");
   click(new Vector3f(0,1,1.5f));ok(Objects.equals(field("selected"),0L),"left click still selects block");
   var before=item(0).center();var p=screen(new Vector3f(0,1,1.5f));mouse((int)p.x,(int)p.y,true);mouse((int)p.x+75,(int)p.y+15,null);mouse((int)p.x+75,(int)p.y+15,false);
   ok(item(0).center().distance(before)>.1f,"selected block still moves with left drag");ok((Float)field("yaw")==yaw,"moving does not rotate camera");
   var scene=design();right(800,500,true);mouse(920,560,null);right(920,560,false);
   ok(Math.abs((Float)field("yaw")-yaw)>.1f && Math.abs((Float)field("elevation")-pitch)>.01f,"right drag rotates yaw and pitch");ok(design().equals(scene),"right drag does not edit blocks");
   float stopped=(Float)field("yaw");mouse(1000,620,null);ok((Float)field("yaw")==stopped && !(Boolean)field("rotating"),"right release stops orbit without drift");
   ok(((Vector3f)field("focus")).distance(focus)<.001f,"right orbit keeps stable scene center");
   System.out.println("RIGHT_ORBIT_PASS: left selection/movement without orbit, right yaw/pitch, stable center, release without drift");
  }finally {app.stop();}
 }
}

