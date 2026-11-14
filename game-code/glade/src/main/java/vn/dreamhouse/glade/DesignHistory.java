package vn.dreamhouse.glade;

import java.util.ArrayDeque;

final class DesignHistory {
    private final ArrayDeque<GladeDesign> undo=new ArrayDeque<>();
    private final ArrayDeque<GladeDesign> redo=new ArrayDeque<>();

    void record(GladeDesign current) {
        undo.addLast(current);
        if(undo.size()>60)undo.removeFirst();
        redo.clear();
    }

    GladeDesign step(GladeDesign current,boolean forward) {
        var from=forward?redo:undo;
        var to=forward?undo:redo;
        if(from.isEmpty())return null;
        to.addLast(current);
        return from.removeLast();
    }

    void clear() {
        undo.clear();
        redo.clear();
    }
}
