const API_BASE = 'http://localhost:8080/api/automata';

async function requestJson(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    },
    ...options
  });

  if (!response.ok) {
    throw new Error(`HTTP ${response.status} - ${response.statusText}`);
  }

  return response.json();
}

export async function simulate(exerciseId, automataType, input) {
  return requestJson('/simulate', {
    method: 'POST',
    body: JSON.stringify({ exerciseId, automataType, input })
  });
}

export async function getTestResults(exerciseId) {
  return requestJson(`/test/${exerciseId}`);
}

export async function getDefinition(exerciseId, automataType = 'AFND') {
  return requestJson(`/definition/${exerciseId}?automataType=${encodeURIComponent(automataType)}`);
}
