const SVG_NS = 'http://www.w3.org/2000/svg';

// ─────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────

function createSvgElement(tagName, attributes = {}) {
  const element = document.createElementNS(SVG_NS, tagName);
  for (const [key, value] of Object.entries(attributes)) {
    element.setAttribute(key, value);
  }
  return element;
}

function normalizeStates(states) {
  return [...new Set(states)].filter(Boolean);
}

// ─────────────────────────────────────────────
// Layout engine
// ─────────────────────────────────────────────

function computeLayout(definition, width, height) {
  return layoutByLevels(definition, width, height);
}

function bfsOrder(states, initial, transitions) {
  const visited = new Map();
  const queue = [initial];
  let order = 0;
  visited.set(initial, order++);

  while (queue.length) {
    const current = queue.shift();
    const trans = transitions[current] || {};
    for (const dest of Object.values(trans).flat()) {
      if (!visited.has(dest)) {
        visited.set(dest, order++);
        queue.push(dest);
      }
    }
  }
  for (const s of states) {
    if (!visited.has(s)) visited.set(s, order++);
  }
  return visited;
}

function buildLevelMap(states, initial, transitions) {
  const levelMap = new Map();
  const queue = [initial];
  levelMap.set(initial, 0);

  while (queue.length) {
    const current = queue.shift();
    const currentLevel = levelMap.get(current) ?? 0;
    const trans = transitions[current] || {};
    for (const dest of Object.values(trans).flat()) {
      if (!levelMap.has(dest)) {
        levelMap.set(dest, currentLevel + 1);
        queue.push(dest);
      }
    }
  }

  let maxLevel = 0;
  for (const level of levelMap.values()) {
    if (level > maxLevel) maxLevel = level;
  }

  for (const state of states) {
    if (!levelMap.has(state)) {
      levelMap.set(state, maxLevel + 1);
    }
  }

  return levelMap;
}

function layoutByLevels(definition, baseWidth, baseHeight) {
  const states = normalizeStates(definition.states || []);
  const orderMap = bfsOrder(states, definition.initialState, definition.transitions || {});
  const levelMap = buildLevelMap(states, definition.initialState, definition.transitions || {});

  const levels = new Map();
  for (const state of states) {
    const level = levelMap.get(state) ?? 0;
    if (!levels.has(level)) levels.set(level, []);
    levels.get(level).push(state);
  }

  for (const list of levels.values()) {
    list.sort((a, b) => (orderMap.get(a) ?? 99) - (orderMap.get(b) ?? 99));
  }

  const levelKeys = Array.from(levels.keys()).sort((a, b) => a - b);
  const maxPerLevel = Math.max(...levelKeys.map((key) => levels.get(key).length), 1);
  const levelCount = Math.max(levelKeys.length, 1);

  const isDeterministic = definition.automataType === 'AFD' || definition.automataType === 'AFD_MIN';
  const padX = Math.max(70, baseWidth * 0.08);
  const padY = Math.max(60, baseHeight * 0.08);
  const minXSpacing = isDeterministic ? 200 : 150;
  const minYSpacing = isDeterministic ? 150 : 120;

  const layoutWidth = Math.max(baseWidth, padX * 2 + (maxPerLevel - 1) * minXSpacing);
  const layoutHeight = Math.max(baseHeight, padY * 2 + (levelCount - 1) * minYSpacing);

  const stepY = levelCount > 1 ? (layoutHeight - padY * 2) / (levelCount - 1) : 0;
  const positions = [];

  levelKeys.forEach((level, index) => {
    const nodes = levels.get(level);
    const stepX = nodes.length > 1 ? (layoutWidth - padX * 2) / (nodes.length - 1) : 0;
    const stagger = isDeterministic ? (index % 2 === 0 ? 0 : Math.min(stepX * 0.35, 90)) : 0;
    nodes.forEach((state, i) => {
      positions.push({
        state,
        x: padX + stepX * i + stagger,
        y: padY + stepY * index
      });
    });
  });

  return {
    positions,
    width: layoutWidth,
    height: layoutHeight
  };
}

