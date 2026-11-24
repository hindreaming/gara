// Gara sandbox page: drag-and-drop uploads to the public sandbox API, one file per request.
const API = '/sandbox/api';
const $ = (id) => document.getElementById(id);

let quota = null;

function megabytes(bytes) {
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function isExhausted(q) {
    return q.uploadsUsed >= q.maxUploads || q.bytesUsed >= q.maxBytes;
}

function renderQuota(q) {
    quota = q;
    $('quota-uploads').textContent = `${q.uploadsUsed} of ${q.maxUploads}`;
    $('quota-bytes').textContent = `${megabytes(q.bytesUsed)} of ${megabytes(q.maxBytes)}`;
    $('quota-uploads-bar').style.width = `${Math.min(100, 100 * q.uploadsUsed / q.maxUploads)}%`;
    $('quota-bytes-bar').style.width = `${Math.min(100, 100 * q.bytesUsed / q.maxBytes)}%`;
    $('quota-note').textContent = `Resets ${new Date(q.resetsAt).toLocaleString()} (midnight UTC).`;
    $('drop-zone').classList.toggle('disabled', isExhausted(q));
    if (isExhausted(q)) {
        showMessage("Today's sandbox limit is reached, so uploads are closed until midnight UTC.");
    }
}

function showMessage(text) {
    $('message').textContent = text;
    $('message').hidden = !text;
}

function link(href, text) {
    const a = document.createElement('a');
    a.href = href;
    a.textContent = text;
    a.target = '_blank';
    a.rel = 'noopener';
    return a;
}

function resultItem(file) {
    const item = document.createElement('li');
    const name = document.createElement('div');
    name.className = 'file';
    name.append(file.name, Object.assign(document.createElement('span'), {textContent: megabytes(file.size)}));
    const status = document.createElement('div');
    status.className = 'status';
    status.textContent = 'Uploading…';
    item.append(name, status);
    $('results').prepend(item);
    return {item, status};
}

async function uploadOne(file) {
    const {item, status} = resultItem(file);
    const query = `name=${encodeURIComponent(file.name)}&type=${encodeURIComponent(file.type)}`;
    const response = await fetch(`${API}/files?${query}`, {
        method: 'POST',
        headers: {'Content-Type': 'application/octet-stream'},
        body: file,
    });
    const body = await response.json().catch(() => ({message: `Upload failed (${response.status})`}));
    if (!response.ok) {
        status.className = 'status error';
        status.textContent = body.message;
        return false;
    }
    status.className = 'status ok';
    status.textContent = 'Stored on GitHub';
    const links = document.createElement('div');
    links.className = 'links';
    links.append(link(body.fileUrl, 'Public link to the file'), link(body.repositoryUrl, 'Repository on GitHub'));
    item.appendChild(links);
    renderQuota(body.quota);
    return true;
}

async function uploadAll(files) {
    if (quota && isExhausted(quota)) {
        return;
    }
    showMessage('');
    for (const file of files) {
        if (!await uploadOne(file)) {
            break;
        }
    }
    loadQuota();
}

async function loadQuota() {
    const response = await fetch(`${API}/quota`);
    if (response.ok) {
        renderQuota(await response.json());
    }
}

const dropZone = $('drop-zone');
dropZone.addEventListener('dragover', (event) => {
    event.preventDefault();
    dropZone.classList.add('dragging');
});
dropZone.addEventListener('dragleave', () => dropZone.classList.remove('dragging'));
dropZone.addEventListener('drop', (event) => {
    event.preventDefault();
    dropZone.classList.remove('dragging');
    uploadAll([...event.dataTransfer.files]);
});
dropZone.addEventListener('click', (event) => {
    if (quota && isExhausted(quota)) {
        event.preventDefault();
    }
});
$('file-input').addEventListener('change', (event) => {
    uploadAll([...event.target.files]);
    event.target.value = '';
});

loadQuota();
