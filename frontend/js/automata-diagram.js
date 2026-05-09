const SVG_NS = 'http://www.w3.org/2000/svg';

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

function getNodeLayout(states, width, height) {
  const centerX = width / 2;
  const centerY = height / 2;
  const marginX = Math.max(110, width * 0.16);
  const marginY = Math.max(90, height * 0.14);
  const radiusX = Math.min(240, Math.max(150, width / 2 - marginX));
  const radiusY = Math.min(180, Math.max(120, height / 2 - marginY));

  if (states.length === 4) {
    const positions = [
      { x: centerX, y: centerY - radiusY },
      { x: centerX + radiusX, y: centerY },
      { x: centerX, y: centerY + radiusY },
      { x: centerX - radiusX, y: centerY }
    ];

    return states.map((state, index) => ({ state, ...positions[index] }));
  }

  if (states.length === 5) {
    const positions = [
      { x: centerX, y: centerY - radiusY },
      { x: centerX + radiusX * 0.92, y: centerY - radiusY * 0.18 },
      { x: centerX + radiusX * 0.72, y: centerY + radiusY * 0.92 },
      { x: centerX - radiusX * 0.72, y: centerY + radiusY * 0.92 },
      { x: centerX - radiusX * 0.92, y: centerY - radiusY * 0.18 }
    ];

    return states.map((state, index) => ({ state, ...positions[index] }));
  }

  if (states.length === 3) {
    const positions = [
      { x: centerX, y: centerY - radiusY },
      { x: centerX + radiusX, y: centerY + radiusY * 0.65 },
      { x: centerX - radiusX, y: centerY + radiusY * 0.65 }
    ];

    return states.map((state, index) => ({ state, ...positions[index] }));
  }

  return states.map((state, index) => {
    const angle = (Math.PI * 2 * index) / Math.max(states.length, 1) - Math.PI / 2;
    return {
      state,
      x: centerX + Math.cos(angle) * radiusX,
      y: centerY + Math.sin(angle) * radiusY
    };
  });
}

function getStateLabel(state) {
  return state;
}

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

function getStateBounds(position) {
  return {
    x: position.x - 34,
    y: position.y - 34,
    width: 68,
    height: 68
  };
}

function drawArrowMarker(svg) {
  const defs = createSvgElement('defs');
  const marker = createSvgElement('marker', {
    id: 'arrowhead',
    viewBox: '0 0 10 10',
    refX: '9',
    refY: '5',
    markerWidth: '8',
    markerHeight: '8',
    orient: 'auto-start-reverse'
  });
  marker.appendChild(createSvgElement('path', { d: 'M 0 0 L 10 5 L 0 10 z', fill: '#7dd3fc' }));
  defs.appendChild(marker);
  svg.appendChild(defs);
}

function drawSelfLoop(svg, position, label, isHighlighted) {
  const loopSize = 34;
  const circle = createSvgElement('path', {
    d: `M ${position.x - 6} ${position.y - 40} C ${position.x + loopSize} ${position.y - 66}, ${position.x + loopSize} ${position.y - 10}, ${position.x - 6} ${position.y - 18}`,
    fill: 'none',
    stroke: isHighlighted ? '#5eead4' : 'rgba(173, 191, 255, 0.75)',
    'stroke-width': '2.2',
    'marker-end': 'url(#arrowhead)'
  });
  svg.appendChild(circle);

  const text = createSvgElement('text', {
    x: position.x + 16,
    y: position.y - 72,
    fill: '#ecf2ff',
    'font-size': '12',
    'font-family': 'IBM Plex Mono, monospace'
  });
  text.textContent = label;
  svg.appendChild(text);
}

function getRadiusForState(position, isAccepting) {
  return isAccepting ? 34 : 32;
}

function getAnchoredPoints(from, to, fromRadius, toRadius) {
  const dx = to.x - from.x;
  const dy = to.y - from.y;
  const distance = Math.sqrt(dx * dx + dy * dy) || 1;
  const unitX = dx / distance;
  const unitY = dy / distance;

  return {
    startX: from.x + unitX * fromRadius,
    startY: from.y + unitY * fromRadius,
    endX: to.x - unitX * toRadius,
    endY: to.y - unitY * toRadius,
    offsetX: -unitY,
    offsetY: unitX,
    distance
  };
}

