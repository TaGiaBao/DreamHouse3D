package vn.dreamhouse.glade;
class LeftGestureCheck {
 public static void main(String[] args){
  check(LeftGesture.click(80_000_000L,3),"short click selects despite jitter");
  check(!LeftGesture.click(80_000_000L,30),"fast swipe must not select");
  check(!LeftGesture.click(500_000_000L,2),"long hold must not become click");
  check(LeftGesture.intent(60_000_000L,30,false)==LeftGesture.Intent.WAIT,"unselected fast swipe waits");
  check(LeftGesture.intent(180_000_000L,30,false)==LeftGesture.Intent.ORBIT,"held drag on unselected block or background orbits");
  check(LeftGesture.intent(180_000_000L,3,false)==LeftGesture.Intent.WAIT,"small jitter does not rotate");
  check(LeftGesture.intent(40_000_000L,30,true)==LeftGesture.Intent.MOVE,"selected body drag moves without orbit");
  check(LeftGesture.intent(40_000_000L,30,false)!=LeftGesture.Intent.MOVE,"background drag cannot move selected object");
  System.out.println("LEFT_GESTURE_PASS: click, jitter, fast swipe, hold, selected move, background orbit");
 }
 static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
}
