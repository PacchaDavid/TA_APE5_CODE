import { drawAutomata } from './automata-diagram.js';
import { getDefinition, getTestResults, simulate } from './api.js';
import { renderTestTable } from './test-runner.js';

const exercises = [
  { id: 4, title: 'Ejercicio 4', subtitle: 'Detección de ataque SYN flood' },
  { id: 5, title: 'Ejercicio 5', subtitle: 'Validación de protocolo IoT' },
  { id: 6, title: 'Ejercicio 6', subtitle: 'Reconocimiento de secuencias genéticas' }
];

const automataLabels = {
  AFND: 'AFND original',
  AFD: 'AFD equivalente',
  AFD_MIN: 'AFD minimizado'
};

const exerciseHelp = {
  4: {
    summary: 'Busca la secuencia <strong>SYN → uno o más ACK → RST</strong> sin paquetes de datos intermedios. El estado de aceptación representa ataque detectado.',
    tip: 'Empieza probando una cadena corta como <strong>sar</strong> y luego usa cadenas con ruido para ver cómo se comporta el autómata.',
    examples: ['sar', 'saar', 'ooosar']
  },
  5: {
    summary: 'Valida un paquete IoT con la forma <strong>HDR → (TEMP | HUM)* → CRC</strong>. El autómata solo acepta si el CRC cierra la secuencia.',
    tip: 'Prueba primero <strong>hc</strong> para verificar el caso mínimo y luego cadenas con lecturas intermedias.',
    examples: ['hc', 'htmc', 'htttttc']
  },
  6: {
    summary: 'Detecta el patrón <strong>K → G → (cualquier aminoácido)* → F</strong> dentro de una cadena más larga. El autómata puede aceptar incluso con prefijos o sufijos.',
    tip: 'Usa <strong>kgf</strong> como base y después agrega prefijos o repeticiones para observar la diferencia entre AFND y AFD.',
    examples: ['kgf', 'xkgf', 'kgxxf']
  }
};

const colorGuide = [
  ['Acepta', '#5eead4', 'La cadena pertenece al lenguaje del autómata.'],
  ['Rechaza', '#fb7185', 'La cadena no cumple el patrón esperado.'],
  ['Inicial', '#fbbf24', 'Marca el estado de inicio en el diagrama.'],
  ['Activo', '#7dd3fc', 'Resalta el estado actual al simular.']
];

const state = {
  exerciseId: exercises[0].id,
  definition: null,
  testRows: [],
  highlightedState: null,
  activeStepIndex: null
};

const elements = {
  exerciseTabs: document.getElementById('exerciseTabs'),
  exerciseTitle: document.getElementById('exerciseTitle'),
  automataType: document.getElementById('automataType'),
  definitionInfo: document.getElementById('definitionInfo'),
  definitionTable: document.getElementById('definitionTable'),
  diagramContainer: document.getElementById('diagramContainer'),
  diagramLegend: document.getElementById('diagramLegend'),
  simulationForm: document.getElementById('simulationForm'),
  inputString: document.getElementById('inputString'),
  simulationStatus: document.getElementById('simulationStatus'),
  simulationResults: document.getElementById('simulationResults'),
  testTable: document.getElementById('testTable'),
  refreshDiagram: document.getElementById('refreshDiagram'),
  reloadTests: document.getElementById('reloadTests'),
  exerciseHelp: document.getElementById('exerciseHelp'),
  exampleChips: document.getElementById('exampleChips'),
  colorGuide: document.getElementById('colorGuide')
};

initialize();

function initialize() {
  renderExerciseTabs();
  bindEvents();
  loadExercise(state.exerciseId);
}

function bindEvents() {
  elements.automataType.addEventListener('change', () => loadDefinition());
  elements.simulationForm.addEventListener('submit', handleSimulationSubmit);
  elements.refreshDiagram.addEventListener('click', () => renderDiagram());
  elements.reloadTests.addEventListener('click', () => loadTestResults());
}