function drawCurve(svg, from, to, label, isHighlighted, reverse = false) {
  const fromRadius = 32;
  const toRadius = 32;
  const anchored = getAnchoredPoints(from, to, fromRadius, toRadius);
  const dx = anchored.endX - anchored.startX;
  const dy = anchored.endY - anchored.startY;
  const offsetX = anchored.offsetX * 48;
  const offsetY = anchored.offsetY * 48;
  const bend = reverse ? -1 : 1;
  const control1X = anchored.startX + dx * 0.28 + offsetX * bend;
  const control1Y = anchored.startY + dy * 0.28 + offsetY * bend;
  const control2X = anchored.startX + dx * 0.72 + offsetX * bend;
  const control2Y = anchored.startY + dy * 0.72 + offsetY * bend;
  const path = createSvgElement('path', {
    d: `M ${anchored.startX} ${anchored.startY} C ${control1X} ${control1Y}, ${control2X} ${control2Y}, ${anchored.endX} ${anchored.endY}`,
    fill: 'none',
    stroke: isHighlighted ? '#5eead4' : 'rgba(173, 191, 255, 0.72)',
    'stroke-width': '2.1',
    'marker-end': 'url(#arrowhead)'
  });
  svg.appendChild(path);

  const labelX = (anchored.startX + anchored.endX) / 2 + offsetX * 0.22 * bend;
  const labelY = (anchored.startY + anchored.endY) / 2 + offsetY * 0.22 * bend - 8;
  const text = createSvgElement('text', {
    x: labelX,
    y: labelY,
    fill: '#ecf2ff',
    'font-size': '12',
    'font-family': 'IBM Plex Mono, monospace',
    'text-anchor': 'middle'
  });
  text.textContent = label;
  svg.appendChild(text);
}