// Ex4 AFD: U-shaped layout to reduce overlapping return arrows
function layoutEx4Afd(states, definition, width, height) {
  const orderMap = bfsOrder(states, definition.initialState, definition.transitions || {});
  const acceptingSet = new Set(definition.acceptingStates || []);
  const errorStates = states.filter((state) => state.includes('qE') || state === '∅');
  const acceptingStates = states.filter((state) => acceptingSet.has(state) && !errorStates.includes(state));
  const topStates = states
    .filter((state) => !acceptingSet.has(state) && !errorStates.includes(state))
    .sort((a, b) => (orderMap.get(a) ?? 99) - (orderMap.get(b) ?? 99));

  const padX = Math.max(64, width * 0.08);
  const usableW = Math.max(width - padX * 2, 1);
  const topY = height * 0.24;
  const botY = height * 0.78;
  const topStep = topStates.length > 1 ? usableW / (topStates.length - 1) : 0;

  const positions = topStates.map((state, i) => ({
    state,
    x: padX + topStep * i,
    y: topY
  }));

  const placed = new Set(topStates);
  const leftX = padX;
  const rightX = padX + topStep * Math.max(topStates.length - 1, 0);

  if (acceptingStates[0]) {
    positions.push({ state: acceptingStates[0], x: leftX, y: botY });
    placed.add(acceptingStates[0]);
  }

  if (errorStates[0]) {
    positions.push({ state: errorStates[0], x: rightX, y: botY });
    placed.add(errorStates[0]);
  }

  const leftovers = states.filter((state) => !placed.has(state));
  if (leftovers.length) {
    const span = Math.max(rightX - leftX, 1);
    const step = leftovers.length > 1 ? span / (leftovers.length - 1) : 0;
    leftovers.forEach((state, i) => {
      positions.push({ state, x: leftX + step * i, y: botY });
    });
  }

  return positions;
}

// Ex5/Ex6 AFD: simple left-to-right chain, dead state below if present
function layoutLinearChain(states, definition, width, height) {
  const orderMap = bfsOrder(states, definition.initialState, definition.transitions || {});
  const mainStates = states
    .filter(s => s !== '∅')
    .sort((a, b) => (orderMap.get(a) ?? 99) - (orderMap.get(b) ?? 99));
  const deadStates = states.filter(s => s === '∅');

  const midY = height * 0.42;
  const botY = height * 0.78;
  const padX = Math.max(80, width * 0.10);
  const usableW = width - padX * 2;
  const step = mainStates.length > 1 ? usableW / (mainStates.length - 1) : 0;

  const positions = mainStates.map((state, i) => ({
    state,
    x: padX + step * i,
    y: midY
  }));

  deadStates.forEach(state => {
    positions.push({ state, x: width * 0.5, y: botY });
  });

  return positions;
}

// AFND: single row for <=5 states, two rows otherwise
function layoutAfnd(states, width, height) {
  const n = states.length;
  const padX = Math.max(80, width * 0.11);
  const usableW = width - padX * 2;

  if (n <= 5) {
    const step = n > 1 ? usableW / (n - 1) : 0;
    return states.map((state, i) => ({ state, x: padX + step * i, y: height * 0.5 }));
  }

  const cols = Math.ceil(n / 2);
  const stepX = cols > 1 ? usableW / (cols - 1) : 0;
  return states.map((state, i) => ({
    state,
    x: padX + stepX * (i % cols),
    y: i < cols ? height * 0.33 : height * 0.67
  }));
}

// Circle fallback
function layoutCircle(states, width, height) {
  const cx = width / 2, cy = height / 2;
  const rx = Math.min(220, width * 0.37);
  const ry = Math.min(160, height * 0.35);
  return states.map((state, i) => {
    const angle = (2 * Math.PI * i) / states.length - Math.PI / 2;
    return { state, x: cx + Math.cos(angle) * rx, y: cy + Math.sin(angle) * ry };
  });
}

// ─────────────────────────────────────────────
// Transition helpers
// ─────────────────────────────────────────────

