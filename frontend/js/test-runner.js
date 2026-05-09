export function renderTestTable(container, rows) {
  if (!container) {
    return;
  }

  const table = document.createElement('table');
  table.innerHTML = `
    <thead>
      <tr>
        <th>#</th>
        <th>Cadena</th>
        <th>AFND</th>
        <th>AFD</th>
        <th>AFD-MIN</th>
        <th>¿Equivalentes?</th>
      </tr>
    </thead>
  `;

  const tbody = document.createElement('tbody');
  for (const row of rows) {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${row.index}</td>
      <td><span class="badge neutral">${escapeHtml(row.input === '' ? 'ε' : row.input)}</span></td>
      <td>${formatDecision(row.afndAccepted)}</td>
      <td>${formatDecision(row.afdAccepted)}</td>
      <td>${formatDecision(row.afdMinAccepted)}</td>
      <td>${row.equivalent ? '<span class="badge success">✅ Sí</span>' : '<span class="badge danger">❌ No</span>'}</td>
    `;
    tbody.appendChild(tr);
  }
  table.appendChild(tbody);
  container.innerHTML = '';
  container.appendChild(table);
}

function formatDecision(value) {
  return value ? '<span class="badge success">ACEPTA</span>' : '<span class="badge danger">RECHAZA</span>';
}

function escapeHtml(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}
