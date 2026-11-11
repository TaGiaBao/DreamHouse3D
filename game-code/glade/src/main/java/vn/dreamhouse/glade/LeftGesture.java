package vn.dreamhouse.glade;

/** Separates a click from an intentional held orbit; time is supplied for deterministic checks. */
final class LeftGesture {
    static final float SLOP=8;
    static final long HOLD_NANOS=120_000_000L;
    enum Intent { WAIT, MOVE, ORBIT }
    static Intent intent(long heldNanos,float pixels,boolean selected) {
        if(pixels<=SLOP)return Intent.WAIT;
        if(selected)return Intent.MOVE;
        return heldNanos>=HOLD_NANOS?Intent.ORBIT:Intent.WAIT;
    }
    static boolean click(float pixels){return pixels<=SLOP;}
    static boolean click(long heldNanos,float pixels){return heldNanos<=300_000_000L && click(pixels);}
}