function renderExerciseTabs() {
  elements.exerciseTabs.innerHTML = '';
  for (const exercise of exercises) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = `exercise-tab ${exercise.id === state.exerciseId ? 'active' : ''}`;
    button.textContent = `${exercise.title}`;
    button.title = exercise.subtitle;
    button.addEventListener('click', () => loadExercise(exercise.id));
    elements.exerciseTabs.appendChild(button);
  }
}

async function loadExercise(exerciseId) {
  state.exerciseId = exerciseId;
  state.highlightedState = null;
  state.activeStepIndex = null;
  renderExerciseTabs();
  renderHelpPanel();
  renderColorGuide();
  elements.exerciseTitle.textContent = exercises.find((item) => item.id === exerciseId)?.title || 'Ejercicio';
  elements.simulationStatus.textContent = 'Esperando una cadena.';
  elements.simulationResults.innerHTML = '';
  await Promise.all([loadDefinition(), loadTestResults()]);
}

async function loadDefinition() {
  try {
    state.definition = await getDefinition(state.exerciseId, elements.automataType.value);
    refreshDefinitionView();
    renderLegend();
  } catch (error) {
    renderError(elements.definitionInfo, `No se pudo cargar la definición: ${error.message}`);
  }
}

function refreshDefinitionView() {
  if (!state.definition) {
    return;
  }

  elements.exerciseTitle.textContent = `${state.definition.name}`;
  elements.definitionInfo.innerHTML = '';
  const pills = [
    ['Ejercicio', `#${state.definition.exerciseId}`],
    ['Tipo', state.definition.automataType],
    ['Estado inicial', state.definition.initialState],
    ['Aceptación', (state.definition.acceptingStates || []).join(', ') || '—'],
    ['Alfabeto', (state.definition.alphabet || []).join(', ')],
    ['Estados', (state.definition.states || []).join(', ')]
  ];

  for (const [label, value] of pills) {
    const pill = document.createElement('div');
    pill.className = 'info-pill';
    pill.innerHTML = `<span>${label}</span><strong>${escapeHtml(value)}</strong>`;
    elements.definitionInfo.appendChild(pill);
  }

  elements.definitionTable.innerHTML = renderTransitionTable(state.definition);
  renderDiagram();
}

function renderTransitionTable(definition) {
  const alphabet = definition.alphabet || [];
  const rows = definition.states || [];
  const transitions = definition.transitions || {};
  const headerCells = alphabet.map((symbol) => `<th>${escapeHtml(symbol)}</th>`).join('');

  const bodyRows = rows.map((stateName) => {
    const cells = alphabet.map((symbol) => {
      const value = transitions[stateName]?.[symbol];
      const formatted = Array.isArray(value) ? value.join(', ') : (value ?? '—');
      return `<td><span class="badge neutral">${escapeHtml(formatted)}</span></td>`;
    }).join('');
    return `<tr><td><strong>${escapeHtml(stateName)}</strong></td>${cells}</tr>`;
  }).join('');

  return `
    <table>
      <thead>
        <tr>
          <th>Estado</th>
          ${headerCells}
        </tr>
      </thead>
      <tbody>${bodyRows}</tbody>
    </table>
  `;
}

function renderLegend() {
  elements.diagramLegend.innerHTML = '';
  const legend = [
    ['Estable', '#7dd3fc'],
    ['Aceptación', '#5eead4'],
    ['Inicial', '#fbbf24']
  ];
  for (const [label, color] of legend) {
    const item = document.createElement('div');
    item.className = 'legend-item';
    item.innerHTML = `<span class="legend-swatch" style="background:${color}"></span>${label}`;
    elements.diagramLegend.appendChild(item);
  }
}

function renderHelpPanel() {
  const help = exerciseHelp[state.exerciseId];
  if (!help) {
    return;
  }

  elements.exerciseHelp.innerHTML = `
    <p>${help.summary}</p>
    <p>${help.tip}</p>
    <p><strong>Sugerencia:</strong> cambia el selector de autómata para comparar la misma entrada en AFND, AFD y AFD_MIN.</p>
  `;

  elements.exampleChips.innerHTML = '';
  help.examples.forEach((example) => {
    const chip = document.createElement('button');
    chip.type = 'button';
    chip.className = 'example-chip';
    chip.textContent = example;
    chip.title = `Probar ${example}`;
    chip.addEventListener('click', () => {
      elements.inputString.value = example;
      elements.inputString.focus();
    });
    elements.exampleChips.appendChild(chip);
  });
}