function getTransitionPairs(definition) {
  const pairs = [];
  for (const [fromState, transitions] of Object.entries(definition.transitions || {})) {
    for (const [symbol, destinations] of Object.entries(transitions || {})) {
      const list = Array.isArray(destinations) ? destinations : [destinations];
      for (const toState of list) {
        pairs.push({ fromState, toState, symbol });
      }
    }
  }
  return pairs;
}

function buildTransitionLabels(definition) {
  const labelMap = new Map();
  for (const pair of getTransitionPairs(definition)) {
    const key = `${pair.fromState}-->${pair.toState}`;
    const current = labelMap.get(key) || [];
    current.push(pair.symbol);
    labelMap.set(key, current);
  }
  return labelMap;
}

// ─────────────────────────────────────────────
// Drawing
// ─────────────────────────────────────────────

const NODE_R     = 32;
const NODE_R_ACC = 34;
const INNER_R    = 27;

function drawArrowMarker(svg) {
  const defs = createSvgElement('defs');
  const marker = createSvgElement('marker', {
    id: 'arrowhead', viewBox: '0 0 10 10',
    refX: '9', refY: '5',
    markerWidth: '7', markerHeight: '7',
    orient: 'auto-start-reverse'
  });
  marker.appendChild(createSvgElement('path', { d: 'M 0 0 L 10 5 L 0 10 z', fill: '#7dd3fc' }));
  defs.appendChild(marker);
  svg.appendChild(defs);
}

function getStateRadius(state, acceptingStates) {
  return acceptingStates.has(state) ? NODE_R_ACC : NODE_R;
}

function getAnchoredPoints(from, to, r1, r2) {
  const dx = to.x - from.x, dy = to.y - from.y;
  const dist = Math.sqrt(dx * dx + dy * dy) || 1;
  return {
    sx: from.x + dx / dist * r1,
    sy: from.y + dy / dist * r1,
    ex: to.x   - dx / dist * r2,
    ey: to.y   - dy / dist * r2,
    nx: -dy / dist, ny: dx / dist,
    dist
  };
}

function chooseSelfLoopSide(pos, allPositions) {
  const others = allPositions.filter(p => p.state !== pos.state);
  if (!others.length) return 'top';
  let sx = 0, sy = 0;
  for (const o of others) { sx += o.x - pos.x; sy += o.y - pos.y; }
  const opp = (Math.atan2(sy, sx) * 180 / Math.PI + 180 + 360) % 360;
  if (opp >= 315 || opp < 45)  return 'right';
  if (opp >= 45  && opp < 135) return 'bottom';
  if (opp >= 135 && opp < 225) return 'left';
  return 'top';
}

function drawSelfLoop(svg, pos, label, isHighlighted, side = 'top') {
  const r  = 26;
  const nr = NODE_R;
  let ox = pos.x, oy = pos.y;
  let lx, ly;

  if (side === 'top')    { oy = pos.y - nr - r; lx = ox + r + 8; ly = oy - 6; }
  if (side === 'bottom') { oy = pos.y + nr + r; lx = ox + r + 8; ly = oy + 6; }
  if (side === 'left')   { ox = pos.x - nr - r; lx = ox - r - 8; ly = oy - 6; }
  if (side === 'right')  { ox = pos.x + nr + r; lx = ox + r + 8; ly = oy - 6; }

  svg.appendChild(createSvgElement('circle', {
    cx: ox, cy: oy, r,
    fill: 'none',
    stroke: isHighlighted ? '#5eead4' : 'rgba(173,191,255,0.75)',
    'stroke-width': '2.2'
  }));

  // Small arrowhead polygon at tangent point
  let ax, ay, rot;
  if (side === 'top')    { ax = pos.x + r; ay = pos.y - nr - r; rot = 90; }
  if (side === 'bottom') { ax = pos.x - r; ay = pos.y + nr + r; rot = 270; }
  if (side === 'left')   { ax = pos.x - nr - r; ay = pos.y - r; rot = 0; }
  if (side === 'right')  { ax = pos.x + nr + r; ay = pos.y + r; rot = 180; }

  svg.appendChild(createSvgElement('polygon', {
    points: '0,-5 9,0 0,5',
    fill: '#7dd3fc',
    transform: `translate(${ax},${ay}) rotate(${rot})`
  }));

  const t = createSvgElement('text', {
    x: lx, y: ly,
    fill: '#ecf2ff', 'font-size': '12',
    'font-family': 'IBM Plex Mono, monospace',
    'text-anchor': side === 'left' ? 'end' : 'start',
    stroke: 'rgba(11,16,32,0.95)',
    'stroke-width': '3',
    'paint-order': 'stroke fill',
    'dominant-baseline': 'middle'
  });
  t.textContent = label;
  svg.appendChild(t);
}

