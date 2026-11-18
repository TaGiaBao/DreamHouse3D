package vn.dreamhouse.glade;

import java.util.List;

class AssemblyClipboardCheck {
    static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        var source=BlueprintDataCheck.source();
        var before=source.items();
        var pasted=AssemblyClipboard.paste(source,10,source,0);
        var scene=pasted.design();
        var root=scene.items().stream().filter(i->i.id()==pasted.selectedId()).findFirst().orElseThrow();
        require(scene.items().size()==7,"only root and its two children copied");
        require(scene.assemblyIds(root.id()).size()==3,"support and mount IDs point to cloned parent");
        require(root.form().finish().equals(source.items().stream().filter(i->i.id()==10).findFirst().orElseThrow().form().finish()),"finish retained");
        require(source.items().equals(before),"source snapshot unchanged");
        var second=AssemblyClipboard.paste(source,10,scene,1);
        require(second.selectedId()!=pasted.selectedId(),"repeated paste has fresh IDs");
        require(second.design().items().size()==10,"repeated paste retains earlier assembly");
        try {AssemblyClipboard.paste(source,999,scene,0);throw new AssertionError("missing root accepted");}
        catch(IllegalArgumentException expected){}
        try {AssemblyClipboard.paste(source,30,new GladeDesign(List.of(),2),0);throw new AssertionError("orphan window accepted");}
        catch(IllegalArgumentException expected){}
        var duplicate=AssemblyDuplicator.duplicate(source,source.items().stream().filter(i->i.id()==10).findFirst().orElseThrow());
        require(duplicate.design().assemblyIds(duplicate.selectedId()).size()==3,"duplicate remaps child hierarchy");
        require(duplicate.design().items().size()==7,"duplicate preserves existing assembly");
        var window=source.items().stream().filter(i->i.id()==30).findFirst().orElseThrow();
        var windowCopy=AssemblyDuplicator.duplicate(source,window);
        var added=windowCopy.design().items().stream().filter(i->i.id()==windowCopy.selectedId()).findFirst().orElseThrow();
        require(added.mount().hostId()==10,"duplicate window stays on original host");
        System.out.println("ASSEMBLY_CLIPBOARD_PASS");
    }
}