function renderColorGuide() {
  elements.colorGuide.innerHTML = '';
  colorGuide.forEach(([label, color, description]) => {
    const item = document.createElement('span');
    item.className = 'guide-tag';
    item.innerHTML = `<span class="guide-dot" style="background:${color}"></span>${label}`;
    item.title = description;
    elements.colorGuide.appendChild(item);
  });
}

function renderDiagram(activeStates = [], highlightState = null) {
  if (!state.definition) {
    return;
  }

  drawAutomata('diagramContainer', state.definition, {
    activeStates,
    highlightState
  });
}

async function loadTestResults() {
  try {
    state.testRows = await getTestResults(state.exerciseId);
    renderTestTable(elements.testTable, state.testRows);
  } catch (error) {
    elements.testTable.innerHTML = `<div class="simulation-status">No se pudieron cargar las pruebas: ${escapeHtml(error.message)}</div>`;
  }
}

async function handleSimulationSubmit(event) {
  event.preventDefault();
  if (!state.definition) {
    return;
  }

  const input = elements.inputString.value.trim();
  const automataType = elements.automataType.value;
  elements.simulationStatus.textContent = 'Simulando...';
  elements.simulationResults.innerHTML = '';

  try {
    const result = await simulate(state.exerciseId, automataType, input);
    elements.simulationStatus.innerHTML = `<span class="badge ${result.accepted ? 'success' : 'danger'}">${result.accepted ? 'ACEPTA' : 'RECHAZA'}</span><span class="status-note">El color verde indica aceptación; el rojo, rechazo. Haz clic en los pasos para seguir la ruta del estado.</span>`;
    renderSimulationResultCard(automataType, result, input);
    renderDiagram([result.finalState].filter(Boolean), result.finalState);
  } catch (error) {
    elements.simulationStatus.innerHTML = `<span class="badge danger">Error</span><span class="status-note">${escapeHtml(error.message)}</span>`;
  }
}

function renderSimulationResultCard(automataType, result, input) {
  const card = document.createElement('article');
  card.className = 'result-card';
  const title = automataLabels[automataType] || automataType;
  card.innerHTML = `
    <header>
      <h3>${escapeHtml(title)}</h3>
      <span class="badge ${result.accepted ? 'success' : 'danger'}">${result.accepted ? 'ACEPTA' : 'RECHAZA'}</span>
    </header>
    <p class="result-state">Entrada: ${escapeHtml(input === '' ? 'ε' : input)}<br>Estado final: ${escapeHtml(result.finalState || '—')}</p>
  `;

  const stepsList = document.createElement('div');
  stepsList.className = 'step-list';

  result.steps.forEach((step, index) => {
    const item = document.createElement('button');
    item.type = 'button';
    item.className = 'step-item';
    item.innerHTML = `<strong>#${index + 1}</strong> ${escapeHtml(step.symbol)}: ${escapeHtml(step.fromState)} → ${escapeHtml(step.toState)}`;
    item.addEventListener('click', () => {
      const stateName = step.toState || step.fromState;
      [...stepsList.querySelectorAll('.step-item')].forEach((node) => node.classList.remove('active'));
      item.classList.add('active');
      renderDiagram([stateName], stateName);
    });
    stepsList.appendChild(item);
  });

  if (result.steps.length === 0) {
    stepsList.innerHTML = '<div class="simulation-status">La cadena no tiene pasos; se evaluó el estado inicial. Usa una cadena de ejemplo para ver el recorrido completo.</div>';
  }

  card.appendChild(stepsList);
  elements.simulationResults.appendChild(card);
}

function renderError(container, message) {
  container.innerHTML = `<div class="simulation-status">${escapeHtml(message)}</div>`;
}

function escapeHtml(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}