function drawEdge(svg, from, to, label, isHighlighted, acceptingStates, isBidirectional, isDeterministic) {
  const r1 = getStateRadius(from.state, acceptingStates);
  const r2 = getStateRadius(to.state,   acceptingStates);
  const a  = getAnchoredPoints(from, to, r1, r2);

  let mag = 0;

  if (isBidirectional) {
    // MAGIA: Flechas bidireccionales forzadas a separarse bastante
    mag = 45;
  } else if (a.dist > 180) {
    // Saltos largos: se abren en un carril ancho para no pisar otros nodos
    mag = a.dist * 0.22 + ((from.y % 40) * 0.4);
  } else {
    // Saltos cortos de una via
    mag = 15;
  }

  // Al no usar el maldito 'bend', la ida y la vuelta reaccionan naturalmente
  // a la direccion del trazo y se separan en lados opuestos.
  const bx = a.nx * mag;
  const by = a.ny * mag;

  // Offset lateral para que no nazcan exactamente del mismo pixel en el borde del nodo
  const sideOffset = isBidirectional ? 14 : 0;
  const sx = a.sx + a.nx * sideOffset;
  const sy = a.sy + a.ny * sideOffset;
  const ex = a.ex + a.nx * sideOffset;
  const ey = a.ey + a.ny * sideOffset;

  const c1x = sx + (ex - sx) * 0.3 + bx;
  const c1y = sy + (ey - sy) * 0.3 + by;
  const c2x = sx + (ex - sx) * 0.7 + bx;
  const c2y = sy + (ey - sy) * 0.7 + by;

  svg.appendChild(createSvgElement('path', {
    d: `M ${sx} ${sy} C ${c1x} ${c1y}, ${c2x} ${c2y}, ${ex} ${ey}`,
    fill: 'none',
    stroke: isHighlighted ? '#5eead4' : 'rgba(173,191,255,0.72)',
    'stroke-width': '2.1',
    'marker-end': 'url(#arrowhead)'
  }));

  const lx = (sx + ex) / 2 + bx * 0.65;
  const ly = (sy + ey) / 2 + by * 0.65 - 8;

  const t = createSvgElement('text', {
    x: lx, y: ly,
    fill: '#ecf2ff', 'font-size': '12',
    'font-family': 'IBM Plex Mono, monospace',
    stroke: 'rgba(11,16,32,0.95)',
    'stroke-width': '3',
    'paint-order': 'stroke fill',
    'text-anchor': 'middle'
  });
  t.textContent = label;
  svg.appendChild(t);
}

function stateLines(state) {
  const clean = state.replace(/[{}]/g, '');
  const parts = clean.split(',').map(s => s.trim()).filter(Boolean);
  if (parts.length <= 2) return [state];
  const mid = Math.ceil(parts.length / 2);
  return ['{' + parts.slice(0, mid).join(',') + '}', '{' + parts.slice(mid).join(',') + '}'];
}

