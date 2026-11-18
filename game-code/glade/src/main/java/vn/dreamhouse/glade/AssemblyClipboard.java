package vn.dreamhouse.glade;

import com.jme3.math.FastMath;
import java.util.ArrayList;
import java.util.HashMap;

final class AssemblyClipboard {
    record Paste(GladeDesign design, long selectedId) {}

    static Paste paste(GladeDesign source, long rootId, GladeDesign design, int previousPastes) {
        var root = source.items().stream().filter(item -> item.id() == rootId).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Công trình sao chép không còn tồn tại."));
        var ids = source.assemblyIds(root.id());
        var mapping = new HashMap<Long, Long>();
        long id = design.nextId();
        for (var part : source.items()) {
            if (ids.contains(part.id())) mapping.put(part.id(), id++);
        }

        float dx = 1.2f * (previousPastes + 1);
        float dz = dx;
        float minX = Float.POSITIVE_INFINITY, maxX = -minX, minZ = minX, maxZ = -minX;
        for (var part : source.items()) {
            if (!ids.contains(part.id()) || part.mount() != null) continue;
            minX = Math.min(minX, part.center().x() - part.halfWidth());
            maxX = Math.max(maxX, part.center().x() + part.halfWidth());
            minZ = Math.min(minZ, part.center().z() - part.halfDepth());
            maxZ = Math.max(maxZ, part.center().z() + part.halfDepth());
        }
        if (Float.isFinite(minX)) {
            dx = FastMath.clamp(dx, design.land().minX() - minX + .1f, design.land().maxX() - maxX - .1f);
            dz = FastMath.clamp(dz, design.land().minZ() - minZ + .1f, design.land().maxZ() - maxZ - .1f);
        }

        var result = new ArrayList<>(design.items());
        for (var part : source.items()) {
            if (!ids.contains(part.id())) continue;
            var mount = part.mount();
            if (mount != null) {
                Long parent = mapping.get(mount.hostId());
                long hostId = mount.hostId();
                if (parent == null && design.items().stream().noneMatch(host -> host.id() == hostId)) {
                    throw new IllegalArgumentException("Cần có nhà gốc để dán cấu kiện này.");
                }
                mount = new GladeDesign.Mount(parent == null ? hostId : parent, mount.u(), mount.v(), mount.lift());
            }
            var form = part.form().support(mapping.getOrDefault(part.form().support(), -1L));
            var points = new ArrayList<GladeDesign.Point>();
            for (var point : part.points()) points.add(new GladeDesign.Point(point.x() + dx, point.z() + dz));
            var center = new GladeDesign.Point(part.center().x() + dx, part.center().z() + dz);
            result.add(new GladeDesign.Item(mapping.get(part.id()), part.kind(), center, part.width(), part.depth(),
                    part.height(), part.pitched(), points, part.rotation(), mount, form));
        }
        return new Paste(new GladeDesign(result, design.land(), design.terrain()), mapping.get(root.id()));
    }
}
