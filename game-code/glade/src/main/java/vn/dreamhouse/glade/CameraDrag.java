package vn.dreamhouse.glade;
/** Drag starts exactly at the press coordinate. Normal pointer mode avoids GLFW recenter jumps. */
final class CameraDrag {
 enum Mode {NONE,ORBIT,PAN}
 record Delta(Mode mode,float x,float y) {}
 private Mode mode=Mode.NONE;
 private boolean anchored;
 private float previousX,previousY,pendingX,pendingY;
 void viewport(int width,int height) {}
 void begin(Mode next,float x,float y) {mode=next;previousX=x;previousY=y;pendingX=pendingY=0;anchored=next!=Mode.NONE;}
 void mode(Mode next) {mode=next;pendingX=pendingY=0;anchored=false;}
 Mode mode(){return mode;}
 void reset(){mode=Mode.NONE;pendingX=pendingY=0;anchored=false;}
 void pointer(float x,float y){
  if(mode==Mode.NONE || !Float.isFinite(x) || !Float.isFinite(y))return;
  if(anchored){pendingX+=x-previousX;pendingY+=y-previousY;}
  previousX=x;previousY=y;anchored=true;
 }
 Delta finishBatch(){var result=new Delta(mode,pendingX,pendingY);pendingX=pendingY=0;return result;}
}