function drawNode(svg, pos, isAccepting, isActive, isDead, highlightState) {
  const g = createSvgElement('g');
  const r = isAccepting ? NODE_R_ACC : NODE_R;

  if (isDead) {
    g.appendChild(createSvgElement('circle', {
      cx: pos.x, cy: pos.y, r,
      fill: isActive ? 'rgba(244,114,182,0.10)' : 'rgba(9,14,30,0.92)',
      stroke: isActive ? '#fda4af' : '#fbbf24',
      'stroke-width': '2.2', 'stroke-dasharray': '6 4'
    }));
    g.appendChild(createSvgElement('circle', {
      cx: pos.x, cy: pos.y, r: 22,
      fill: 'transparent',
      stroke: isActive ? '#fda4af' : 'rgba(251,191,36,0.55)',
      'stroke-width': '1.4', 'stroke-dasharray': '3 4'
    }));
  } else if (isAccepting) {
    g.appendChild(createSvgElement('circle', {
      cx: pos.x, cy: pos.y, r,
      fill: isActive ? 'rgba(94,234,212,0.12)' : 'rgba(125,211,252,0.06)',
      stroke: isActive ? '#5eead4' : '#7dd3fc', 'stroke-width': '2.2'
    }));
    g.appendChild(createSvgElement('circle', {
      cx: pos.x, cy: pos.y, r: INNER_R,
      fill: 'transparent',
      stroke: isActive ? '#5eead4' : '#7dd3fc', 'stroke-width': '1.6'
    }));
  } else {
    g.appendChild(createSvgElement('circle', {
      cx: pos.x, cy: pos.y, r,
      fill: isActive ? 'rgba(94,234,212,0.14)' : 'rgba(255,255,255,0.05)',
      stroke: isActive ? '#5eead4' : 'rgba(173,191,255,0.82)', 'stroke-width': '2.2'
    }));
  }

  const lines = stateLines(pos.state);
  const fs = pos.state.length > 12 ? '9' : pos.state.length > 8 ? '10' : pos.state.length > 5 ? '11' : '13';
  const lineH = 14;
  const startY = pos.y - (lines.length - 1) * lineH / 2;

  lines.forEach((line, i) => {
    const t = createSvgElement('text', {
      x: pos.x, y: startY + i * lineH,
      fill: isDead ? '#fbbf24' : '#ecf2ff',
      'font-size': fs, 'font-weight': '700',
      'text-anchor': 'middle', 'dominant-baseline': 'middle',
      'font-family': 'IBM Plex Mono, monospace'
    });
    t.textContent = line;
    g.appendChild(t);
  });

  if (highlightState === pos.state) {
    g.appendChild(createSvgElement('circle', {
      cx: pos.x, cy: pos.y, r: r + 7,
      fill: 'transparent', stroke: '#fbbf24',
      'stroke-width': '2', 'stroke-dasharray': '5 4'
    }));
  }

  svg.appendChild(g);
}

function enablePanZoom(svg, width, height) {
  let viewBox = { x: 0, y: 0, width, height };
  let isPanning = false;
  let start = { x: 0, y: 0 };
  let origin = { x: 0, y: 0 };

  svg.setAttribute('viewBox', `0 0 ${width} ${height}`);
  svg.style.cursor = 'grab';

  function setViewBox() {
    svg.setAttribute('viewBox', `${viewBox.x} ${viewBox.y} ${viewBox.width} ${viewBox.height}`);
  }

  function toSvgPoint(clientX, clientY) {
    const point = svg.createSVGPoint();
    point.x = clientX;
    point.y = clientY;
    const ctm = svg.getScreenCTM();
    return ctm ? point.matrixTransform(ctm.inverse()) : { x: clientX, y: clientY };
  }

  svg.addEventListener('wheel', (event) => {
    event.preventDefault();
    const zoomFactor = event.deltaY > 0 ? 1.12 : 0.9;
    const mouse = toSvgPoint(event.clientX, event.clientY);

    const newWidth = Math.min(width * 6, Math.max(width * 0.4, viewBox.width * zoomFactor));
    const newHeight = Math.min(height * 6, Math.max(height * 0.4, viewBox.height * zoomFactor));

    const scaleX = newWidth / viewBox.width;
    const scaleY = newHeight / viewBox.height;

    viewBox.x = mouse.x - (mouse.x - viewBox.x) * scaleX;
    viewBox.y = mouse.y - (mouse.y - viewBox.y) * scaleY;
    viewBox.width = newWidth;
    viewBox.height = newHeight;
    setViewBox();
  }, { passive: false });

  svg.addEventListener('pointerdown', (event) => {
    isPanning = true;
    svg.setPointerCapture(event.pointerId);
    svg.style.cursor = 'grabbing';
    start = { x: event.clientX, y: event.clientY };
    origin = { x: viewBox.x, y: viewBox.y };
  });

  svg.addEventListener('pointermove', (event) => {
    if (!isPanning) return;
    const dx = (event.clientX - start.x) * (viewBox.width / svg.clientWidth);
    const dy = (event.clientY - start.y) * (viewBox.height / svg.clientHeight);
    viewBox.x = origin.x - dx;
    viewBox.y = origin.y - dy;
    setViewBox();
  });

  const stopPan = (event) => {
    if (!isPanning) return;
    isPanning = false;
    svg.releasePointerCapture(event.pointerId);
    svg.style.cursor = 'grab';
  };

  svg.addEventListener('pointerup', stopPan);
  svg.addEventListener('pointerleave', stopPan);
}

