// Merge a group of adjacent polygons into one by cancelling shared internal edges.
// Works when adjacent polygons share exact coincident boundary vertices (common in
// admin boundary datasets built from one consistent source layer).

function pointKey(p, precision) {
  return `${p[0].toFixed(precision)}|${p[1].toFixed(precision)}`;
}

function dissolveRings(rings, precision = 6) {
  // collect directed edges from every ring (ring is closed: first point === last point)
  const forward = new Map(); // "keyA>keyB" -> {a, b}
  for (const ring of rings) {
    for (let i = 0; i < ring.length - 1; i++) {
      const a = ring[i];
      const b = ring[i + 1];
      const ka = pointKey(a, precision);
      const kb = pointKey(b, precision);
      if (ka === kb) continue;
      forward.set(`${ka}>${kb}`, { a, b, ka, kb });
    }
  }

  // drop edges whose exact reverse also exists (shared internal border, both directions cancel)
  const kept = [];
  for (const [key, edge] of forward.entries()) {
    const reverseKey = `${edge.kb}>${edge.ka}`;
    if (forward.has(reverseKey)) continue;
    kept.push(edge);
  }

  // chain remaining edges into closed loops
  const outgoing = new Map(); // ka -> [edge, ...]
  for (const edge of kept) {
    if (!outgoing.has(edge.ka)) outgoing.set(edge.ka, []);
    outgoing.get(edge.ka).push(edge);
  }

  const usedEdgeIds = new Set();
  const edgeId = (e) => `${e.ka}>${e.kb}`;
  const resultRings = [];

  for (const startEdge of kept) {
    if (usedEdgeIds.has(edgeId(startEdge))) continue;
    const loopPoints = [startEdge.a];
    let current = startEdge;
    usedEdgeIds.add(edgeId(current));
    let guard = 0;
    while (guard < 200000) {
      loopPoints.push(current.b);
      if (current.kb === startEdge.ka) break; // closed the loop
      const options = outgoing.get(current.kb) || [];
      const next = options.find((e) => !usedEdgeIds.has(edgeId(e)));
      if (!next) break; // dead end - incomplete topology, stop here
      usedEdgeIds.add(edgeId(next));
      current = next;
      guard++;
    }
    if (loopPoints.length >= 4) resultRings.push(loopPoints);
  }

  return { rings: resultRings, droppedInternalEdgeCount: forward.size - kept.length };
}

module.exports = { dissolveRings, pointKey };