export function drawAutomata(containerId, definition, options = {}) {
  const container = document.getElementById(containerId);
  if (!container) {
    return;
  }

  const width = container.clientWidth || 900;
  const height = container.clientHeight || 560;
  const states = normalizeStates(definition.states || []);
  const positions = getNodeLayout(states, width, height);
  const positionsByState = new Map(positions.map((item) => [item.state, item]));
  const acceptingStates = new Set(definition.acceptingStates || []);
  const activeStates = new Set((options.activeStates || []).filter(Boolean));
  const transitionLabels = buildTransitionLabels(definition);

  const svg = createSvgElement('svg', {
    viewBox: `0 0 ${width} ${height}`,
    role: 'img',
    'aria-label': `Diagrama de ${definition.name || 'autómata'}`
  });

  drawArrowMarker(svg);

  const initialPosition = positionsByState.get(definition.initialState) || positions[0];
  if (initialPosition) {
    const entryLine = createSvgElement('line', {
      x1: Math.max(24, initialPosition.x - 110),
      y1: initialPosition.y,
      x2: initialPosition.x - 42,
      y2: initialPosition.y,
      stroke: '#fbbf24',
      'stroke-width': '2.2',
      'marker-end': 'url(#arrowhead)'
    });
    svg.appendChild(entryLine);

    const entryText = createSvgElement('text', {
      x: Math.max(22, initialPosition.x - 130),
      y: initialPosition.y - 10,
      fill: '#fbbf24',
      'font-size': '12',
      'font-family': 'IBM Plex Mono, monospace'
    });
    entryText.textContent = 'inicio';
    svg.appendChild(entryText);
  }

  const drawnPairs = new Set();
  for (const [key, labels] of transitionLabels.entries()) {
    const [fromState, toState] = key.split('-->');
    const from = positionsByState.get(fromState);
    const to = positionsByState.get(toState);
    if (!from || !to) {
      continue;
    }

    const isHighlighted = activeStates.has(fromState) || activeStates.has(toState);
    if (fromState === toState) {
      drawSelfLoop(svg, from, labels.join(', '), isHighlighted);
    } else {
      const reverse = drawnPairs.has(`${toState}-->${fromState}`);
      drawCurve(svg, from, to, labels.join(', '), isHighlighted, reverse);
    }
    drawnPairs.add(key);
  }

  for (const position of positions) {
    const isAccepting = acceptingStates.has(position.state);
    const isActive = activeStates.has(position.state);
    const isDeadState = position.state === '∅';
    const group = createSvgElement('g', {});

    if (isDeadState) {
      group.appendChild(createSvgElement('circle', {
        cx: position.x,
        cy: position.y,
        r: '36',
        fill: isActive ? 'rgba(244, 114, 182, 0.10)' : 'rgba(9, 14, 30, 0.92)',
        stroke: isActive ? '#fda4af' : '#fbbf24',
        'stroke-width': '2.4',
        'stroke-dasharray': '6 4'
      }));
      group.appendChild(createSvgElement('circle', {
        cx: position.x,
        cy: position.y,
        r: '24',
        fill: 'transparent',
        stroke: isActive ? '#fda4af' : 'rgba(251, 191, 36, 0.55)',
        'stroke-width': '1.4',
        'stroke-dasharray': '3 4'
      }));
    } else if (isAccepting) {
      group.appendChild(createSvgElement('circle', {
        cx: position.x,
        cy: position.y,
        r: '34',
        fill: isActive ? 'rgba(94, 234, 212, 0.12)' : 'rgba(125, 211, 252, 0.06)',
        stroke: isActive ? '#5eead4' : '#7dd3fc',
        'stroke-width': '2.2'
      }));
      group.appendChild(createSvgElement('circle', {
        cx: position.x,
        cy: position.y,
        r: '28',
        fill: 'transparent',
        stroke: isActive ? '#5eead4' : '#7dd3fc',
        'stroke-width': '1.6'
      }));
    } else {
      group.appendChild(createSvgElement('circle', {
        cx: position.x,
        cy: position.y,
        r: '32',
        fill: isActive ? 'rgba(94, 234, 212, 0.14)' : 'rgba(255, 255, 255, 0.05)',
        stroke: isActive ? '#5eead4' : 'rgba(173, 191, 255, 0.82)',
        'stroke-width': '2.2'
      }));
    }

    const text = createSvgElement('text', {
      x: position.x,
      y: position.y + 5,
      fill: '#ecf2ff',
      'font-size': isDeadState ? '15' : position.state.length > 8 ? '11' : '13',
      'font-weight': '700',
      'text-anchor': 'middle',
      'dominant-baseline': 'middle',
      'font-family': 'IBM Plex Mono, monospace'
    });
    if (isDeadState) {
      text.textContent = '∅';
      group.appendChild(text);

      const subtitle = createSvgElement('text', {
        x: position.x,
        y: position.y + 21,
        fill: '#fbbf24',
        'font-size': '10',
        'font-family': 'IBM Plex Mono, monospace',
        'text-anchor': 'middle'
      });
      subtitle.textContent = 'muerto';
      group.appendChild(subtitle);
    } else if (position.state.length > 8) {
      const lines = position.state.replace(/[{}]/g, '').split(',');
      text.textContent = '';
      const firstLine = createSvgElement('tspan', { x: position.x, dy: '-4' });
      firstLine.textContent = `{${lines.slice(0, Math.ceil(lines.length / 2)).join(',')}}`;
      text.appendChild(firstLine);
      if (lines.length > 1) {
        const secondLine = createSvgElement('tspan', { x: position.x, dy: '14' });
        secondLine.textContent = `{${lines.slice(Math.ceil(lines.length / 2)).join(',')}}`;
        text.appendChild(secondLine);
      }
    } else {
      text.textContent = getStateLabel(position.state);
      group.appendChild(text);
    }

    svg.appendChild(group);

    if (options.highlightState === position.state) {
      svg.appendChild(createSvgElement('circle', {
        cx: position.x,
        cy: position.y,
        r: '40',
        fill: 'transparent',
        stroke: '#fbbf24',
        'stroke-width': '2',
        'stroke-dasharray': '5 5'
      }));
    }
  }

  container.innerHTML = '';
  container.appendChild(svg);
}