// ─────────────────────────────────────────────
// Public API
// ─────────────────────────────────────────────

export function drawAutomata(containerId, definition, options = {}) {
  const container = document.getElementById(containerId);
  if (!container) return;

  const baseWidth  = Math.max(container.clientWidth  || 900, 600);
  const baseHeight = Math.max(container.clientHeight || 480, 340);

  const acceptingStates = new Set(definition.acceptingStates || []);
  const activeStates    = new Set((options.activeStates || []).filter(Boolean));

  const layout = computeLayout(definition, baseWidth, baseHeight);
  const positions = layout.positions;
  const width = layout.width;
  const height = layout.height;
  const byState   = new Map(positions.map(p => [p.state, p]));

  const svg = createSvgElement('svg', {
    width: `${width}`,
    height: `${height}`,
    role: 'img',
    'aria-label': `Diagrama de ${definition.name || 'autómata'}`
  });

  drawArrowMarker(svg);

  // Initial arrow
  const initPos = byState.get(definition.initialState) || positions[0];
  if (initPos) {
    const arrowLen = 50;
    const ix = Math.max(initPos.x - NODE_R - arrowLen, 10);
    svg.appendChild(createSvgElement('line', {
      x1: ix, y1: initPos.y,
      x2: initPos.x - NODE_R - 2, y2: initPos.y,
      stroke: '#fbbf24', 'stroke-width': '2.2',
      'marker-end': 'url(#arrowhead)'
    }));
    const lt = createSvgElement('text', {
      x: ix - 3, y: initPos.y - 9,
      fill: '#fbbf24', 'font-size': '11',
      'font-family': 'IBM Plex Mono, monospace', 'text-anchor': 'end'
    });
    lt.textContent = 'inicio';
    svg.appendChild(lt);
  }

  // Edges
  const transitionLabels = buildTransitionLabels(definition);
  const drawnPairs = new Set();

  for (const [key, labels] of transitionLabels.entries()) {
    const [fromState, toState] = key.split('-->');
    const from = byState.get(fromState);
    const to   = byState.get(toState);
    if (!from || !to) continue;

    const isHighlighted = activeStates.has(fromState) || activeStates.has(toState);
    const label = labels.join(', ');

    if (fromState === toState) {
      drawSelfLoop(svg, from, label, isHighlighted, chooseSelfLoopSide(from, positions));
    } else {
      const reverseKey = `${toState}-->${fromState}`;
      const isBidirectional = transitionLabels.has(reverseKey);
      const isDeterministic = definition.automataType === 'AFD' || definition.automataType === 'AFD_MIN';
      drawEdge(svg, from, to, label, isHighlighted, acceptingStates, isBidirectional, isDeterministic);
    }
    drawnPairs.add(key);
  }

  // Nodes on top
  for (const pos of positions) {
    drawNode(svg, pos,
      acceptingStates.has(pos.state),
      activeStates.has(pos.state),
      pos.state === '∅',
      options.highlightState || null
    );
  }

  container.innerHTML = '';
  container.appendChild(svg);
  enablePanZoom(svg, width, height);
}