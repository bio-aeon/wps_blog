/* Content Assistant — Composer interactions */

let _platform = null;
let _csrfToken = null;
let _draftId = null;

function initComposer(platform, csrfToken, draftId) {
    _platform = platform;
    _csrfToken = csrfToken;
    _draftId = draftId;

    const btnSuggest = document.getElementById('btn-suggest');
    const btnGenerate = document.getElementById('btn-generate');
    const btnRefine = document.getElementById('btn-refine');
    const btnSave = document.getElementById('btn-save');
    const btnCopy = document.getElementById('btn-copy');
    const btnSeo = document.getElementById('btn-seo');

    if (btnSuggest) btnSuggest.addEventListener('click', suggestTopics);
    if (btnGenerate) btnGenerate.addEventListener('click', generateContent);
    if (btnRefine) btnRefine.addEventListener('click', refineContent);
    if (btnSave) btnSave.addEventListener('click', saveDraft);
    if (btnCopy) btnCopy.addEventListener('click', copyToClipboard);
    if (btnSeo) btnSeo.addEventListener('click', seoSuggest);
}

function initCharCounter(textareaId, maxChars) {
    const textarea = document.getElementById(textareaId);
    const counter = document.getElementById('char-counter');
    if (!textarea || !counter) return;

    function update() {
        const len = textarea.value.length;
        counter.textContent = len + ' / ' + maxChars + ' characters';
        counter.className = 'char-counter';
        if (len > maxChars) counter.classList.add('over');
        else if (len > maxChars * 0.9) counter.classList.add('warning');
    }

    textarea.addEventListener('input', update);
    update();
}

async function apiCall(url, method, body) {
    const opts = {
        method: method,
        headers: {
            'Content-Type': 'application/json',
            'X-CSRFToken': _csrfToken,
        },
    };
    if (body) opts.body = JSON.stringify(body);
    const resp = await fetch(url, opts);
    const data = await resp.json();
    if (!resp.ok) {
        alert('Error: ' + (data.error || resp.statusText));
        return null;
    }
    return data;
}

async function suggestTopics() {
    const topic = document.getElementById('topic').value;
    const lang = (document.getElementById('language-select') || {}).value || 'en';
    const data = await apiCall(
        '/admin/assistant/suggest/' + _platform + '/',
        'POST',
        {
            count: 5,
            language: lang,
            focus_areas: topic ? [topic] : [],
        }
    );
    if (!data) return;

    const panel = document.getElementById('suggestions-panel');
    const list = document.getElementById('suggestions-list');
    list.innerHTML = '';
    (data.suggestions || []).forEach(function(s) {
        const li = document.createElement('li');
        li.style.cursor = 'pointer';
        li.style.padding = '4px 0';
        li.innerHTML = '<strong>' + s.topic + '</strong> — ' + (s.brief || '');
        li.addEventListener('click', function() {
            document.getElementById('topic').value = s.topic;
            panel.style.display = 'none';
        });
        list.appendChild(li);
    });
    panel.style.display = 'block';
}

async function generateContent() {
    const topic = document.getElementById('topic').value;
    if (!topic) { alert('Please enter a topic first.'); return; }

    const lang = (document.getElementById('language-select') || {}).value || 'en';
    const params = { topic: topic, language: lang };

    const toneSelect = document.getElementById('tone-select');
    if (toneSelect) params.tone = toneSelect.value;

    const lengthSelect = document.getElementById('length-select');
    if (lengthSelect) params.target_length = lengthSelect.value;

    const formatSelect = document.getElementById('format-select');
    if (formatSelect) params.format = formatSelect.value;

    const data = await apiCall(
        '/admin/assistant/generate/' + _platform + '/',
        'POST',
        params
    );
    if (!data) return;

    document.getElementById('body').value = data.body || '';
    _draftId = data.id;
    const statusEl = document.getElementById('draft-status');
    if (statusEl) statusEl.textContent = data.status || 'draft';
    const idEl = document.getElementById('draft-id');
    if (idEl) idEl.textContent = data.id || '—';

    // Trigger char counter update
    document.getElementById('body').dispatchEvent(new Event('input'));
}

async function refineContent() {
    if (!_draftId) { alert('Generate or save a draft first.'); return; }
    const instructions = prompt('How should this be improved?');
    if (!instructions) return;

    const data = await apiCall('/admin/assistant/refine/', 'POST', {
        draft_id: _draftId,
        instructions: instructions,
    });
    if (!data) return;

    document.getElementById('body').value = data.body || '';
    document.getElementById('body').dispatchEvent(new Event('input'));
}

async function saveDraft() {
    const body = document.getElementById('body').value;
    const title = document.getElementById('topic').value;
    const lang = (document.getElementById('language-select') || {}).value || 'en';

    if (_draftId) {
        const data = await apiCall('/admin/assistant/drafts/' + _draftId + '/', 'PUT', {
            title: title,
            body: body,
        });
        if (data) alert('Draft saved.');
    } else {
        const data = await apiCall('/admin/assistant/drafts/', 'POST', {
            platform: _platform,
            title: title,
            body: body,
            language_code: lang,
        });
        if (data) {
            _draftId = data.id;
            const idEl = document.getElementById('draft-id');
            if (idEl) idEl.textContent = data.id;
            alert('Draft created.');
        }
    }
}

function copyToClipboard() {
    const body = document.getElementById('body').value;
    if (!body) { alert('Nothing to copy.'); return; }
    navigator.clipboard.writeText(body).then(function() {
        alert('Copied to clipboard!');
    });
}

async function seoSuggest() {
    if (!_draftId) { alert('Generate or save a draft first.'); return; }

    const data = await apiCall('/admin/assistant/seo-suggest/', 'POST', {
        draft_id: _draftId,
    });
    if (!data) return;

    const panel = document.getElementById('seo-panel');
    const results = document.getElementById('seo-results');
    if (!panel || !results) return;

    results.innerHTML =
        '<p><strong>Title:</strong> ' + (data.seo_title || '') + '</p>' +
        '<p><strong>Description:</strong> ' + (data.seo_description || '') + '</p>' +
        '<p><strong>Keywords:</strong> ' + (data.seo_keywords || []).join(', ') + '</p>' +
        '<p><strong>Tips:</strong></p><ul>' +
        (data.suggestions || []).map(function(s) { return '<li>' + s + '</li>'; }).join('') +
        '</ul>';
    panel.style.display = 'block';
}
