package vn.dreamhouse.glade;
import com.jme3.input.*;
import com.jme3.input.event.*;
import com.jme3.system.AppSettings;
import java.nio.file.*;
class CameraNativeProbe extends DirectEditCheck {
 public static void main(String[] args)throws Exception {
  app=new GladeApp();var s=new AppSettings(true);s.setResolution(1360,860);s.setTitle("DreamHouse · Kiểm thử kéo camera");s.setRenderer(AppSettings.LWJGL_OPENGL33);s.setSamples(4);s.setGammaCorrection(true);s.setAudioRenderer(null);app.setSettings(s);app.setShowSettings(false);app.setPauseOnLostFocus(false);app.start();engine(()->true);
  engine(()->{app.getInputManager().deleteMapping("left");var method=GladeApp.class.getDeclaredMethod("cameraButton",int.class,boolean.class);method.setAccessible(true);
   app.getInputManager().addRawInputListener(new RawInputListener(){boolean held;public void beginInput(){}public void endInput(){}public void onJoyAxisEvent(JoyAxisEvent e){}public void onJoyButtonEvent(JoyButtonEvent e){}public void onKeyEvent(KeyInputEvent e){}public void onTouchEvent(TouchEvent e){}public void onMouseMotionEvent(MouseMotionEvent e){System.out.println("MOVE "+e.getX()+","+e.getY()+" delta="+e.getDX()+","+e.getDY());}
    public void onMouseButtonEvent(MouseButtonEvent e){System.out.println("BUTTON "+e.getButtonIndex()+" "+e.isPressed());if(e.getButtonIndex()==0 && e.isPressed())try{if(!held){held=true;method.invoke(app,1,true);}}catch(Exception ex){throw new RuntimeException(ex);}}
   });return true;});
  var file=Path.of("glade/classes/native-camera-status.txt");
  try{for(int i=0;i<1000;i++){Files.writeString(file,"yaw="+field("yaw")+" pitch="+field("elevation")+" rotating="+field("rotating")+" panning="+field("panning"));Thread.sleep(250);}}finally{app.stop();}
 }
}

